package io.ruleforge.event.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

public class WebhookRetryConfigRequest {

    @Min(1) @Max(3600)
    private Integer initialDelaySeconds;

    @DecimalMin("1.0") @DecimalMax("10.0")
    private BigDecimal backoffMultiplier;

    @Min(1) @Max(25)
    private Integer maxAttempts;

    private List<Integer> retryOnStatusCodes;

    public Integer getInitialDelaySeconds() { return initialDelaySeconds; }
    public void setInitialDelaySeconds(Integer initialDelaySeconds) { this.initialDelaySeconds = initialDelaySeconds; }

    public BigDecimal getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(BigDecimal backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }

    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    public List<Integer> getRetryOnStatusCodes() { return retryOnStatusCodes; }
    public void setRetryOnStatusCodes(List<Integer> retryOnStatusCodes) { this.retryOnStatusCodes = retryOnStatusCodes; }
}
