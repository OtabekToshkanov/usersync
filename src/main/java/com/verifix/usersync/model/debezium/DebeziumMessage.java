package com.verifix.usersync.model.debezium;

import com.fasterxml.jackson.databind.JsonNode;

public record DebeziumMessage(JsonNode payload) {
}

