package io.kestra.plugin.miro.boards;

/**
 * Sort orders accepted by the Miro "list boards" endpoint.
 * Each constant maps to the exact query-parameter value the API expects.
 * The Miro API does not support a descending `-` prefix, so the set is closed.
 */
public enum SortOrder {
    DEFAULT("default"),
    LAST_MODIFIED("last_modified"),
    LAST_OPENED("last_opened"),
    LAST_OPENED_BY_ANYONE("last_opened_by_anyone"),
    LAST_CREATED("last_created"),
    ALPHABETICALLY("alphabetically");

    private final String value;

    SortOrder(String value) {
        this.value = value;
    }

    /** The query-parameter value sent to the Miro API. */
    public String value() {
        return value;
    }
}
