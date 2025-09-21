package com.verifix.usersync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2TokenService {
    private static final Duration EXPIRY_BUFFER = Duration.ofSeconds(30);

    private final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager;
    private OAuth2AuthorizedClient cachedClient;

    /**
     * Get a valid access token, refreshing if necessary
     */
    public String getAccessToken() {
        // Check if we have a valid cached token
        if (cachedClient != null && isTokenValid(cachedClient.getAccessToken())) {
            log.debug("Using cached access token");
            return cachedClient.getAccessToken().getTokenValue();
        }

        log.info("Obtaining new access token");

        // Create authorize request for client credentials grant
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("keycloak")
                .principal("keycloak-service")
                .build();

        // Authorize and get the client
        cachedClient = authorizedClientManager.authorize(authorizeRequest);

        if (cachedClient == null) {
            throw new RuntimeException("Failed to obtain OAuth2 authorized client");
        }

        log.info("Access token obtained successfully");
        return cachedClient.getAccessToken().getTokenValue();
    }

    /**
     * Check if the token is still valid (not expired with buffer)
     */
    private boolean isTokenValid(OAuth2AccessToken token) {
        if (token == null || token.getExpiresAt() == null) {
            return false;
        }

        Instant expiryWithBuffer = token.getExpiresAt().minus(EXPIRY_BUFFER);
        return Instant.now().isBefore(expiryWithBuffer);
    }
}