package com.verifix.usersync.model.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakUser(
        String id,
        String username,
        Boolean enabled,
        String firstName,
        String lastName,
        String email,
        List<KeycloakCredential> credentials,
        Map<String, List<String>> attributes
) {
}
