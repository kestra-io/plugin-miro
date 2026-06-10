package io.kestra.plugin.miro.boards;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.JacksonMapper;
import jakarta.inject.Inject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class BoardsTest {

    @Inject
    private RunContextFactory runContextFactory;

    private static final ObjectMapper MAPPER = JacksonMapper.ofJson(false);

    private WireMockServer wireMock;
    private String previousBaseUrl;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        previousBaseUrl = System.getProperty("miro.api.base.url");
        System.setProperty("miro.api.base.url", wireMock.baseUrl());
        configureFor("localhost", wireMock.port());
    }

    @AfterEach
    void stopWireMock() {
        wireMock.stop();
        if (previousBaseUrl == null) {
            System.clearProperty("miro.api.base.url");
        } else {
            System.setProperty("miro.api.base.url", previousBaseUrl);
        }
    }

    // --- List ---

    @Test
    void list_happyPath_returnsBoards() throws Exception {
        var responseBody = Map.of(
            "data", java.util.List.of(
                boardJson("board-1", "Sprint Retro"),
                boardJson("board-2", "Design Review")
            ),
            "total", 2,
            "size", 2
        );

        wireMock.stubFor(get(urlPathEqualTo("/boards"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(MAPPER.writeValueAsString(responseBody))
                .withStatus(200)));

        var task = io.kestra.plugin.miro.boards.List.builder()
            .token(Property.ofValue("test-token"))
            .limit(Property.ofValue(20))
            .build();

        var output = task.run(runContextFactory.of(Map.of()));

        assertThat(output.getBoards(), hasSize(2));
        assertThat(output.getBoards().getFirst().get("id"), Matchers.equalTo("board-1"));
        assertThat(output.getSize(), Matchers.equalTo(2));
    }

    @Test
    void list_withFilters_passesQueryParams() throws Exception {
        var responseBody = Map.of(
            "data", java.util.List.of(boardJson("board-3", "Team Board")),
            "total", 1,
            "size", 1
        );

        wireMock.stubFor(get(urlPathEqualTo("/boards"))
            .withQueryParam("team_id", com.github.tomakehurst.wiremock.client.WireMock.equalTo("team-abc"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(MAPPER.writeValueAsString(responseBody))
                .withStatus(200)));

        var task = io.kestra.plugin.miro.boards.List.builder()
            .token(Property.ofValue("test-token"))
            .teamId(Property.ofValue("team-abc"))
            .build();

        var output = task.run(runContextFactory.of(Map.of()));

        assertThat(output.getBoards(), hasSize(1));
        assertThat(output.getBoards().getFirst().get("name"), Matchers.equalTo("Team Board"));
    }

    @Test
    void list_withCursor_passesParam() throws Exception {
        var responseBody = Map.of(
            "data", java.util.List.of(),
            "total", 0,
            "size", 0
        );

        wireMock.stubFor(get(urlPathEqualTo("/boards"))
            .withQueryParam("cursor", com.github.tomakehurst.wiremock.client.WireMock.equalTo("next-page-cursor"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(MAPPER.writeValueAsString(responseBody))
                .withStatus(200)));

        var task = io.kestra.plugin.miro.boards.List.builder()
            .token(Property.ofValue("test-token"))
            .cursor(Property.ofValue("next-page-cursor"))
            .build();

        var output = task.run(runContextFactory.of(Map.of()));

        assertThat(output.getBoards(), empty());
    }

    // --- Get ---

    @Test
    void get_happyPath_returnsBoard() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/boards/board-xyz"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(MAPPER.writeValueAsString(boardJson("board-xyz", "My Board")))
                .withStatus(200)));

        var task = Get.builder()
            .token(Property.ofValue("test-token"))
            .boardId(Property.ofValue("board-xyz"))
            .build();

        var output = task.run(runContextFactory.of(Map.of()));

        assertThat(output.getId(), Matchers.equalTo("board-xyz"));
        assertThat(output.getName(), Matchers.equalTo("My Board"));
        assertThat(output.getViewLink(), notNullValue());
    }

    @Test
    void get_missingBoardId_throws() {
        var task = Get.builder()
            .token(Property.ofValue("test-token"))
            .build();

        assertThrows(Exception.class, () -> task.run(runContextFactory.of(Map.of())));
    }

    // --- Create ---

    @Test
    void create_happyPath_returnsNewBoard() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/boards"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(MAPPER.writeValueAsString(boardJson("new-board", "Sprint 42")))
                .withStatus(201)));

        var task = Create.builder()
            .token(Property.ofValue("test-token"))
            .name(Property.ofValue("Sprint 42"))
            .boardDescription(Property.ofValue("Provisioned by Kestra"))
            .teamId(Property.ofValue("team-abc"))
            .build();

        var output = task.run(runContextFactory.of(Map.of()));

        assertThat(output.getId(), Matchers.equalTo("new-board"));
        assertThat(output.getName(), Matchers.equalTo("Sprint 42"));
        assertThat(output.getViewLink(), notNullValue());
    }

    @Test
    void create_missingName_throws() {
        var task = Create.builder()
            .token(Property.ofValue("test-token"))
            .build();

        assertThrows(Exception.class, () -> task.run(runContextFactory.of(Map.of())));
    }

    // --- Update ---

    @Test
    void update_happyPath_returnsUpdatedBoard() throws Exception {
        wireMock.stubFor(patch(urlEqualTo("/boards/board-xyz"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(MAPPER.writeValueAsString(boardJson("board-xyz", "Renamed Board")))
                .withStatus(200)));

        var task = Update.builder()
            .token(Property.ofValue("test-token"))
            .boardId(Property.ofValue("board-xyz"))
            .name(Property.ofValue("Renamed Board"))
            .build();

        var output = task.run(runContextFactory.of(Map.of()));

        assertThat(output.getId(), Matchers.equalTo("board-xyz"));
        assertThat(output.getName(), Matchers.equalTo("Renamed Board"));
    }

    @Test
    void update_noFields_throws() {
        var task = Update.builder()
            .token(Property.ofValue("test-token"))
            .boardId(Property.ofValue("board-xyz"))
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContextFactory.of(Map.of())));
    }

    // --- Delete ---

    @Test
    void delete_happyPath_succeeds() throws Exception {
        wireMock.stubFor(delete(urlEqualTo("/boards/board-to-delete"))
            .willReturn(aResponse()
                .withStatus(204)));

        var task = Delete.builder()
            .token(Property.ofValue("test-token"))
            .boardId(Property.ofValue("board-to-delete"))
            .build();

        var output = task.run(runContextFactory.of(Map.of()));

        assertThat(output, nullValue());
        wireMock.verify(1, deleteRequestedFor(urlEqualTo("/boards/board-to-delete")));
    }

    @Test
    void delete_missingBoardId_throws() {
        var task = Delete.builder()
            .token(Property.ofValue("test-token"))
            .build();

        assertThrows(Exception.class, () -> task.run(runContextFactory.of(Map.of())));
    }

    // --- Copy ---

    @Test
    void copy_happyPath_returnsCopiedBoard() throws Exception {
        wireMock.stubFor(put(urlPathEqualTo("/boards"))
            .withQueryParam("copy_from", com.github.tomakehurst.wiremock.client.WireMock.equalTo("source-board"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(MAPPER.writeValueAsString(boardJson("copied-board", "Sprint 42 (copy)")))
                .withStatus(201)));

        var task = Copy.builder()
            .token(Property.ofValue("test-token"))
            .sourceBoardId(Property.ofValue("source-board"))
            .name(Property.ofValue("Sprint 42 (copy)"))
            .build();

        var output = task.run(runContextFactory.of(Map.of()));

        assertThat(output.getId(), Matchers.equalTo("copied-board"));
        assertThat(output.getName(), Matchers.equalTo("Sprint 42 (copy)"));
    }

    @Test
    void copy_missingSourceBoardId_throws() {
        var task = Copy.builder()
            .token(Property.ofValue("test-token"))
            .build();

        assertThrows(Exception.class, () -> task.run(runContextFactory.of(Map.of())));
    }

    // --- Integration tests (require a real Miro token) ---

    @Test
    @Disabled("Requires MIRO_TOKEN and MIRO_TEAM_ID env vars — run manually")
    void integration_createGetDeleteBoard_roundtrip() throws Exception {
        var token = System.getenv("MIRO_TOKEN");
        var teamId = System.getenv("MIRO_TEAM_ID");
        var runContext = runContextFactory.of(Map.of());

        // Create
        var create = Create.builder()
            .token(Property.ofValue(token))
            .name(Property.ofValue("Kestra IT " + System.currentTimeMillis()))
            .teamId(Property.ofValue(teamId))
            .build();
        var created = create.run(runContext);
        assertThat(created.getId(), notNullValue());

        // Get
        var getTask = Get.builder()
            .token(Property.ofValue(token))
            .boardId(Property.ofValue(created.getId()))
            .build();
        var fetched = getTask.run(runContext);
        assertThat(fetched.getId(), Matchers.equalTo(created.getId()));

        // Delete (cleanup)
        var delete = Delete.builder()
            .token(Property.ofValue(token))
            .boardId(Property.ofValue(created.getId()))
            .build();
        delete.run(runContext);
    }

    // --- Helpers ---

    private static Map<String, Object> boardJson(String id, String name) {
        return Map.of(
            "id", id,
            "name", name,
            "description", "Test board",
            "type", "board",
            "viewLink", "https://miro.com/app/board/" + id + "/",
            "createdAt", "2024-01-01T10:00:00Z",
            "modifiedAt", "2024-01-02T10:00:00Z"
        );
    }
}
