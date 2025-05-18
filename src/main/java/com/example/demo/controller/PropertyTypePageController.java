package com.example.demo.controller;

import com.example.demo.model.Property;
import com.example.demo.model.PropertyType;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.service.PropertyService;
import com.example.demo.service.PropertyTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for handling property type specific pages (apartments, villas, etc.)
 */
@Controller
public class PropertyTypePageController {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyTypeService propertyTypeService;

    /**
     * Helper method to parse price and area range parameters
     */
    private void parseRangeParams(String priceRange, String areaRange, 
                                Model model, 
                                Double[] priceValues, 
                                Double[] areaValues) {
        // Parse priceRange
        if (priceRange != null && !priceRange.isEmpty()) {
            String[] parts = priceRange.split("-");
            if (parts.length > 0) {
                try {
                    priceValues[0] = Double.parseDouble(parts[0]) * 1_000_000_000;
                    if (parts.length > 1 && !parts[1].equals("+")) {
                        priceValues[1] = Double.parseDouble(parts[1]) * 1_000_000_000;
                    }
                } catch (NumberFormatException e) {
                    // Handle parsing error
                }
            }
        }
        
        // Parse areaRange
        if (areaRange != null && !areaRange.isEmpty()) {
            String[] parts = areaRange.split("-");
            if (parts.length > 0) {
                try {
                    areaValues[0] = Double.parseDouble(parts[0]);
                    if (parts.length > 1 && !parts[1].equals("+")) {
                        areaValues[1] = Double.parseDouble(parts[1]);
                    }
                } catch (NumberFormatException e) {
                    // Handle parsing error
                }
            }
        }

        // Add search parameters to the model
        model.addAttribute("priceRange", priceRange);
        model.addAttribute("areaRange", areaRange);
    }

    /**
     * Helper method to add pagination results to model
     */
    private void addPaginationAttributes(Model model, Page<Property> propertyPage, int page, int size) {
        model.addAttribute("properties", propertyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", propertyPage.getTotalPages());
        model.addAttribute("totalItems", propertyPage.getTotalElements());
        model.addAttribute("size", size);
    }

    private String getTitle(String type) {
        switch (type.toLowerCase()) {
            case "apartment": return "Căn hộ";
            case "villa": return "Biệt thự";
            case "house": return "Nhà phố";
            case "land": return "Đất nền";
            case "commercial": return "Thương mại";
            case "industrial": return "Công nghiệp";
            default: return "Bất động sản";
        }
    }

    private Page<Property> getProperties(String typeName, String keyword, Double minPrice, Double maxPrice, Double minArea, Double maxArea, Pageable pageable) {
        return propertyService.advancedSearch(
            keyword, 
            typeName, 
            minPrice, 
            maxPrice, 
            minArea, 
            maxArea, 
            PropertyStatus.AVAILABLE, 
            null, 
            pageable
        );
    }

    @GetMapping("/apartments")
    public String apartments(Model model,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String priceRange,
                           @RequestParam(required = false) String areaRange,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "9") int size) {
        
        Double[] priceValues = {null, null};
        Double[] areaValues = {null, null};
        
        parseRangeParams(priceRange, areaRange, model, priceValues, areaValues);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Property> propertyPage = getProperties("Căn hộ", keyword, priceValues[0], priceValues[1], areaValues[0], areaValues[1], pageable);
        
        addPaginationAttributes(model, propertyPage, page, size);
        
        // Add keyword to the model
        model.addAttribute("keyword", keyword);
        
        return "apartments";
    }

    @GetMapping("/villas")
    public String villas(Model model,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String priceRange,
                        @RequestParam(required = false) String areaRange,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "9") int size) {
        
        Double[] priceValues = {null, null};
        Double[] areaValues = {null, null};
        
        parseRangeParams(priceRange, areaRange, model, priceValues, areaValues);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Property> propertyPage = getProperties("Biệt thự", keyword, priceValues[0], priceValues[1], areaValues[0], areaValues[1], pageable);
        
        addPaginationAttributes(model, propertyPage, page, size);
        
        // Add keyword to the model
        model.addAttribute("keyword", keyword);
        
        return "villas";
    }

    @GetMapping("/condominiums")
    public String condominiums(Model model,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String priceRange,
                              @RequestParam(required = false) String areaRange,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "9") int size) {
        
        Double[] priceValues = {null, null};
        Double[] areaValues = {null, null};
        
        parseRangeParams(priceRange, areaRange, model, priceValues, areaValues);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Property> propertyPage = getProperties("Chung cư", keyword, priceValues[0], priceValues[1], areaValues[0], areaValues[1], pageable);
        
        addPaginationAttributes(model, propertyPage, page, size);
        
        // Add keyword to the model
        model.addAttribute("keyword", keyword);
        
        return "condominiums";
    }

    @GetMapping("/townhouses")
    public String townhouses(Model model,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String priceRange,
                            @RequestParam(required = false) String areaRange,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "9") int size) {
        
        Double[] priceValues = {null, null};
        Double[] areaValues = {null, null};
        
        parseRangeParams(priceRange, areaRange, model, priceValues, areaValues);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Property> propertyPage = getProperties("Nhà phố", keyword, priceValues[0], priceValues[1], areaValues[0], areaValues[1], pageable);
        
        addPaginationAttributes(model, propertyPage, page, size);
        
        // Add keyword to the model
        model.addAttribute("keyword", keyword);
        
        return "townhouses";
    }

    @GetMapping("/offices")
    public String offices(Model model,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String priceRange,
                          @RequestParam(required = false) String areaRange,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "9") int size) {
        
        Double[] priceValues = {null, null};
        Double[] areaValues = {null, null};
        
        parseRangeParams(priceRange, areaRange, model, priceValues, areaValues);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Property> propertyPage = getProperties("Văn phòng", keyword, priceValues[0], priceValues[1], areaValues[0], areaValues[1], pageable);
        
        addPaginationAttributes(model, propertyPage, page, size);
        
        // Add keyword to the model
        model.addAttribute("keyword", keyword);
        
        return "offices";
    }

    @GetMapping("/land")
    public String land(Model model,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String priceRange,
                      @RequestParam(required = false) String areaRange,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "9") int size) {
        
        Double[] priceValues = {null, null};
        Double[] areaValues = {null, null};
        
        parseRangeParams(priceRange, areaRange, model, priceValues, areaValues);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Property> propertyPage = getProperties("Đất nền", keyword, priceValues[0], priceValues[1], areaValues[0], areaValues[1], pageable);
        
        addPaginationAttributes(model, propertyPage, page, size);
        
        // Add keyword to the model
        model.addAttribute("keyword", keyword);
        
        return "land";
    }
} 