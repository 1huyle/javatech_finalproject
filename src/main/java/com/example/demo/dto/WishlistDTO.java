package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class WishlistDTO {
    private Long id;

    @NotNull
    private Long userId;

    private List<Long> propertyIds;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getPropertyIds() {
        return propertyIds;
    }

    public void setPropertyIds(List<Long> propertyIds) {
        this.propertyIds = propertyIds;
    }
} 