package io.ruleforge.event.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookAuthConfigDto {
    private String headerName;   // not masked
    private String username;     // not masked
    // All credential fields masked to [REDACTED] in response
    private String apiKey;
    private String password;
    private String token;
    private String secret;
    private String headerValue;

    public WebhookAuthConfigDto() {}

    private WebhookAuthConfigDto(Builder builder) {
        this.headerName = builder.headerName;
        this.username = builder.username;
        this.apiKey = builder.apiKey;
        this.password = builder.password;
        this.token = builder.token;
        this.secret = builder.secret;
        this.headerValue = builder.headerValue;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String headerName;
        private String username;
        private String apiKey;
        private String password;
        private String token;
        private String secret;
        private String headerValue;

        public Builder headerName(String headerName) { this.headerName = headerName; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder token(String token) { this.token = token; return this; }
        public Builder secret(String secret) { this.secret = secret; return this; }
        public Builder headerValue(String headerValue) { this.headerValue = headerValue; return this; }
        public WebhookAuthConfigDto build() { return new WebhookAuthConfigDto(this); }
    }

    public String getHeaderName() { return headerName; }
    public void setHeaderName(String headerName) { this.headerName = headerName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getHeaderValue() { return headerValue; }
    public void setHeaderValue(String headerValue) { this.headerValue = headerValue; }
}
