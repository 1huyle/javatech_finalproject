package com.example.demo.service.impl;

import com.example.demo.dto.PropertyTypeDTO;
import com.example.demo.model.PropertyType;
import com.example.demo.repository.PropertyTypeRepository;
import com.example.demo.service.PropertyTypeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PropertyTypeServiceImpl implements PropertyTypeService {

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Override
    public PropertyType createPropertyType(PropertyTypeDTO propertyTypeDTO) {
        if (propertyTypeRepository.existsByName(propertyTypeDTO.getName())) {
            throw new IllegalArgumentException("Property type with this name already exists");
        }

        PropertyType propertyType = new PropertyType();
        propertyType.setName(propertyTypeDTO.getName());
        propertyType.setDescription(propertyTypeDTO.getDescription());

        return propertyTypeRepository.save(propertyType);
    }

    @Override
    public PropertyType updatePropertyType(Long id, PropertyTypeDTO propertyTypeDTO) {
        PropertyType propertyType = propertyTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property type not found"));

        if (!propertyType.getName().equals(propertyTypeDTO.getName()) &&
            propertyTypeRepository.existsByName(propertyTypeDTO.getName())) {
            throw new IllegalArgumentException("Property type with this name already exists");
        }

        propertyType.setName(propertyTypeDTO.getName());
        propertyType.setDescription(propertyTypeDTO.getDescription());

        return propertyTypeRepository.save(propertyType);
    }

    @Override
    public void deletePropertyType(Long id) {
        if (!propertyTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("Property type not found");
        }
        propertyTypeRepository.deleteById(id);
    }

    @Override
    public Optional<PropertyType> getPropertyTypeById(Long id) {
        return propertyTypeRepository.findById(id);
    }

    @Override
    public Optional<PropertyType> getPropertyTypeByName(String name) {
        return propertyTypeRepository.findByName(name);
    }

    @Override
    public List<PropertyType> getAllPropertyTypes() {
        return propertyTypeRepository.findAll();
    }

    @Override
    public boolean existsByName(String name) {
        return propertyTypeRepository.existsByName(name);
    }
} 