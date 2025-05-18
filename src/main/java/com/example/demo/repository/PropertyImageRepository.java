package com.example.demo.repository;

import com.example.demo.model.Property;
import com.example.demo.model.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {
    List<PropertyImage> findByProperty(Property property);
    List<PropertyImage> findByPropertyOrderByOrderAsc(Property property);
    PropertyImage findByPropertyAndPrimaryTrue(Property property);
} 