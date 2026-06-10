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

import java.util.Map;

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
public class Get extends AbstractMiroConnection implements RunnableTask<Get.Output> {

    @Schema(
        title = "Board ID",
        description = "The unique identifier of the Miro board to retrieve."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> boardId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        var rBoardId = runContext.render(boardId).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("boardId is required")
        );

        logger.info("Getting Miro board {}", rBoardId);
        var url = getBaseUrl() + "/boards/" + rBoardId;
        var request = authorizedRequest(runContext, "GET", url);
        var response = execute(runContext, request, BoardResponse.class);

        return Output.builder()
            .id(response.getId())
            .name(response.getName())
            .description(response.getDescription())
            .viewLink(response.getViewLink())
            .createdAt(response.getCreatedAt())
            .modifiedAt(response.getModifiedAt())
            .team(response.getTeam())
            .project(response.getProject())
            .sharingPolicy(response.getSharingPolicy())
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "Board ID", description = "Unique identifier of the board.")
        private String id;

        @Schema(title = "Name", description = "Name of the board.")
        private String name;

        @Schema(title = "Description", description = "Description of the board.")
        private String description;

        @Schema(title = "View link", description = "URL to open the board in the Miro editor.")
        private String viewLink;

        @Schema(title = "Created at", description = "ISO-8601 timestamp when the board was created.")
        private String createdAt;

        @Schema(title = "Modified at", description = "ISO-8601 timestamp when the board was last modified.")
        private String modifiedAt;

        @Schema(title = "Team", description = "Team the board belongs to.")
        private Map<String, Object> team;

        @Schema(title = "Project", description = "Project the board belongs to, if any.")
        private Map<String, Object> project;

        @Schema(title = "Sharing policy", description = "Sharing policy of the board.")
        private Map<String, Object> sharingPolicy;
    }
}
