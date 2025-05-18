package com.example.demo.service;

import com.example.demo.dto.PropertyTypeDTO;
import com.example.demo.model.PropertyType;

import java.util.List;
import java.util.Optional;

public interface PropertyTypeService {
    PropertyType createPropertyType(PropertyTypeDTO propertyTypeDTO);
    PropertyType updatePropertyType(Long id, PropertyTypeDTO propertyTypeDTO);
    void deletePropertyType(Long id);
    Optional<PropertyType> getPropertyTypeById(Long id);
    Optional<PropertyType> getPropertyTypeByName(String name);
    List<PropertyType> getAllPropertyTypes();
    boolean existsByName(String name);
} 