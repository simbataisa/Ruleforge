package io.ruleforge.event.delivery;

import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class SigningSecretInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.SIGNING_SECRET; }

    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.SigningSecret ss && ss.secret() != null) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(ss.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] hmac = mac.doFinal(payload);
                headers.set("X-RuleForge-Signature", "sha256=" + HexFormat.of().formatHex(hmac));
            } catch (Exception e) {
                throw new RuntimeException("Failed to compute HMAC signature", e);
            }
        }
    }
}
