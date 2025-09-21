package com.verifix.usersync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@ConfigurationProperties(prefix = "app")
@Validated
public record ApplicationProperties(
        Kafka kafka,
        Keycloak keycloak,
        @NotEmpty List<String> trackedColumns
) {

    public record Kafka(
            @NotEmpty String topic,
            @NotEmpty String clientId
    ) {
    }

    public record Keycloak(
            @NotEmpty String baseUrl,
            @NotEmpty String realm,
            @NotEmpty String adminClientId,
            @NotEmpty String adminClientSecret
    ) {
    }
}