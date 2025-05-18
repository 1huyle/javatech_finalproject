package com.example.demo.model;

import com.example.demo.model.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "property_image")
public class PropertyImage extends BaseEntity {
    @NotBlank
    @Column(nullable = false)
    private String imageUrl;

    @NotNull
    @Column(nullable = false)
    private Boolean primary = false;

    @Column
    private Integer order;

    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    public PropertyImage() {
        super();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }
} 