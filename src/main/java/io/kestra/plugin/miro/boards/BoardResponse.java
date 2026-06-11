package io.kestra.plugin.miro.boards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a Miro board as returned by the Miro REST API.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardResponse {

    private String id;
    private String name;
    private String description;
    private String type;

    @JsonProperty("viewLink")
    private String viewLink;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("modifiedAt")
    private String modifiedAt;

    @JsonProperty("createdBy")
    private Map<String, Object> createdBy;

    @JsonProperty("modifiedBy")
    private Map<String, Object> modifiedBy;

    @JsonProperty("owner")
    private Map<String, Object> owner;

    @JsonProperty("team")
    private Map<String, Object> team;

    @JsonProperty("project")
    private Map<String, Object> project;

    @JsonProperty("sharingPolicy")
    private Map<String, Object> sharingPolicy;

    @JsonProperty("permissionsPolicy")
    private Map<String, Object> permissionsPolicy;
}
