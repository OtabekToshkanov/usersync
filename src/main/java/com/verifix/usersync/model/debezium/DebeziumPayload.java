package com.verifix.usersync.model.debezium;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record DebeziumPayload(
        @JsonProperty("op") String operation,
        @JsonProperty("before") JsonNode before,
        @JsonProperty("after") JsonNode after
) {
}