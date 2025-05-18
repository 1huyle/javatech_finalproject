package com.example.demo.model;

import com.example.demo.model.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Entity
@Table(name = "property_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "properties"}) 
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class PropertyType extends BaseEntity {
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    @Size(min = 10)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "propertyType", cascade = CascadeType.ALL)
    private List<Property> properties;

    public PropertyType() {
        super();
    }

    public PropertyType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }
} 