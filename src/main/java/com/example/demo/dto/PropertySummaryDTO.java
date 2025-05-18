package com.example.demo.dto;

import com.example.demo.model.enums.PropertyStatus;

public class PropertySummaryDTO {
    private Long id;
    private String name;
    private String address;
    private String imageUrl;
    private String propertyTypeName;
    private Double price;
    private String status;

    // Constructors
    public PropertySummaryDTO() {}

    public PropertySummaryDTO(Long id, String name, String address, String imageUrl, String propertyTypeName, Double price, PropertyStatus status) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.imageUrl = imageUrl;
        this.propertyTypeName = propertyTypeName;
        this.price = price;
        if (status != null) {
            this.status = status.name(); 
        } else {
            this.status = "N/A";
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPropertyTypeName() { return propertyTypeName; }
    public void setPropertyTypeName(String propertyTypeName) { this.propertyTypeName = propertyTypeName; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    // Overload để nhận enum từ controller dễ hơn nếu Property.getStatus() trả về enum
    public void setStatus(PropertyStatus status) { 
        if (status != null) {
            this.status = status.name();
        } else {
            this.status = "N/A";
        }
    }
} 