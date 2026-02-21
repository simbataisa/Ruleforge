package io.ruleforge.sdk;

/**
 * Main entry point for the RuleForge Java SDK.
 *
 * <p>Supports API key and JWT authentication.
 *
 * <pre>{@code
 * RuleForgeClient client = new RuleForgeClient("https://api.ruleforge.io", "your-api-key");
 * Map<String, Object> result = client.solver().solve("loan-eligibility", inputMap);
 * }</pre>
 */
public class RuleForgeClient {

    private final String baseUrl;
    private final String apiKey;
    private final String jwtToken;

    /**
     * Creates a client authenticated with an API key.
     *
     * @param baseUrl the RuleForge base URL (e.g. {@code https://api.ruleforge.io})
     * @param apiKey  the API key used for authentication via {@code X-API-Key} header
     */
    public RuleForgeClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, null);
    }

    /**
     * Creates a client with explicit API key or JWT token.
     *
     * @param baseUrl   the RuleForge base URL
     * @param apiKey    API key for {@code X-API-Key} header authentication (may be null)
     * @param jwtToken  JWT token for {@code Authorization: Bearer} header authentication (may be null)
     */
    public RuleForgeClient(String baseUrl, String apiKey, String jwtToken) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be null or blank");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", ""); // strip trailing slash
        this.apiKey = apiKey;
        this.jwtToken = jwtToken;
    }

    /**
     * Returns the solver API for executing rules.
     */
    public SolverApi solver() {
        return new SolverApi(baseUrl, apiKey, jwtToken);
    }

    /**
     * Returns the rules API for managing rule definitions.
     */
    public RulesApi rules() {
        return new RulesApi(baseUrl, apiKey, jwtToken);
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
