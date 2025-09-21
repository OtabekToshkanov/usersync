package com.verifix.usersync.model.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakSecretData(String value) {
}