package com.verifix.usersync.service;

import com.verifix.usersync.config.ApplicationProperties;
import com.verifix.usersync.mapper.KeycloakMapper;
import com.verifix.usersync.model.UserData;
import com.verifix.usersync.model.keycloak.KeycloakUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@Slf4j
public class KeycloakService {
    private final RestTemplate restTemplate;
    private final KeycloakMapper keycloakMapper;
    private final OAuth2TokenService tokenService;
    private final String baseUrl;
    private final String realm;

    public KeycloakService(RestTemplate restTemplate,
                           ApplicationProperties properties,
                           KeycloakMapper keycloakMapper,
                           OAuth2TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.keycloakMapper = keycloakMapper;
        this.tokenService = tokenService;
        this.baseUrl = normalizeUrl(properties.keycloak().baseUrl());
        this.realm = properties.keycloak().realm();
    }

    /**
     * Find a user in Keycloak by user_id (stored as attribute)
     */
    public KeycloakUser findUserByExternalId(Long userId) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/admin/realms/{realm}/users")
                    .queryParam("q", "user_id:" + userId)
                    .buildAndExpand(realm)
                    .toUriString();

            ResponseEntity<List<KeycloakUser>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            List<KeycloakUser> users = response.getBody();

            if (users != null && !users.isEmpty()) {
                log.info("Found user in Keycloak with user_id: {}", userId);
                return users.getFirst();
            }

            return null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.error("Error finding user by user_id {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to find user by external ID", e);
        }
    }

    /**
     * Create a new user in Keycloak
     */
    public void createUser(UserData userData) {
        try {
            HttpHeaders headers = createAuthHeaders();
            KeycloakUser keycloakUser = keycloakMapper.mapToKeycloakUser(userData);
            HttpEntity<KeycloakUser> entity = new HttpEntity<>(keycloakUser, headers);

            String url = baseUrl + "/admin/realms/" + realm + "/users";

            log.info("Creating user in Keycloak: userID: {}, login: {}", userData.userId(), userData.login());

            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            // Get the created user's ID from the Location header
            String locationHeader = response.getHeaders().getFirst("Location");
            if (locationHeader != null) {
                String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
                log.info("User created in Keycloak: userID: {}, login: {}, ID: {}", userData.userId(), userData.login(), keycloakUserId);
            }

            throw new RuntimeException("Failed to get created user ID from response");
        } catch (Exception e) {
            log.error("Error creating user in Keycloak: userID: {}, login: {}. {}", userData.userId(), userData.login(), e.getMessage());
            throw new RuntimeException("Failed to create user", e);
        }
    }

    /**
     * Update an existing user in Keycloak
     */
    public void updateUser(String keycloakId, UserData userData) {
        try {
            HttpHeaders headers = createAuthHeaders();
            KeycloakUser keycloakUser = keycloakMapper.mapToKeycloakUser(userData);
            HttpEntity<KeycloakUser> entity = new HttpEntity<>(keycloakUser, headers);

            String url = baseUrl + "/admin/realms/" + realm + "/users/" + keycloakId;

            log.info("Updating user in Keycloak: userID: {}, login: {}", userData.userId(), userData.login());
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);

            log.info("User updated in Keycloak: userID: {}, login: {}", userData.userId(), userData.login());
        } catch (Exception e) {
            log.error("Error updating user in Keycloak: userID: {}, login: {}. {}", userData.userId(), userData.login(), e.getMessage());
            throw new RuntimeException("Failed to update user", e);
        }
    }

    /**
     * Delete a user from Keycloak
     */
    public void deleteUser(String keycloakId, UserData userData) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = baseUrl + "/admin/realms/" + realm + "/users/" + keycloakId;

            log.info("Deleting user from Keycloak: userID: {}, login: {}", userData.userId(), userData.login());
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

            log.info("User deleted from Keycloak: userID: {}, login: {}", userData.userId(), userData.login());
        } catch (Exception e) {
            log.error("Error deleting user from Keycloak: userID: {}, login: {}. {}", userData.userId(), userData.login(), e.getMessage());
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenService.getAccessToken());
        return headers;
    }

    private String normalizeUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}