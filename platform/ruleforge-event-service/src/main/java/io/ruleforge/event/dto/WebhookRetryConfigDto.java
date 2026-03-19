package io.ruleforge.event.dto;

import java.math.BigDecimal;
import java.util.List;

public class WebhookRetryConfigDto {
    private int initialDelaySeconds;
    private BigDecimal backoffMultiplier;
    private int maxAttempts;
    private List<Integer> retryOnStatusCodes;

    public WebhookRetryConfigDto() {}

    private WebhookRetryConfigDto(Builder builder) {
        this.initialDelaySeconds = builder.initialDelaySeconds;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.maxAttempts = builder.maxAttempts;
        this.retryOnStatusCodes = builder.retryOnStatusCodes;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int initialDelaySeconds;
        private BigDecimal backoffMultiplier;
        private int maxAttempts;
        private List<Integer> retryOnStatusCodes;

        public Builder initialDelaySeconds(int initialDelaySeconds) { this.initialDelaySeconds = initialDelaySeconds; return this; }
        public Builder backoffMultiplier(BigDecimal backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; return this; }
        public Builder maxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; return this; }
        public Builder retryOnStatusCodes(List<Integer> retryOnStatusCodes) { this.retryOnStatusCodes = retryOnStatusCodes; return this; }
        public WebhookRetryConfigDto build() { return new WebhookRetryConfigDto(this); }
    }

    public int getInitialDelaySeconds() { return initialDelaySeconds; }
    public void setInitialDelaySeconds(int initialDelaySeconds) { this.initialDelaySeconds = initialDelaySeconds; }

    public BigDecimal getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(BigDecimal backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public List<Integer> getRetryOnStatusCodes() { return retryOnStatusCodes; }
    public void setRetryOnStatusCodes(List<Integer> retryOnStatusCodes) { this.retryOnStatusCodes = retryOnStatusCodes; }
}
