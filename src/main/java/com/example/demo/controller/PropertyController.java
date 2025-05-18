package com.example.demo.controller;

import com.example.demo.dto.PropertyDTO;
import com.example.demo.dto.PropertySummaryDTO;
import com.example.demo.model.Property;
import com.example.demo.model.PropertyType;
import com.example.demo.model.User;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.service.PropertyService;
import com.example.demo.service.PropertyTypeService;
import com.example.demo.service.UserService;
import com.example.demo.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.model.CustomUserDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import com.example.demo.repository.PropertyTypeRepository;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import java.util.stream.Collectors;
import com.example.demo.dto.PropertyUpdateRequestDTO;
import org.springframework.dao.DataIntegrityViolationException;
import com.example.demo.model.PropertyImage;
import com.example.demo.model.enums.UserRole;
import java.util.Optional;
import com.example.demo.dto.PropertyImageDTO;
import java.util.Map;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyTypeService propertyTypeService;

    @Autowired
    private UserService userService;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Property> createProperty(
            @Valid @ModelAttribute PropertyDTO propertyDTO,
            @RequestParam(required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(propertyService.createProperty(propertyDTO, imageFile));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Property> updateProperty(
            @PathVariable Long id,
            @Valid @ModelAttribute PropertyDTO propertyDTO,
            @RequestParam(required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(propertyService.updateProperty(id, propertyDTO, imageFile));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<Property>> getAllProperties(Pageable pageable) {
        return ResponseEntity.ok(propertyService.getAllAvailableProperties(pageable));
    }

    @GetMapping("/type/{typeId}")
    public ResponseEntity<List<Property>> getPropertiesByType(@PathVariable Long typeId) {
        PropertyType propertyType = propertyTypeService.getPropertyTypeById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Property type not found"));
        return ResponseEntity.ok(propertyService.getPropertiesByType(propertyType));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasRole('ADMIN') or #ownerId == authentication.principal.id")
    public ResponseEntity<List<Property>> getPropertiesByOwner(@PathVariable Long ownerId) {
        User owner = userService.getUserById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        return ResponseEntity.ok(propertyService.getPropertiesByOwner(owner));
    }

    @GetMapping("/realtor/{realtorId}")
    @PreAuthorize("hasRole('ADMIN') or #realtorId == authentication.principal.id")
    public ResponseEntity<List<PropertySummaryDTO>> getPropertiesByRealtor(@PathVariable Long realtorId) {
        User realtor = userService.getUserById(realtorId)
                .orElseThrow(() -> new IllegalArgumentException("Realtor not found"));
        List<Property> properties = propertyService.getPropertiesByRealtor(realtor);
        
        List<PropertySummaryDTO> propertySummaries = properties.stream()
            .map(property -> {
                String imageUrl = property.getImages() != null && !property.getImages().isEmpty() 
                                ? property.getImages().get(0).getImageUrl()
                                : "/assets/images/property-placeholder.jpg";

                String propertyTypeName = property.getPropertyType() != null 
                                        ? property.getPropertyType().getName() 
                                        : "N/A";
                
                return new PropertySummaryDTO(
                    property.getId(),
                    property.getName(),
                    property.getAddress(),
                    imageUrl,
                    propertyTypeName,
                    property.getPrice(),
                    property.getStatus()
                );
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(propertySummaries);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Property>> getPropertiesByStatus(@PathVariable PropertyStatus status) {
        return ResponseEntity.ok(propertyService.getPropertiesByStatus(status));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Property>> searchProperties(@RequestParam String keyword) {
        return ResponseEntity.ok(propertyService.searchAvailableProperties(keyword));
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<Property>> getPropertiesByPriceRange(
            @RequestParam double minPrice,
            @RequestParam double maxPrice) {
        return ResponseEntity.ok(propertyService.getPropertiesByPriceRange(minPrice, maxPrice));
    }

    @GetMapping("/area-range")
    public ResponseEntity<List<Property>> getPropertiesByAreaRange(
            @RequestParam double minArea,
            @RequestParam double maxArea) {
        return ResponseEntity.ok(propertyService.getPropertiesByAreaRange(minArea, maxArea));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> updatePropertyStatus(
            @PathVariable Long id,
            @RequestParam PropertyStatus status) {
        propertyService.updatePropertyStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // SEO and social sharing endpoints
    @PutMapping("/{id}/meta")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> updatePropertyMetaInfo(
            @PathVariable Long id,
            @RequestParam String metaTitle,
            @RequestParam String metaDescription,
            @RequestParam(required = false) String socialImageUrl) {
        propertyService.updatePropertyMetaInfo(id, metaTitle, metaDescription, socialImageUrl);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/language")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> updatePropertyLanguage(
            @PathVariable Long id,
            @RequestParam String languageCode) {
        propertyService.updatePropertyLanguage(id, languageCode);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
        propertyService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleFeatured(@PathVariable Long id) {
        propertyService.toggleFeatured(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> updatePropertyLocation(
            @PathVariable Long id,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        propertyService.updatePropertyLocation(id, latitude, longitude);
        return ResponseEntity.ok().build();
    }

    // Property images endpoints
    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> addPropertyImage(
            @PathVariable Long id,
            @RequestParam MultipartFile imageFile,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        propertyService.addPropertyImage(id, imageFile, isPrimary);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{propertyId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> removePropertyImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {
        propertyService.removePropertyImage(propertyId, imageId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{propertyId}/images/{imageId}/order")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> updateImageOrder(
            @PathVariable Long propertyId,
            @PathVariable Long imageId,
            @RequestParam Integer newOrder) {
        propertyService.updateImageOrder(propertyId, imageId, newOrder);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{propertyId}/images/{imageId}/primary")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {
        propertyService.setPrimaryImage(propertyId, imageId);
        return ResponseEntity.ok().build();
    }

    // New search endpoints
    @GetMapping("/featured")
    public ResponseEntity<List<Property>> getFeaturedProperties() {
        return ResponseEntity.ok(propertyService.getFeaturedProperties());
    }

    @GetMapping("/location")
    public ResponseEntity<List<Property>> getPropertiesByLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radius) {
        return ResponseEntity.ok(propertyService.getPropertiesByLocation(latitude, longitude, radius));
    }

    @GetMapping("/language/{languageCode}")
    public ResponseEntity<List<Property>> getPropertiesByLanguage(
            @PathVariable String languageCode) {
        return ResponseEntity.ok(propertyService.getPropertiesByLanguage(languageCode));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<Property>> getSimilarProperties(
            @PathVariable Long id,
            @RequestParam(defaultValue = "3") int limit) {
        Property property = propertyService.getPropertyById(id).orElse(null);
        if (property == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(propertyService.getSimilarProperties(property, limit));
    }

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<PropertySummaryDTO>> getRecommendedProperties(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        // userId is passed for future personalization, not used in current basic implementation
        List<PropertySummaryDTO> recommendations = propertyService.getRecommendedProperties(userId, limit);
        if (recommendations.isEmpty()) {
            return ResponseEntity.noContent().build(); // Or ok with empty list
        }
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<?> addProperty(
            @RequestParam("name") String name,
            @RequestParam("propertyTypeId") Long propertyTypeId,
            @RequestParam("price") double price,
            @RequestParam("area") double area,
            @RequestParam("bedrooms") int bedrooms,
            @RequestParam("bathrooms") int bathrooms,
            @RequestParam("address") String address,
            @RequestParam("description") String description,
            @RequestParam("status") String statusString,
            @RequestParam("transactionType") String transactionTypeString,
            @RequestParam(value = "yearBuilt", required = false) Integer yearBuilt,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetail currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Realtor not authenticated.");
        }

        try {
            PropertyType propertyType = propertyTypeRepository.findById(propertyTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("PropertyType not found with id: " + propertyTypeId));
            
            User realtor = userService.getUserById(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Realtor not found with id: " + currentUser.getId()));

            Property property = new Property();
            property.setName(name);
            property.setPropertyType(propertyType);
            property.setPrice(price);
            property.setArea(area);
            property.setBedrooms(bedrooms);
            property.setBathrooms(bathrooms);
            property.setAddress(address);
            property.setDescription(description);
            property.setRealtor(realtor);
            property.setOwner(realtor);
            if (yearBuilt != null) {
                property.setYearBuilt(yearBuilt);
            }

            try {
                property.setStatus(PropertyStatus.valueOf(statusString.toUpperCase()));
                property.setTransactionType(TransactionType.valueOf(transactionTypeString.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid status or transactionType value: " + e.getMessage());
            }
            
            property.setFeatured(false);

            Property savedProperty = propertyService.createProperty(property, images);
            
            return ResponseEntity.ok(savedProperty);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error adding property: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding property: " + e.getMessage());
        }
    }

    // PUT /api/properties/update/{id} - Endpoint to update property information
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('REALTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProperty(
            @PathVariable Long id,
            @RequestPart(name = "propertyData") @Valid PropertyUpdateRequestDTO propertyUpdateDTO,
            @RequestPart(name = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }

        Property existingProperty = propertyService.getPropertyById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        if (currentUser.getRole() == UserRole.REALTOR &&
            (existingProperty.getRealtor() == null || !existingProperty.getRealtor().getId().equals(currentUser.getId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to update this property.");
        }

        try {
            Property updatedProperty = propertyService.updateProperty(id, propertyUpdateDTO, images);
            return ResponseEntity.ok(convertToDto(updatedProperty));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Data integrity violation: " + e.getMostSpecificCause().getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating the property: " + e.getMessage());
        }
    }

    private PropertyDTO convertToDto(Property property) {
        PropertyDTO dto = new PropertyDTO();
        dto.setId(property.getId());
        dto.setName(property.getName());
        dto.setDescription(property.getDescription());
        dto.setPrice(property.getPrice());
        dto.setArea(property.getArea());
        dto.setAddress(property.getAddress());
        if (property.getPropertyType() != null) {
            dto.setPropertyTypeId(property.getPropertyType().getId());
            dto.setPropertyTypeName(property.getPropertyType().getName());
        }
        if (property.getOwner() != null) {
            dto.setOwnerId(property.getOwner().getId());
        }
        if (property.getRealtor() != null) {
            dto.setRealtorId(property.getRealtor().getId());
        }
        dto.setStatus(property.getStatus());
        dto.setTransactionType(property.getTransactionType());
        dto.setFileUrl(property.getFileUrl());
        if (property.getImages() != null) {
            List<PropertyImageDTO> imageDTOs = property.getImages().stream()
                    .map(img -> new PropertyImageDTO(img.getId(), img.getImageUrl(), img.getPrimary() != null && img.getPrimary()))
                    .collect(Collectors.toList());
            dto.setImages(imageDTOs);
        }
        dto.setBedrooms(property.getBedrooms());
        dto.setBathrooms(property.getBathrooms());
        dto.setYearBuilt(property.getYearBuilt());
        dto.setFeatured(property.getFeatured());
        dto.setLatitude(property.getLatitude());
        dto.setLongitude(property.getLongitude());
        dto.setViewCount(property.getViewCount());
        dto.setMetaTitle(property.getMetaTitle());
        dto.setMetaDescription(property.getMetaDescription());
        return dto;
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<?> softDeleteProperty(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetail currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }

        try {
            // Kiểm tra xem property có tồn tại không
            Property property = propertyService.getPropertyById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
            
            // Kiểm tra xem realtor có quyền với property này không
            if (property.getRealtor() == null || !property.getRealtor().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You don't have permission to delete this property");
            }
            
            // Đánh dấu property là đã xóa bởi realtor
            property.setStatus(PropertyStatus.INACTIVE);
            property.setRealtorDeleted(true);
            propertyService.saveProperty(property);
            
            return ResponseEntity.ok().body("Property with id " + id + " has been marked as deleted.");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) { // Bắt lỗi nếu realtor không có quyền
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) { // Bắt lỗi nếu có pending requests (nếu đã implement)
             return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            // Log lỗi e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while deleting the property: " + e.getMessage());
        }
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<?> getPropertyById(@PathVariable Long id) {
        Optional<Property> propertyOptional = propertyService.getPropertyById(id);
        if (propertyOptional.isPresent()) {
            Property property = propertyOptional.get();
            // Increment view count or perform other actions if needed
            // propertyService.incrementViewCount(id); // Ví dụ
            return ResponseEntity.ok(convertToDto(property)); 
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Property not found with id: " + id);
        }
    }

    // New endpoint to deactivate property and notify realtor
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateProperty(@PathVariable Long id) {
        try {
            Property property = propertyService.getPropertyById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found with id " + id));

            // Kiểm tra nếu property đã bị realtor xóa, không cho phép admin thao tác
            if (property.getRealtorDeleted() != null && property.getRealtorDeleted()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false, 
                                "message", "Không thể vô hiệu hóa bất động sản đã bị môi giới xóa"
                        ));
            }
            
            // Kiểm tra nếu property không ở trạng thái AVAILABLE
            if (property.getStatus() != PropertyStatus.AVAILABLE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false, 
                                "message", "Chỉ bất động sản đang ở trạng thái AVAILABLE mới có thể vô hiệu hóa"
                        ));
            }

            // Set property status to INACTIVE and mark as admin deactivated
            property.setStatus(PropertyStatus.INACTIVE);
            property.setAdminDeactivated(true);
            propertyService.saveProperty(property);
            
            // Send email notification to realtor if assigned
            if (property.getRealtor() != null) {
                // Send email notification using the EmailService
                emailService.sendPropertyDeactivationNotification(property, property.getRealtor());
                System.out.println("Email notification sent to realtor: " + property.getRealtor().getEmail() + 
                        " about property " + property.getName() + " being deactivated by admin");
            }
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "success", true, 
                            "message", "Property deactivated successfully and realtor notified"
                    ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate property: " + e.getMessage()));
        }
    }

    // New endpoint to reactivate property and notify realtor
    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reactivateProperty(@PathVariable Long id) {
        try {
            Property property = propertyService.getPropertyById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found with id " + id));

            // Kiểm tra nếu property đã bị realtor xóa, không cho phép admin thao tác
            if (property.getRealtorDeleted() != null && property.getRealtorDeleted()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false, 
                                "message", "Không thể kích hoạt lại bất động sản đã bị môi giới xóa"
                        ));
            }
            
            // Kiểm tra nếu property không ở trạng thái INACTIVE
            if (property.getStatus() != PropertyStatus.INACTIVE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false, 
                                "message", "Chỉ bất động sản đang ở trạng thái INACTIVE mới có thể kích hoạt lại"
                        ));
            }

            // Set property status to AVAILABLE and remove admin deactivation mark
            property.setStatus(PropertyStatus.AVAILABLE);
            property.setAdminDeactivated(false);
            property.setRealtorDeleted(false); // Xóa cả đánh dấu xóa từ realtor nếu có
            propertyService.saveProperty(property);
            
            // Send email notification to realtor if assigned
            if (property.getRealtor() != null) {
                emailService.sendPropertyReactivationNotification(property, property.getRealtor());
                System.out.println("Email notification sent to realtor: " + property.getRealtor().getEmail() + 
                        " about property " + property.getName() + " being reactivated by admin");
            }
            
            return ResponseEntity.ok()
                    .body(Map.of(
                            "success", true, 
                            "message", "Property reactivated successfully and realtor notified"
                    ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reactivate property: " + e.getMessage()));
        }
    }
}
