package com.example.demo.dto;

public class PropertyImageDTO {
    private Long id;
    private String imageUrl;
    private boolean isPrimary; // Optional: to indicate primary image

    public PropertyImageDTO() {
    }

    public PropertyImageDTO(Long id, String imageUrl, boolean isPrimary) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
} 