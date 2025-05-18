package com.example.demo.dto;

public class WishlistActionRequest {
    private Long userId; // Sẽ được thay thế bằng Principal trong controller
    private Long propertyId;

    // Constructors, Getters and Setters
    public WishlistActionRequest() {
    }

    public WishlistActionRequest(Long userId, Long propertyId) {
        this.userId = userId;
        this.propertyId = propertyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }
} 