package io.kestra.plugin.miro.boards;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
public abstract class AbstractMiroConnection extends Task {

    protected static final ObjectMapper MAPPER = JacksonMapper.ofJson(false);
    static final String MIRO_API_BASE_URL = "https://api.miro.com/v2";

    @Builder.Default
    @Schema(hidden = true)
    @JsonIgnore
    protected String baseUrl = MIRO_API_BASE_URL;

    @Schema(
        title = "Miro OAuth 2.0 access token",
        description = """
            OAuth 2.0 bearer token used to authenticate against the Miro REST API.
            Use `{{ secret('MIRO_TOKEN') }}` to avoid exposing the value in flow definitions.
            Tokens expire in 1 hour; non-expiring tokens are also supported for service accounts."""
    )
    @NotNull
    @PluginProperty(group = "connection", secret = true)
    protected Property<String> token;

    @Schema(title = "HTTP client configuration")
    @PluginProperty(group = "connection")
    protected HttpConfiguration options;

    protected String renderToken(RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(token).as(String.class).orElseThrow(
            () -> new IllegalArgumentException("token is required")
        );
    }

    protected HttpRequest.HttpRequestBuilder authorizedRequest(RunContext runContext, String method, String url)
        throws IllegalVariableEvaluationException {
        var rToken = renderToken(runContext);
        return HttpRequest.builder()
            .method(method)
            .uri(URI.create(url))
            .addHeader("Authorization", "Bearer " + rToken)
            .addHeader("Accept", "application/json");
    }

    protected HttpRequest.HttpRequestBuilder authorizedRequestWithBody(RunContext runContext, String method, String url, Object body)
        throws Exception {
        var json = MAPPER.writeValueAsString(body);
        return authorizedRequest(runContext, method, url)
            .addHeader("Content-Type", "application/json")
            .body(HttpRequest.StringRequestBody.builder().content(json).build());
    }

    protected <T> T execute(RunContext runContext, HttpRequest.HttpRequestBuilder builder, Class<T> responseType)
        throws Exception {
        try (var client = new HttpClient(runContext, options)) {
            HttpResponse<T> response = client.request(builder.build(), responseType);
            return response.getBody();
        } catch (HttpClientResponseException e) {
            var status = e.getResponse() != null && e.getResponse().getStatus() != null
                ? e.getResponse().getStatus().getCode()
                : -1;
            var hint = switch (status) {
                case 401 -> " Check the token (it may be expired or invalid).";
                case 403 -> " The token may lack permission for this board/operation.";
                default -> "";
            };
            throw new RuntimeException("Miro API error (HTTP " + status + "): " + e.getMessage() + hint, e);
        }
    }
}
