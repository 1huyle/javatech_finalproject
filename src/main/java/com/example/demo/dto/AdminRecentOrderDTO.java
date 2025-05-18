package com.example.demo.dto;

import java.time.LocalDateTime;

public class AdminRecentOrderDTO {
    private Long id;
    private String customerName;
    private String propertyName;
    private String amount; // Hoặc BigDecimal/Double tùy theo cách bạn muốn xử lý
    private String status; // Có thể là String hoặc Enum tùy theo hiển thị
    private String statusDisplay; // Tên trạng thái hiển thị (ví dụ: "Đang chờ xử lý")
    private LocalDateTime createdAt;

    public AdminRecentOrderDTO() {
    }

    public AdminRecentOrderDTO(Long id, String customerName, String propertyName, String amount, String status, String statusDisplay, LocalDateTime createdAt) {
        this.id = id;
        this.customerName = customerName;
        this.propertyName = propertyName;
        this.amount = amount;
        this.status = status;
        this.statusDisplay = statusDisplay;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDisplay() {
        return statusDisplay;
    }

    public void setStatusDisplay(String statusDisplay) {
        this.statusDisplay = statusDisplay;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 