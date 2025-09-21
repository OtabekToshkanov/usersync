package com.verifix.usersync.service;

import com.verifix.usersync.model.UserData;
import com.verifix.usersync.model.keycloak.KeycloakUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserSyncService {
    private final KeycloakService keycloakService;

    public UserSyncService(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    public void handleUserSave(UserData userData) {
        log.info("Processing user save for user_id: {} and login: {}", userData.userId(), userData.login());

        try {
            KeycloakUser existingUser = keycloakService.findUserByExternalId(userData.userId());

            if (existingUser != null) {
                keycloakService.updateUser(existingUser.id(), userData);
            } else {
                keycloakService.createUser(userData);
            }

            log.info("Successfully processed user save user_id: {} and login: {}", userData.userId(), userData.login());
        } catch (Exception e) {
            log.error("Error in handleUserSave for user_id: {} and login: {}. {}", userData.userId(), userData.login(), e.getMessage());
            throw new RuntimeException("Failed to process user save", e);
        }
    }

    public void handleUserDelete(UserData userData) {
        log.info("Processing user delete for user_id: {} and login: {}", userData.userId(), userData.login());

        try {
            KeycloakUser existingUser = keycloakService.findUserByExternalId(userData.userId());

            if (existingUser != null) {
                keycloakService.deleteUser(existingUser.id(), userData);
            }

            log.info("Successfully processed user delete for user_id: {} and login: {}", userData.userId(), userData.login());
        } catch (Exception e) {
            log.error("Error in handleUserDelete for user_id: {} and login: {}. {}", userData.userId(), userData.login(), e.getMessage());
            throw new RuntimeException("Failed to process user delete", e);
        }
    }
}