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


@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get a Miro board",
    description = "Retrieves the details of a single Miro board by its ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Get a board by ID",
            full = true,
            code = """
                id: get_miro_board
                namespace: company.team

                tasks:
                  - id: get
                    type: io.kestra.plugin.miro.boards.Get
                    token: "{{ secret('MIRO_TOKEN') }}"
                    boardId: "uXjVK1234567="
                """
        )
    }
)
public class Get extends AbstractMiroConnection implements RunnableTask<BoardOutput> {

    @Schema(
        title = "Board ID",
        description = "The unique identifier of the Miro board to retrieve."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> boardId;

    @Override
    public BoardOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        var rBoardId = runContext.render(boardId).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("boardId is required")
        );

        logger.info("Getting Miro board {}", rBoardId);
        var url = getBaseUrl() + "/boards/" + rBoardId;
        var request = authorizedRequest(runContext, "GET", url);
        var response = execute(runContext, request, BoardResponse.class);

        return BoardOutput.from(response);
    }
}
