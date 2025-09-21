package com.verifix.usersync.model.keycloak;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakCredential(
        String type,
        String credentialData,
        String secretData,
        Boolean temporary
) {}