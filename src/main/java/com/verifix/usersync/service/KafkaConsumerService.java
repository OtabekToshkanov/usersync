package com.verifix.usersync.service;

import com.verifix.usersync.mapper.MessageMapper;
import com.verifix.usersync.model.UserData;
import com.verifix.usersync.model.debezium.DebeziumMessage;
import com.verifix.usersync.model.debezium.DebeziumOperation;
import com.verifix.usersync.model.debezium.DebeziumPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final ObjectMapper objectMapper;
    private final MessageMapper messageMapper;
    private final UserSyncService userSyncService;

    @KafkaListener(topics = "${app.kafka.topic}")
    public void consumeUserChanges(@Payload String message,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        log.info("Received message from topic: {}, partition: {}", topic, partition);

        try {
            processMessage(message);
        } catch (JsonProcessingException e) {
            log.error("JSON processing error for message: {}", e.getMessage());
            logProblematicMessage(message);
        } catch (IllegalArgumentException e) {
            log.error("Invalid message format: {}", e.getMessage());
            logProblematicMessage(message);
        } catch (Exception e) {
            log.error("Unexpected error processing message: {}", e.getMessage(), e);
            logProblematicMessage(message);
        }
    }

    private void logProblematicMessage(String message) {
        log.error("Problematic message: {}", message);
    }

    private void processMessage(String messageValue) throws JsonProcessingException {
        if (messageValue == null || messageValue.trim().isEmpty()) {
            log.warn("Received message with empty value, skipping");
            return;
        }

        // Parse message value
        DebeziumMessage debeziumMessage;
        try {
            debeziumMessage = objectMapper.readValue(messageValue, DebeziumMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Debezium message: {}", e.getMessage());
            throw e;
        }

        if (debeziumMessage.payload() == null) {
            log.warn("Received message with no payload, skipping");
            return;
        }

        // Parse payload
        DebeziumPayload payload;
        try {
            payload = objectMapper.treeToValue(debeziumMessage.payload(), DebeziumPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Debezium payload: {}", e.getMessage());
            throw e;
        }

        // Skip messages with no relevant changes
        if (!messageMapper.hasRelevantChanges(payload.before(), payload.after())) {
            String userId = extractUserId(payload);
            log.info("Skipping message: no changes in tracked columns for userId {}", userId);
            return;
        }

        // Map to user data
        UserData userData;
        try {
            userData = messageMapper.mapToUserData(payload);
        } catch (Exception e) {
            log.error("Failed to map message to user data: {}", e.getMessage());
            throw e;
        }

        // Validate user data
        if (userData.userId() == null) {
            log.error("Invalid user data: login: {}", userData.login());
            throw new IllegalArgumentException("Invalid user data: missing userId");
        }

        try {
            DebeziumOperation operation = DebeziumOperation.fromCode(payload.operation());

            log.info("Processing {} operation for userId: {}, login: {}", operation, userData.userId(), userData.login());

            switch (operation) {
                case READ -> {
                    log.debug("Processing user.read event for userId: {}, login: {}", userData.userId(), userData.login());
                    userSyncService.handleUserSave(userData);
                }
                case CREATE -> {
                    log.debug("Processing user.add event for userId: {}, login: {}", userData.userId(), userData.login());
                    userSyncService.handleUserSave(userData);
                }
                case UPDATE -> {
                    log.debug("Processing user.edit event for userId: {}, login: {}", userData.userId(), userData.login());
                    userSyncService.handleUserSave(userData);
                }
                case DELETE -> {
                    log.debug("Processing user.delete event for userId: {}, login: {}", userData.userId(), userData.login());
                    userSyncService.handleUserDelete(userData);
                }
            }

            log.info("Successfully processed {} operation for userId: {}, login: {}", operation, userData.userId(), userData.login());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown operation type: {} for userId: {}, login: {}", payload.operation(), userData.userId(), userData.login());
            throw e;
        }
    }

    private String extractUserId(DebeziumPayload payload) {
        if (payload.after() != null && payload.after().has("USER_ID")) {
            return payload.after().get("USER_ID").asText();
        }
        if (payload.before() != null && payload.before().has("USER_ID")) {
            return payload.before().get("USER_ID").asText();
        }
        return "unknown";
    }
}