package com.verifix.usersync.model.debezium;

import lombok.Getter;

@Getter
public enum DebeziumOperation {
    READ("r"),
    CREATE("c"),
    UPDATE("u"),
    DELETE("d");

    private final String code;

    DebeziumOperation(String code) {
        this.code = code;
    }

    public static DebeziumOperation fromCode(String code) {
        for (DebeziumOperation op : values()) {
            if (op.code.equals(code)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operation code: " + code);
    }
}
