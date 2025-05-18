package com.example.demo.controller;

import com.example.demo.model.Property;
import com.example.demo.model.PropertyType;
import com.example.demo.model.Transaction;
import com.example.demo.service.PropertyService;
import com.example.demo.service.PropertyTypeService;
import com.example.demo.service.TransactionService;
import com.example.demo.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {
    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyTypeService propertyTypeService;

    @Autowired(required = false)
    private TransactionService transactionService;

    @Autowired(required = false)
    private UserService userService;

    // Trang chủ user
    @GetMapping("/index")
    public String home() {
        return "index";
    }

    // Danh sách bất động sản cho user - đổi URL để tránh xung đột với PageController
    @GetMapping("/dashboard/listings")
    public String listings(
            Model model,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long propertyTypeId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea
    ) {
        List<Property> properties;
        
        // Áp dụng các điều kiện tìm kiếm
        if (keyword != null && !keyword.isEmpty()) {
            properties = propertyService.searchProperties(keyword);
        } else if (propertyTypeId != null) {
            PropertyType propertyType = propertyTypeService.getPropertyTypeById(propertyTypeId)
                    .orElseThrow(() -> new EntityNotFoundException("Property type not found"));
            properties = propertyService.getPropertiesByType(propertyType);
        } else if (minPrice != null && maxPrice != null) {
            properties = propertyService.getPropertiesByPriceRange(minPrice, maxPrice);
        } else if (minArea != null && maxArea != null) {
            properties = propertyService.getPropertiesByAreaRange(minArea, maxArea);
        } else if (minPrice != null) {
            properties = propertyService.getPropertiesByPriceRange(minPrice, Double.MAX_VALUE);
        } else if (maxPrice != null) {
            properties = propertyService.getPropertiesByPriceRange(0, maxPrice);
        } else if (minArea != null) {
            properties = propertyService.getPropertiesByAreaRange(minArea, Double.MAX_VALUE);
        } else if (maxArea != null) {
            properties = propertyService.getPropertiesByAreaRange(0, maxArea);
        } else {
            properties = propertyService.getAllProperties();
        }
        
        List<PropertyType> propertyTypes = propertyTypeService.getAllPropertyTypes();
        
        model.addAttribute("properties", properties);
        model.addAttribute("propertyTypes", propertyTypes);
        model.addAttribute("keyword", keyword);
        model.addAttribute("propertyTypeId", propertyTypeId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minArea", minArea);
        model.addAttribute("maxArea", maxArea);
        
        return "listings";
    }

    // Chi tiết bất động sản cho user
    @GetMapping("/listing-detail/{id}")
    public String listingDetail(@PathVariable Long id, Model model) {
        Property property = propertyService.getPropertyById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        model.addAttribute("property", property);
        
        // Get and add similar properties to the model
        List<Property> similarProperties = propertyService.getSimilarProperties(property, 4);
        model.addAttribute("similarProperties", similarProperties);
        
        return "listing-detail";
    }

    // Trang dashboard admin - ĐÃ CHUYỂN SANG AdminController.java
    // @GetMapping("/admin/dashboard")
    // @PreAuthorize("hasRole('ADMIN')")
    // public String adminDashboard(Model model) { ... }

    // Quản lý bất động sản admin - ĐÃ CHUYỂN SANG AdminController.java
    // @GetMapping("/admin/properties")
    // @PreAuthorize("hasRole('ADMIN')")
    // public String adminProperties(Model model) { ... }

    @GetMapping("/account")
    // Removing the @PreAuthorize annotation to allow access to the page
    // Client-side authentication is handled in account.js
    public String userAccount() {
        return "account";
    }
}
