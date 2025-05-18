package com.example.demo.controller;

import com.example.demo.dto.PropertyTypeDTO;
import com.example.demo.model.PropertyType;
import com.example.demo.service.PropertyTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/property-types")
public class PropertyTypeController {

    @Autowired
    private PropertyTypeService propertyTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyType> createPropertyType(@Valid @RequestBody PropertyTypeDTO propertyTypeDTO) {
        return ResponseEntity.ok(propertyTypeService.createPropertyType(propertyTypeDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyType> updatePropertyType(
            @PathVariable Long id,
            @Valid @RequestBody PropertyTypeDTO propertyTypeDTO) {
        return ResponseEntity.ok(propertyTypeService.updatePropertyType(id, propertyTypeDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePropertyType(@PathVariable Long id) {
        propertyTypeService.deletePropertyType(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyType> getPropertyTypeById(@PathVariable Long id) {
        return propertyTypeService.getPropertyTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<PropertyType> getPropertyTypeByName(@PathVariable String name) {
        return propertyTypeService.getPropertyTypeByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PropertyType>> getAllPropertyTypes() {
        return ResponseEntity.ok(propertyTypeService.getAllPropertyTypes());
    }

    @GetMapping("/exists/{name}")
    public ResponseEntity<Boolean> existsByName(@PathVariable String name) {
        return ResponseEntity.ok(propertyTypeService.existsByName(name));
    }
} 