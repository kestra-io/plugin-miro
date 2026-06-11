package io.kestra.plugin.miro.boards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated list of Miro boards as returned by the GET /v2/boards endpoint.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardListResponse {

    private List<BoardResponse> data;
    private Integer total;
    private Integer size;
    private Integer offset;
    private Integer limit;
}
