package io.kestra.plugin.miro.boards;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Copy a Miro board",
    description = """
        Creates a copy of an existing Miro board.
        Optionally override the name, description, or target team."""
)
@Plugin(
    examples = {
        @Example(
            title = "Copy a board to a different team",
            full = true,
            code = """
                id: copy_miro_board
                namespace: company.team

                tasks:
                  - id: copy
                    type: io.kestra.plugin.miro.boards.Copy
                    token: "{{ secret('MIRO_TOKEN') }}"
                    sourceBoardId: "uXjVK1234567="
                    name: "Sprint 42 Retro (copy)"
                    teamId: "{{ secret('MIRO_TEAM_ID') }}"
                """
        )
    }
)
public class Copy extends AbstractMiroConnection implements RunnableTask<Copy.Output> {

    @Schema(
        title = "Source board ID",
        description = "ID of the board to copy."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> sourceBoardId;

    @Schema(
        title = "Name",
        description = "Name for the new board. Defaults to the source board name if not set."
    )
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(
        title = "Description",
        description = "Description for the new board."
    )
    @PluginProperty(group = "main")
    private Property<String> boardDescription;

    @Schema(
        title = "Team ID",
        description = "ID of the team the copied board will be created in."
    )
    @PluginProperty(group = "destination")
    private Property<String> teamId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        var rSourceBoardId = runContext.render(sourceBoardId).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("sourceBoardId is required")
        );

        var body = new HashMap<String, Object>();

        var rName = runContext.render(name).as(String.class).orElse(null);
        if (rName != null) body.put("name", rName);

        var rDescription = runContext.render(boardDescription).as(String.class).orElse(null);
        if (rDescription != null) body.put("description", rDescription);

        var rTeamId = runContext.render(teamId).as(String.class).orElse(null);
        if (rTeamId != null) body.put("teamId", rTeamId);

        logger.info("Copying Miro board {}", rSourceBoardId);
        // The Miro API copies a board via PUT /v2/boards/{copy_from} with an empty or partial body
        var url = getBaseUrl() + "/boards?copy_from=" + rSourceBoardId;
        var request = authorizedRequestWithBody(runContext, "PUT", url, body);
        var response = execute(runContext, request, BoardResponse.class);

        logger.info("Copied board, new ID: {}", response.getId());

        return Output.builder()
            .id(response.getId())
            .name(response.getName())
            .description(response.getDescription())
            .viewLink(response.getViewLink())
            .createdAt(response.getCreatedAt())
            .modifiedAt(response.getModifiedAt())
            .team(response.getTeam())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "Board ID", description = "Unique identifier of the newly created copy.")
        private String id;

        @Schema(title = "Name", description = "Name of the copied board.")
        private String name;

        @Schema(title = "Description", description = "Description of the copied board.")
        private String description;

        @Schema(title = "View link", description = "URL to open the copied board in the Miro editor.")
        private String viewLink;

        @Schema(title = "Created at", description = "ISO-8601 timestamp when the copy was created.")
        private String createdAt;

        @Schema(title = "Modified at", description = "ISO-8601 timestamp when the copy was last modified.")
        private String modifiedAt;

        @Schema(title = "Team", description = "Team the copied board belongs to.")
        private Map<String, Object> team;
    }
}
