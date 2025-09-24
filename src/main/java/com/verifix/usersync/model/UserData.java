package com.verifix.usersync.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserData(
        @JsonProperty("company_id") Long companyId,
        @JsonProperty("user_id") Long userId,
        String name,
        String login,
        String password,
        String email
) {
    public String getFirstName() {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        String[] parts = name.trim().split("\\s+");
        return parts[0];
    }

    public String getLastName() {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        String[] parts = name.trim().split("\\s+");
        if (parts.length > 1) {
            return String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        }
        return "";
    }
}