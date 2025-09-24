package com.verifix.usersync.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.verifix.usersync.config.ApplicationProperties;
import com.verifix.usersync.model.UserData;
import com.verifix.usersync.model.debezium.DebeziumPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
public class MessageMapper {
    private final List<String> trackedColumns;

    public MessageMapper(ApplicationProperties properties) {
        this.trackedColumns = properties.trackedColumns();
    }

    /**
     * Check if relevant fields have changed
     */
    public boolean hasRelevantChanges(JsonNode before, JsonNode after) {
        // For new records, all tracked fields are considered changed
        if (before == null && after != null) {
            return true;
        }
        // For deleted records, all tracked fields are considered changed
        if (before != null && after == null) {
            return true;
        }
        // For updates, check if any tracked field changed
        if (after != null) {
            return trackedColumns.stream()
                    .anyMatch(column -> !nodeEquals(before.get(column), after.get(column)));
        }

        return false;
    }

    /**
     * Map Debezium message payload to user object
     */
    public UserData mapToUserData(DebeziumPayload payload) {
        JsonNode data = payload.after() != null ? payload.after() : payload.before();

        return new UserData(
                getLongValue(data.get("COMPANY_ID")),
                getLongValue(data.get("USER_ID")),
                getTextValue(data.get("NAME")),
                getTextValue(data.get("LOGIN")),
                getTextValue(data.get("PASSWORD")),
                getTextValue(data.get("EMAIL"))
        );
    }

    private Long getLongValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        try {
            return Long.parseLong(node.asText());
        } catch (NumberFormatException e) {
            return decodeBase64Number(node.asText());
        }
    }

    /**
     * Decode a base64 encoded string into a number
     */
    private Long decodeBase64Number(String value) {
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            return new BigInteger(1, bytes).longValue(); // 1 = positive sign
        } catch (Exception e) {
            log.error("Failed to decode base64 number: {}", e.getMessage());
            return null;
        }
    }

    private String getTextValue(JsonNode node) {
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private boolean nodeEquals(JsonNode a, JsonNode b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

}