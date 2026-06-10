package io.kestra.plugin.miro.boards;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "List Miro boards",
    description = """
        Returns a paginated list of boards the authenticated user can access.
        Optionally filter by team ID, project ID, sort order, or a search query.
        Returns the first page by default; use `cursor` from the output to fetch subsequent pages."""
)
@Plugin(
    examples = {
        @Example(
            title = "List all boards in a team",
            full = true,
            code = """
                id: list_miro_boards
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.miro.boards.List
                    token: "{{ secret('MIRO_TOKEN') }}"
                    teamId: "{{ secret('MIRO_TEAM_ID') }}"
                """
        ),
        @Example(
            title = "List boards with cursor-based pagination",
            full = true,
            code = """
                id: list_miro_boards_paged
                namespace: company.team

                tasks:
                  - id: page1
                    type: io.kestra.plugin.miro.boards.List
                    token: "{{ secret('MIRO_TOKEN') }}"
                    teamId: "{{ secret('MIRO_TEAM_ID') }}"
                    limit: 10

                  - id: page2
                    type: io.kestra.plugin.miro.boards.List
                    token: "{{ secret('MIRO_TOKEN') }}"
                    teamId: "{{ secret('MIRO_TEAM_ID') }}"
                    limit: 10
                    cursor: "{{ outputs.page1.cursor }}"
                """
        )
    }
)
public class List extends AbstractMiroConnection implements RunnableTask<List.Output> {

    @Schema(
        title = "Team ID",
        description = "Filter boards belonging to the given team."
    )
    @PluginProperty(group = "processing")
    private Property<String> teamId;

    @Schema(
        title = "Project ID",
        description = "Filter boards belonging to the given project."
    )
    @PluginProperty(group = "processing")
    private Property<String> projectId;

    @Schema(
        title = "Search query",
        description = "Filter boards whose name matches this query string."
    )
    @PluginProperty(group = "processing")
    private Property<String> query;

    @Schema(
        title = "Owner",
        description = "Filter boards by owner (user ID or email)."
    )
    @PluginProperty(group = "processing")
    private Property<String> owner;

    @Schema(
        title = "Sort order",
        description = "Sort order for the board list. The Miro API does not support a descending `-` prefix."
    )
    @PluginProperty(group = "processing")
    private Property<SortOrder> sort;

    @Schema(
        title = "Cursor",
        description = "Cursor returned by a previous List call, used to fetch the next page of results."
    )
    @PluginProperty(group = "advanced")
    private Property<String> cursor;

    @Builder.Default
    @Schema(
        title = "Limit",
        description = "Maximum number of boards to return per page (1-50). Defaults to 20."
    )
    @PluginProperty(group = "advanced")
    private Property<Integer> limit = Property.ofValue(20);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        var urlBuilder = new StringBuilder(getBaseUrl() + "/boards?");

        var rTeamId = runContext.render(teamId).as(String.class).orElse(null);
        if (rTeamId != null) urlBuilder.append("team_id=").append(URLEncoder.encode(rTeamId, StandardCharsets.UTF_8)).append("&");

        var rProjectId = runContext.render(projectId).as(String.class).orElse(null);
        if (rProjectId != null) urlBuilder.append("project_id=").append(URLEncoder.encode(rProjectId, StandardCharsets.UTF_8)).append("&");

        var rQuery = runContext.render(query).as(String.class).orElse(null);
        if (rQuery != null) urlBuilder.append("query=").append(URLEncoder.encode(rQuery, StandardCharsets.UTF_8)).append("&");

        var rOwner = runContext.render(owner).as(String.class).orElse(null);
        if (rOwner != null) urlBuilder.append("owner=").append(URLEncoder.encode(rOwner, StandardCharsets.UTF_8)).append("&");

        var rSort = runContext.render(sort).as(SortOrder.class).orElse(null);
        if (rSort != null) urlBuilder.append("sort=").append(rSort.value()).append("&");

        var rCursor = runContext.render(cursor).as(String.class).orElse(null);
        if (rCursor != null) urlBuilder.append("cursor=").append(URLEncoder.encode(rCursor, StandardCharsets.UTF_8)).append("&");

        var rLimit = runContext.render(limit).as(Integer.class).orElse(20);
        urlBuilder.append("limit=").append(rLimit);

        var url = urlBuilder.toString();
        logger.info("Listing Miro boards (limit={})", rLimit);

        var request = authorizedRequest(runContext, "GET", url);
        var response = execute(runContext, request, BoardListResponse.class);

        var boards = response.getData() != null ? response.getData() : new ArrayList<BoardResponse>();
        logger.info("Retrieved {} boards", boards.size());

        return Output.builder()
            .boards(boards.stream().map(List::toMap).toList())
            .total(response.getTotal())
            .size(boards.size())
            .cursor(response.getCursor())
            .build();
    }

    private static java.util.Map<String, Object> toMap(BoardResponse b) {
        return MAPPER.convertValue(b, new TypeReference<java.util.Map<String, Object>>() {});
    }

    @lombok.Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(
            title = "Boards",
            description = "List of boards returned by this page."
        )
        private java.util.List<java.util.Map<String, Object>> boards;

        @Schema(
            title = "Total",
            description = "Total number of boards matching the filter (may exceed this page)."
        )
        private Integer total;

        @Schema(
            title = "Size",
            description = "Number of boards returned in this page."
        )
        private Integer size;

        @Schema(
            title = "Cursor",
            description = "Cursor to pass to the next List call to retrieve the following page. Null when there are no more results."
        )
        private String cursor;
    }
}
