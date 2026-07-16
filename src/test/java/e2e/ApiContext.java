package e2e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds session state (headers, pending body, last response) for API/REST steps, and resolves
 * {{variable}} placeholders the same way TestContext does for UI steps.
 */
public class ApiContext {

    private final TestContext context;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final String baseUrl;
    private String body;
    private HttpResponse<String> response;

    public ApiContext(TestContext context) {
        this.context = context;
        String apiBaseUrl = EnvConfig.get("API_BASE_URL");
        String fallback = EnvConfig.get("BASE_URL");
        this.baseUrl = (apiBaseUrl != null && !apiBaseUrl.isEmpty())
                ? apiBaseUrl
                : (fallback != null ? fallback : "");
    }

    public String resolve(String value) {
        return context.resolve(value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setBody(String jsonBody) {
        this.body = jsonBody;
    }

    public HttpResponse<String> getResponse() {
        return response;
    }

    public String buildUrl(String path) {
        String resolved = resolve(path);
        if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
            return resolved;
        }
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String trimmedPath = resolved.startsWith("/") ? resolved.substring(1) : resolved;
        return trimmedBase + "/" + trimmedPath;
    }

    public void send(String method, String path) {
        String url = buildUrl(path);
        HttpRequest.BodyPublisher publisher = body != null
                ? HttpRequest.BodyPublishers.ofString(body)
                : HttpRequest.BodyPublishers.noBody();

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .method(method.toUpperCase(), publisher);

        boolean hasContentType = headers.keySet().stream().anyMatch(h -> h.equalsIgnoreCase("Content-Type"));
        if (body != null && !hasContentType) {
            builder.header("Content-Type", "application/json");
        }
        headers.forEach(builder::header);

        try {
            response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Request failed: " + e.getMessage(), e);
        }
        body = null;
    }

    /**
     * Traverses the JSON response body using a dot-separated path, e.g. "data.0.id" or "title".
     * Supports both object keys and array indices.
     */
    public Object getField(String fieldPath) {
        if (response == null) {
            throw new IllegalStateException("No response received yet. Send a request first.");
        }
        Object current = parseJson(response.body());
        for (String part : fieldPath.split("\\.")) {
            if (current instanceof JSONArray array) {
                current = array.get(Integer.parseInt(part));
            } else if (current instanceof JSONObject obj) {
                current = obj.get(part);
            } else {
                throw new IllegalArgumentException(
                        "Cannot resolve '" + part + "' in path '" + fieldPath
                                + "': value at that point is not an object or array."
                );
            }
        }
        return current;
    }

    private Object parseJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        return new JSONObject(trimmed);
    }
}
