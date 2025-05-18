package com.example.demo.model;

import com.example.demo.model.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wishlist")
public class Wishlist extends BaseEntity {
    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany
    @JoinTable(
        name = "wishlist_property",
        joinColumns = @JoinColumn(name = "wishlist_id"),
        inverseJoinColumns = @JoinColumn(name = "property_id")
    )
    private List<Property> properties;

    public Wishlist() {
        super();
        this.properties = new ArrayList<>();
    }

    public Wishlist(User user) {
        this.user = user;
        this.properties = new ArrayList<>();
    }

    public boolean containsProperty(Property property) {
        return properties != null && properties.contains(property);
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }
} 