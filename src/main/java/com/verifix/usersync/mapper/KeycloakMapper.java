package com.verifix.usersync.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.icu.text.Transliterator;
import com.verifix.usersync.model.UserData;
import com.verifix.usersync.model.keycloak.KeycloakCredential;
import com.verifix.usersync.model.keycloak.KeycloakCredentialData;
import com.verifix.usersync.model.keycloak.KeycloakSecretData;
import com.verifix.usersync.model.keycloak.KeycloakUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakMapper {
    private static final Transliterator transliterator = Transliterator.getInstance("Cyrillic-Latin");
    private final ObjectMapper objectMapper;

    public KeycloakUser mapToKeycloakUser(UserData userData) {
        return new KeycloakUser(
                null, // ID will be set by Keycloak
                prepareLogin(userData),
                true,
                userData.getFirstName(),
                userData.getLastName(),
                userData.email(),
                prepareCredentials(userData),
                prepareAttributes(userData)
        );


    }

    private String prepareLogin(UserData userData) {
        String transliterated = transliterator.transliterate( userData.login());

        // Remove all characters except alphanumeric, @, dash, underscore, and dot
        // Replace spaces with underscores
        return transliterated
                .toLowerCase()
                .replace(" ", "_")  // or use .replace(" ", "") to remove spaces
                .replaceAll("[^a-z0-9@._-]", "");
    }

    private List<KeycloakCredential> prepareCredentials(UserData userData) {
        if (userData.password() == null || userData.password().isEmpty()) {
            return null;
        }

        try {
            KeycloakCredentialData credentialData = new KeycloakCredentialData("SHA-1", -1, Map.of());
            KeycloakSecretData secretData = new KeycloakSecretData(userData.password().toLowerCase());

            KeycloakCredential credential = new KeycloakCredential(
                    "password",
                    objectMapper.writeValueAsString(credentialData),
                    objectMapper.writeValueAsString(secretData),
                    false
            );

            return List.of(credential);
        } catch (JsonProcessingException e) {
            log.error("Error mapping user data to Keycloak format: {}", e.getMessage());
            throw new RuntimeException("Failed to map user data", e);
        }
    }

    private Map<String, List<String>> prepareAttributes(UserData userData) {
        return Map.of("fullName", List.of(userData.name()),
                "companyId", List.of(userData.companyId().toString()),
                "userId", List.of(userData.userId().toString()));
    }
}