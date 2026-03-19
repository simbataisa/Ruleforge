package io.ruleforge.event.delivery;

import io.ruleforge.event.model.WebhookAuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WebhookAuthInjectorRegistry {

    private final Map<WebhookAuthType, WebhookAuthInjector> injectors;

    public WebhookAuthInjectorRegistry(List<WebhookAuthInjector> injectorList) {
        this.injectors = injectorList.stream()
                .collect(Collectors.toMap(WebhookAuthInjector::supportedType, Function.identity()));
    }

    public WebhookAuthInjector getFor(WebhookAuthType type) {
        return injectors.getOrDefault(type, injectors.get(WebhookAuthType.NONE));
    }
}
