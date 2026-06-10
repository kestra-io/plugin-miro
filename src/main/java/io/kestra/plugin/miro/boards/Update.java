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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Update a Miro board",
    description = """
        Updates the name, description, sharing policy, or permissions policy of an existing Miro board.
        Only the fields you provide are modified."""
)
@Plugin(
    examples = {
        @Example(
            title = "Rename a Miro board",
            full = true,
            code = """
                id: update_miro_board
                namespace: company.team

                tasks:
                  - id: update
                    type: io.kestra.plugin.miro.boards.Update
                    token: "{{ secret('MIRO_TOKEN') }}"
                    boardId: "uXjVK1234567="
                    name: "Q3 Retrospective"
                    boardDescription: "Updated by Kestra"
                """
        )
    }
)
public class Update extends AbstractMiroConnection implements RunnableTask<BoardOutput> {

    @Schema(
        title = "Board ID",
        description = "Unique identifier of the board to update."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> boardId;

    @Schema(
        title = "Name",
        description = "New name for the board."
    )
    @PluginProperty(group = "main")
    private Property<String> name;

    @Schema(
        title = "Description",
        description = "New description for the board."
    )
    @PluginProperty(group = "main")
    private Property<String> boardDescription;

    @Schema(
        title = "Sharing policy",
        description = """
            Updated sharing policy. Follows the Miro board sharing policy schema.
            See [Miro sharing policy reference](https://developers.miro.com/reference/update-board)."""
    )
    @PluginProperty(group = "advanced")
    private Property<Map<String, Object>> sharingPolicy;

    @Schema(
        title = "Permissions policy",
        description = """
            Updated permissions policy. Follows the Miro board permissions policy schema.
            See [Miro permissions policy reference](https://developers.miro.com/reference/update-board)."""
    )
    @PluginProperty(group = "advanced")
    private Property<Map<String, Object>> permissionsPolicy;

    @Override
    public BoardOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        var rBoardId = runContext.render(boardId).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("boardId is required")
        );

        var body = new HashMap<String, Object>();

        var rName = runContext.render(name).as(String.class).orElse(null);
        if (rName != null) {
            body.put("name", rName);
        }

        var rDescription = runContext.render(boardDescription).as(String.class).orElse(null);
        if (rDescription != null) {
            body.put("description", rDescription);
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

        if (body.isEmpty()) {
            throw new IllegalArgumentException("At least one of name, description, sharingPolicy, or permissionsPolicy must be provided.");
        }

        logger.info("Updating Miro board {}", rBoardId);
        var url = getBaseUrl() + "/boards/" + URLEncoder.encode(rBoardId, StandardCharsets.UTF_8);
        var request = authorizedRequestWithBody(runContext, "PATCH", url, body);
        var response = execute(runContext, request, BoardResponse.class);

        return BoardOutput.from(response);
    }
}
