package io.kestra.plugin.miro.boards;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a Miro board",
    description = """
        Creates a new Miro board. The board name is required.
        Optionally assign it to a team, project, or configure sharing and permissions policies."""
)
@Plugin(
    examples = {
        @Example(
            title = "Create a new Miro board",
            full = true,
            code = """
                id: create_miro_board
                namespace: company.team

                inputs:
                  - id: board_name
                    type: STRING
                  - id: team_id
                    type: STRING

                tasks:
                  - id: create
                    type: io.kestra.plugin.miro.boards.Create
                    token: "{{ secret('MIRO_TOKEN') }}"
                    name: "{{ inputs.board_name }}"
                    teamId: "{{ inputs.team_id }}"
                    boardDescription: "Provisioned automatically by Kestra"
                """
        )
    }
)
public class Create extends AbstractMiroConnection implements RunnableTask<BoardOutput> {

    @Schema(
        title = "Board name",
        description = "The name for the new board."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(
        title = "Description",
        description = "Optional description for the new board."
    )
    @PluginProperty(group = "main")
    private Property<String> boardDescription;

    @Schema(
        title = "Team ID",
        description = "ID of the team the board will be created in."
    )
    @PluginProperty(group = "destination")
    private Property<String> teamId;

    @Schema(
        title = "Project ID",
        description = "ID of the project to associate the board with."
    )
    @PluginProperty(group = "destination")
    private Property<String> projectId;

    @Schema(
        title = "Sharing policy",
        description = """
            Sharing policy for the new board. Follows the Miro board sharing policy schema.
            Example: `{access: "private", inviteToBoard: "viewer"}`.
            See [Miro sharing policy reference](https://developers.miro.com/reference/create-board)."""
    )
    @PluginProperty(group = "advanced")
    private Property<Map<String, Object>> sharingPolicy;

    @Schema(
        title = "Permissions policy",
        description = """
            Permissions policy for the new board. Follows the Miro board permissions policy schema.
            See [Miro permissions policy reference](https://developers.miro.com/reference/create-board)."""
    )
    @PluginProperty(group = "advanced")
    private Property<Map<String, Object>> permissionsPolicy;

    @Override
    public BoardOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        var rName = runContext.render(name).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("name is required")
        );

        var body = new HashMap<String, Object>();
        body.put("name", rName);

        var rDescription = runContext.render(boardDescription).as(String.class).orElse(null);
        if (rDescription != null) {
            body.put("description", rDescription);
        }

        var rTeamId = runContext.render(teamId).as(String.class).orElse(null);
        if (rTeamId != null) {
            body.put("teamId", rTeamId);
        }

        var rProjectId = runContext.render(projectId).as(String.class).orElse(null);
        if (rProjectId != null) {
            body.put("projectId", Map.of("id", rProjectId));
        }

        var policy = new HashMap<String, Object>();

        var rSharingPolicy = runContext.render(sharingPolicy).asMap(String.class, Object.class);
        if (!rSharingPolicy.isEmpty()) {
            policy.put("sharingPolicy", rSharingPolicy);
        }

        var rPermissionsPolicy = runContext.render(permissionsPolicy).asMap(String.class, Object.class);
        if (!rPermissionsPolicy.isEmpty()) {
            policy.put("permissionsPolicy", rPermissionsPolicy);
        }

        if (!policy.isEmpty()) {
            body.put("policy", policy);
        }

        logger.info("Creating Miro board: {}", rName);
        var url = getBaseUrl() + "/boards";
        var request = authorizedRequestWithBody(runContext, "POST", url, body);
        var response = execute(runContext, request, BoardResponse.class);

        logger.info("Created board with ID: {}", response.getId());

        return BoardOutput.from(response);
    }
}
