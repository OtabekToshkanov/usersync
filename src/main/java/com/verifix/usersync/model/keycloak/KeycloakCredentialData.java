package com.verifix.usersync.model.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakCredentialData(
        String algorithm,
        Integer hashIterations,
        Map<String, Object> additionalParameters
) {
}