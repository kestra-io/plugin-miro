package io.kestra.plugin.miro.boards;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a Miro board",
    description = """
        Permanently deletes a Miro board by ID. This action cannot be undone.
        The authenticated user must have owner or administrator rights on the board."""
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a Miro board",
            full = true,
            code = """
                id: delete_miro_board
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.miro.boards.Delete
                    token: "{{ secret('MIRO_TOKEN') }}"
                    boardId: "uXjVK1234567="
                """
        )
    }
)
public class Delete extends AbstractMiroConnection implements RunnableTask<VoidOutput> {

    @Schema(
        title = "Board ID",
        description = "Unique identifier of the board to delete."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> boardId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        var rBoardId = runContext.render(boardId).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("boardId is required")
        );

        logger.info("Deleting Miro board {}", rBoardId);
        var url = getBaseUrl() + "/boards/" + URLEncoder.encode(rBoardId, StandardCharsets.UTF_8);
        var request = authorizedRequest(runContext, "DELETE", url);
        execute(runContext, request, String.class);

        logger.info("Board {} deleted", rBoardId);
        return new VoidOutput();
    }
}
