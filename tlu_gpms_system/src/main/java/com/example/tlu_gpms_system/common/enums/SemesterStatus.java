package com.example.tlu_gpms_system.common.enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum SemesterStatus {
    UPCOMING("Chưa bắt đầu"),
    ONGOING("Đang diễn ra"),
    COMPLETED("Kết thúc");

    private final String description;

    SemesterStatus(String description) {
        this.description = description;
    }

    @JsonCreator
    public static SemesterStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        for (SemesterStatus status : values()) {
            if (status.name().equalsIgnoreCase(trimmed) || status.description.equalsIgnoreCase(trimmed)) {
                return status;
            }
        }
        return null;
    }
}
