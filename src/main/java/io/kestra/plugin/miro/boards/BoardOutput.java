package io.kestra.plugin.miro.boards;

import io.kestra.core.models.tasks.Output;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Shared output for tasks that return a single Miro board (Create, Get, Update, Copy).
 * The Miro board endpoints all return the full board object, so a single shape is reused;
 * fields the API omits for a given call stay null.
 */
@Builder
@Getter
public class BoardOutput implements Output {

    @Schema(title = "Board ID", description = "Unique identifier of the board.")
    private String id;

    @Schema(title = "Name", description = "Name of the board.")
    private String name;

    @Schema(title = "Description", description = "Description of the board.")
    private String description;

    @Schema(title = "View link", description = "URL to open the board in the Miro editor.")
    private String viewLink;

    @Schema(title = "Created at", description = "ISO-8601 creation timestamp.")
    private String createdAt;

    @Schema(title = "Modified at", description = "ISO-8601 last-modified timestamp.")
    private String modifiedAt;

    @Schema(title = "Team", description = "Full team object the board belongs to, as returned by Miro (id, name, ...).")
    private Map<String, Object> team;

    @Schema(title = "Project", description = "Full project object the board belongs to, as returned by Miro (id, name, ...). Null if not assigned to a project.")
    private Map<String, Object> project;

    @Schema(title = "Sharing policy", description = "Board sharing policy.")
    private Map<String, Object> sharingPolicy;

    @Schema(title = "Permissions policy", description = "Board permissions policy.")
    private Map<String, Object> permissionsPolicy;

    /** Build the output from a Miro board API response. */
    public static BoardOutput from(BoardResponse board) {
        return BoardOutput.builder()
            .id(board.getId())
            .name(board.getName())
            .description(board.getDescription())
            .viewLink(board.getViewLink())
            .createdAt(board.getCreatedAt())
            .modifiedAt(board.getModifiedAt())
            .team(board.getTeam())
            .project(board.getProject())
            .sharingPolicy(board.getSharingPolicy())
            .permissionsPolicy(board.getPermissionsPolicy())
            .build();
    }
}
