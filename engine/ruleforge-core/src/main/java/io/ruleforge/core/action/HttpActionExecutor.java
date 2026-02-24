package io.ruleforge.core.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.spi.ActionExecutor;
import io.ruleforge.core.spi.EvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action executor that makes HTTP calls.
 * Uses {@code java.net.http.HttpClient} to keep ruleforge-core dependency-free.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code method} - HTTP method (GET, POST, PUT, DELETE). Default: POST</li>
 *   <li>{@code headers} - Map of request headers</li>
 *   <li>{@code body} - Request body (serialized as JSON)</li>
 *   <li>{@code timeout} - Timeout in seconds. Default: 10</li>
 *   <li>{@code queryParams} - Map of query parameters to append to the target URL.
 *       Values support {@code ${factPath}} expression resolution. Keys and values are URL-encoded.</li>
 *   <li>{@code authorization} - Authorization convenience object:
 *       {@code {type:"bearer", token:"..."}} or {@code {type:"api_key", header:"X-API-Key", value:"..."}}</li>
 * </ul>
 *
 * <p>Expression resolution: {@code ${factPath}} expressions in the target URL and body string
 * values are resolved from the evaluation context's working memory before making the HTTP call.
 */
public final class HttpActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpActionExecutor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final HttpClient httpClient;

    public HttpActionExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String protocol() {
        return "http";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String target, Map<String, Object> params, EvaluationContext ctx) {
        try {
            WorkingMemory wm = ctx.getWorkingMemory();

            // Resolve ${factPath} expressions in the target URL (URL-encode substituted values)
            String resolvedTarget = resolveExpressions(target, wm, true);

            // Append query parameters (URL-encoded, with expression resolution)
            Object queryParamsObj = params.get("queryParams");
            if (queryParamsObj instanceof Map<?, ?> queryParams && !queryParams.isEmpty()) {
                StringJoiner qsJoiner = new StringJoiner("&");
                for (Map.Entry<?, ?> entry : queryParams.entrySet()) {
                    String key = URLEncoder.encode(String.valueOf(entry.getKey()), StandardCharsets.UTF_8);
                    String rawValue = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                    String resolvedValue = resolveExpressions(rawValue, wm);
                    String encodedValue = URLEncoder.encode(resolvedValue, StandardCharsets.UTF_8);
                    qsJoiner.add(key + "=" + encodedValue);
                }
                resolvedTarget += (resolvedTarget.contains("?") ? "&" : "?") + qsJoiner;
            }

            // SSRF protection: validate URL scheme and block private/internal network addresses
            URI targetUri;
            try {
                targetUri = new URI(resolvedTarget);
            } catch (Exception e) {
                return Map.of("_error", "Invalid URL after expression resolution: " + resolvedTarget +
                        ". Ensure path parameter values do not contain special characters.");
            }
            String scheme = targetUri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                return Map.of("_error", "URL scheme not allowed: " + scheme +
                        ". Only http:// and https:// are permitted.");
            }
            String host = targetUri.getHost();
            if (host != null) {
                try {
                    InetAddress addr = InetAddress.getByName(host);
                    if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                            || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                        return Map.of("_error", "HTTP actions cannot target private/internal network addresses: " + host);
                    }
                } catch (Exception e) {
                    // DNS resolution failure — let the HTTP client handle it
                    log.warn("Could not resolve host '{}' for SSRF check, proceeding", host);
                }
            }

            String method = params.getOrDefault("method", "POST").toString().toUpperCase();
            int timeout = params.containsKey("timeout")
                    ? ((Number) params.get("timeout")).intValue()
                    : 10;

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(Duration.ofSeconds(timeout));

            // Apply authorization convenience
            Object authObj = params.get("authorization");
            if (authObj instanceof Map<?, ?> auth) {
                String type = String.valueOf(auth.get("type")).toLowerCase();
                switch (type) {
                    case "bearer" -> {
                        String token = resolveExpressions(String.valueOf(auth.get("token")), wm);
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }
                    case "api_key" -> {
                        Object headerObj = auth.get("header");
                        String header = headerObj != null ? String.valueOf(headerObj) : "X-API-Key";
                        String value = resolveExpressions(String.valueOf(auth.get("value")), wm);
                        requestBuilder.header(header, value);
                    }
                    default -> log.warn("Unknown authorization type: {}", type);
                }
            }

            // Add headers — supports both Map<String,String> and List<{key,value,enabled}> formats
            Object headersObj = params.get("headers");
            if (headersObj instanceof Map<?, ?> headers) {
                headers.forEach((k, v) -> requestBuilder.header(k.toString(), v != null ? v.toString() : ""));
            } else if (headersObj instanceof List<?> headersList) {
                for (Object item : headersList) {
                    if (item instanceof Map<?, ?> headerEntry) {
                        Object enabled = headerEntry.get("enabled");
                        if (enabled != null && !Boolean.TRUE.equals(enabled) && !"true".equals(enabled.toString())) {
                            continue;
                        }
                        Object key = headerEntry.get("key");
                        Object value = headerEntry.get("value");
                        if (key != null && !key.toString().isBlank()) {
                            String resolvedValue = value != null ? resolveExpressions(value.toString(), wm) : "";
                            requestBuilder.header(key.toString(), resolvedValue);
                        }
                    }
                }
            }

            // Set method and body, resolving expressions in body values
            Object body = params.get("body");
            Object resolvedBody = resolveBodyExpressions(body, wm);
            String bodyStr = resolvedBody != null ? MAPPER.writeValueAsString(resolvedBody) : "";

            switch (method) {
                case "GET" -> requestBuilder.GET();
                case "DELETE" -> requestBuilder.DELETE();
                case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(bodyStr))
                        .header("Content-Type", "application/json");
                default -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyStr))
                        .header("Content-Type", "application/json");
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();
            Map<String, Object> result;
            try {
                result = MAPPER.readValue(responseBody, Map.class);
            } catch (Exception e) {
                // Non-JSON response — wrap gracefully
                result = Map.of("_response", responseBody != null ? responseBody : "",
                                "_statusCode", response.statusCode());
            }

            return result;
        } catch (Exception e) {
            log.error("HTTP action failed for target '{}': {}", target, e.getMessage(), e);
            return Map.of("_error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    /**
     * Resolve {@code ${factPath}} expressions in a string using working memory values.
     * Resolved values are URL-encoded when the input appears to be a URL (starts with http).
     */
    private String resolveExpressions(String input, WorkingMemory wm) {
        return resolveExpressions(input, wm, false);
    }

    private String resolveExpressions(String input, WorkingMemory wm, boolean urlEncode) {
        if (input == null || !input.contains("${")) {
            return input;
        }
        Matcher matcher = EXPRESSION_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String factPath = matcher.group(1).trim();
            Object value = wm.get(factPath);
            if (value == null) {
                log.warn("Expression ${{{}}} resolved to null — fact path '{}' not found in working memory", factPath, factPath);
            }
            String strValue = value != null ? String.valueOf(value) : "";
            if (urlEncode) {
                strValue = URLEncoder.encode(strValue, StandardCharsets.UTF_8);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(strValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Recursively resolve {@code ${factPath}} expressions in body values.
     * Handles Map, String, and passes other types through unchanged.
     */
    @SuppressWarnings("unchecked")
    private Object resolveBodyExpressions(Object body, WorkingMemory wm) {
        if (body == null) {
            return null;
        }
        if (body instanceof String s) {
            return resolveExpressions(s, wm);
        }
        if (body instanceof Map<?, ?> map) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                resolved.put(String.valueOf(entry.getKey()), resolveBodyExpressions(entry.getValue(), wm));
            }
            return resolved;
        }
        if (body instanceof List<?> list) {
            return list.stream().map(item -> resolveBodyExpressions(item, wm)).toList();
        }
        return body;
    }
}
