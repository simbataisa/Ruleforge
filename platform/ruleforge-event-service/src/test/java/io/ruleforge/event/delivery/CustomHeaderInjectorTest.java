package io.ruleforge.event.delivery;

import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.*;

class CustomHeaderInjectorTest {

    private final CustomHeaderInjector injector = new CustomHeaderInjector();

    @Test
    void supportedType_isCustomHeader() {
        assertThat(injector.supportedType()).isEqualTo(WebhookAuthType.CUSTOM_HEADER);
    }

    @Test
    void injectAuth_addsConfiguredHeader() {
        HttpHeaders headers = new HttpHeaders();
        injector.injectAuth(headers, new WebhookAuthConfig.CustomHeader("X-Service-Token", "secret123"), new byte[0]);
        assertThat(headers.getFirst("X-Service-Token")).isEqualTo("secret123");
    }

    @Test
    void injectAuth_defaultsHeaderNameWhenBlank() {
        HttpHeaders headers = new HttpHeaders();
        injector.injectAuth(headers, new WebhookAuthConfig.CustomHeader("", "myvalue"), new byte[0]);
        assertThat(headers.getFirst("X-Custom-Header")).isEqualTo("myvalue");
    }

    @Test
    void injectAuth_defaultsHeaderNameWhenNull() {
        HttpHeaders headers = new HttpHeaders();
        injector.injectAuth(headers, new WebhookAuthConfig.CustomHeader(null, "myvalue"), new byte[0]);
        assertThat(headers.getFirst("X-Custom-Header")).isEqualTo("myvalue");
    }

    @Test
    void injectAuth_throwsAuthException_whenHeaderValueBlank() {
        assertThatThrownBy(() ->
            injector.injectAuth(new HttpHeaders(), new WebhookAuthConfig.CustomHeader("X-Hdr", ""), new byte[0])
        ).isInstanceOf(WebhookAuthException.class)
         .hasMessageContaining("headerValue");
    }

    @Test
    void injectAuth_throwsAuthException_whenHeaderValueNull() {
        assertThatThrownBy(() ->
            injector.injectAuth(new HttpHeaders(), new WebhookAuthConfig.CustomHeader("X-Hdr", null), new byte[0])
        ).isInstanceOf(WebhookAuthException.class);
    }

    @Test
    void injectAuth_doesNotAddAuthorizationHeader() {
        HttpHeaders headers = new HttpHeaders();
        injector.injectAuth(headers, new WebhookAuthConfig.CustomHeader("X-Custom", "value"), new byte[0]);
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }
}
