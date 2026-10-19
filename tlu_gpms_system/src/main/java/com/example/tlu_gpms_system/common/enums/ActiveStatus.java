package com.example.tlu_gpms_system.common.enums;

import lombok.Getter;

@Getter
public enum ActiveStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động");

    private final String description;

    ActiveStatus(String description) {
        this.description = description;
    }

    public static ActiveStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (ActiveStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized) || status.getDescription().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
