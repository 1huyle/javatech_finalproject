package com.example.demo.model.enums;

public enum RequestStatus {
    PENDING("Đang chờ xử lý"),    // Đang chờ xử lý
    COMPLETED("Hoàn thành"),  // Hoàn thành
    CANCELLED("Đã hủy");   // Đã hủy

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 