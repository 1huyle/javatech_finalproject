package com.example.demo.service.impl;

import com.example.demo.dto.PropertyDTO;
import com.example.demo.dto.PropertySummaryDTO;
import com.example.demo.dto.PropertyUpdateRequestDTO;
import com.example.demo.model.Property;
import com.example.demo.model.PropertyImage;
import com.example.demo.model.PropertyType;
import com.example.demo.model.User;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.repository.PropertyRepository;
import com.example.demo.repository.PropertyTypeRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PropertyService;
import com.example.demo.service.SystemConfigService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PropertyServiceImpl implements PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemConfigService systemConfigService;

    @Value("${physical.upload-dir.base}")
    private String physicalUploadDirBase;

    private static final String PROPERTIES_SUB_DIR = "real-estate";

    @Override
    public Property createProperty(PropertyDTO propertyDTO, MultipartFile imageFile) {
        PropertyType propertyType = propertyTypeRepository.findById(propertyDTO.getPropertyTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Property type not found"));

        User owner = userRepository.findById(propertyDTO.getOwnerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        User realtor = null;
        if (propertyDTO.getRealtorId() != null) {
            realtor = userRepository.findById(propertyDTO.getRealtorId())
                    .orElseThrow(() -> new EntityNotFoundException("Realtor not found"));
        }

        Property property = new Property();
        property.setName(propertyDTO.getName());
        property.setDescription(propertyDTO.getDescription());
        property.setPrice(propertyDTO.getPrice());
        property.setArea(propertyDTO.getArea());
        property.setAddress(propertyDTO.getAddress());
        property.setPropertyType(propertyType);
        property.setOwner(owner);
        property.setRealtor(realtor);
        property.setStatus(PropertyStatus.AVAILABLE);
        
        if (property.getImages() == null) {
            property.setImages(new ArrayList<>());
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String uniqueFileName = generateUniqueFileName(imageFile.getOriginalFilename());
                Path uploadPathDir = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR);
                Files.createDirectories(uploadPathDir); 
                Path filePath = uploadPathDir.resolve(uniqueFileName);
                Files.copy(imageFile.getInputStream(), filePath);
                String imageUrl = "/uploads/" + PROPERTIES_SUB_DIR + "/" + uniqueFileName;
                
                property.setFileUrl(imageUrl); 

                PropertyImage propertyImage = new PropertyImage();
                propertyImage.setImageUrl(imageUrl);
                propertyImage.setPrimary(true); 
                propertyImage.setProperty(property);
                property.getImages().add(propertyImage);

            } catch (IOException e) {
                System.err.println("Failed to upload single property image during creation: " + imageFile.getOriginalFilename() + " - " + e.getMessage());
            }
        }

        // Thiết lập tỷ lệ hoa hồng mặc định nếu không được chỉ định
        if (property.getCommissionRate() == null) {
            property.setCommissionRate(systemConfigService.getDefaultCommissionRate());
        }

        return propertyRepository.save(property);
    }

    @Override
    @Transactional
    public Property updateProperty(Long id, PropertyUpdateRequestDTO dto, List<MultipartFile> newImages) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + id));

        // Cập nhật các trường cơ bản
        property.setName(dto.getName());
        property.setDescription(dto.getDescription());
        property.setPrice(dto.getPrice());
        property.setArea(dto.getArea());
        property.setAddress(dto.getAddress());
        property.setBedrooms(dto.getBedrooms());
        property.setBathrooms(dto.getBathrooms());
        property.setYearBuilt(dto.getYearBuilt());
        property.setFeatured(dto.getFeatured() != null ? dto.getFeatured() : property.getFeatured());
        property.setLatitude(dto.getLatitude());
        property.setLongitude(dto.getLongitude());
        property.setMetaTitle(dto.getMetaTitle());
        property.setMetaDescription(dto.getMetaDescription());
        property.setSocialImageUrl(dto.getSocialImageUrl());
        property.setLanguageCode(dto.getLanguageCode() != null ? dto.getLanguageCode() : property.getLanguageCode());
        property.setCommissionRate(dto.getCommissionRate() != null ? dto.getCommissionRate() : property.getCommissionRate());
        property.setRentalPeriod(dto.getRentalPeriod());

        if (dto.getStatus() != null) {
            property.setStatus(dto.getStatus());
        }
        if (dto.getTransactionType() != null) {
            property.setTransactionType(dto.getTransactionType());
        }

        PropertyType propertyType = propertyTypeRepository.findById(dto.getPropertyTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Property type not found with id: " + dto.getPropertyTypeId()));
        property.setPropertyType(propertyType);

        if (dto.getOwnerId() != null && (property.getOwner() == null || !dto.getOwnerId().equals(property.getOwner().getId()))) {
            User owner = userRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new EntityNotFoundException("Owner not found with id: " + dto.getOwnerId()));
            property.setOwner(owner);
        }
        
        if (property.getImages() == null) {
            property.setImages(new ArrayList<>());
        }

        // Xử lý ảnh: Xóa ảnh cũ không có trong existingImageIds
        List<PropertyImage> imagesToRemove = new ArrayList<>();
        if (dto.getExistingImageIds() != null) {
            List<Long> existingImageIdsFromDto = dto.getExistingImageIds();
            for (PropertyImage currentImage : new ArrayList<>(property.getImages())) { // Iterate over a copy
                if (!existingImageIdsFromDto.contains(currentImage.getId())) {
                    imagesToRemove.add(currentImage);
                }
            }
        } else { 
             if (newImages != null && !newImages.isEmpty() && newImages.stream().anyMatch(f -> f!=null && !f.isEmpty())) {
                imagesToRemove.addAll(new ArrayList<>(property.getImages())); 
             }
        }
        
        for (PropertyImage imageToRemove : imagesToRemove) {
            try {
                if (imageToRemove.getImageUrl() != null && !imageToRemove.getImageUrl().startsWith("http")) {
                    String fileName = imageToRemove.getImageUrl().substring(imageToRemove.getImageUrl().lastIndexOf("/") + 1);
                    Path filePath = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR, fileName);
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException e) {
                System.err.println("Failed to delete physical image file: " + imageToRemove.getImageUrl() + " - " + e.getMessage());
            }
            property.getImages().remove(imageToRemove); 
        }

        // Thêm ảnh mới
        if (newImages != null && !newImages.isEmpty()) {
            Path uploadPathDir = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR);
            try {
                Files.createDirectories(uploadPathDir);
            } catch (IOException e) {
                System.err.println("Could not create directory for property images: " + uploadPathDir.toString() + " - " + e.getMessage());
            }

            for (MultipartFile imageFileItem : newImages) { // Đổi tên biến để tránh nhầm lẫn
                if (imageFileItem != null && !imageFileItem.isEmpty()) {
                    try {
                        String uniqueFileName = generateUniqueFileName(imageFileItem.getOriginalFilename());
                        Path filePath = uploadPathDir.resolve(uniqueFileName);
                        Files.copy(imageFileItem.getInputStream(), filePath);
                        String imageUrl = "/uploads/" + PROPERTIES_SUB_DIR + "/" + uniqueFileName;

                        PropertyImage propertyImage = new PropertyImage();
                        propertyImage.setImageUrl(imageUrl);
                        propertyImage.setProperty(property); 
                        property.getImages().add(propertyImage);
                    } catch (IOException e) {
                        System.err.println("Failed to upload new property image: " + imageFileItem.getOriginalFilename() + " due to " + e.getMessage());
                    }
                }
            }
        }
        
        if (property.getImages() != null && !property.getImages().isEmpty()) {
            Optional<PropertyImage> currentPrimary = property.getImages().stream()
                .filter(img -> img.getPrimary() != null && img.getPrimary())
                .findFirst();
            
            if (!currentPrimary.isPresent()) { 
                property.getImages().get(0).setPrimary(true); 
                property.setFileUrl(property.getImages().get(0).getImageUrl());
            } else { 
                 property.setFileUrl(currentPrimary.get().getImageUrl());
            }
        } else { 
            property.setFileUrl(null);
        }

        // Nếu vẫn null (cả giá trị mới và cũ) thì sử dụng giá trị mặc định
        if (property.getCommissionRate() == null) {
            property.setCommissionRate(systemConfigService.getDefaultCommissionRate());
        }

        return propertyRepository.save(property);
    }

    @Override
    @Transactional
    public Property updateProperty(Long id, PropertyDTO propertyDTO, MultipartFile imageFile) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + id));

        PropertyType propertyType = propertyTypeRepository.findById(propertyDTO.getPropertyTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Property type not found with id: " + propertyDTO.getPropertyTypeId()));

        User owner = userRepository.findById(propertyDTO.getOwnerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found with id: " + propertyDTO.getOwnerId()));

        User realtor = null;
        if (propertyDTO.getRealtorId() != null) {
            realtor = userRepository.findById(propertyDTO.getRealtorId())
                    .orElseThrow(() -> new EntityNotFoundException("Realtor not found with id: " + propertyDTO.getRealtorId()));
        }

        property.setName(propertyDTO.getName());
        property.setDescription(propertyDTO.getDescription());
        property.setPrice(propertyDTO.getPrice());
        property.setArea(propertyDTO.getArea());
        property.setAddress(propertyDTO.getAddress());
        property.setBedrooms(propertyDTO.getBedrooms()); 
        property.setBathrooms(propertyDTO.getBathrooms());
        property.setPropertyType(propertyType);
        property.setOwner(owner);
        property.setRealtor(realtor);
        if (propertyDTO.getStatus() != null) {
             property.setStatus(propertyDTO.getStatus());
        }
        if (propertyDTO.getTransactionType() != null) { 
            property.setTransactionType(propertyDTO.getTransactionType());
        }
        property.setYearBuilt(propertyDTO.getYearBuilt());
        property.setFeatured(propertyDTO.getFeatured() != null ? propertyDTO.getFeatured() : property.getFeatured());
        property.setLatitude(propertyDTO.getLatitude());
        property.setLongitude(propertyDTO.getLongitude());
        property.setMetaTitle(propertyDTO.getMetaTitle());
        property.setMetaDescription(propertyDTO.getMetaDescription());
        
        if (property.getImages() == null) {
            property.setImages(new ArrayList<>());
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String uniqueFileName = generateUniqueFileName(imageFile.getOriginalFilename());
                Path uploadPathDir = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR);
                Files.createDirectories(uploadPathDir);
                Path filePath = uploadPathDir.resolve(uniqueFileName);
                Files.copy(imageFile.getInputStream(), filePath);
                String newImageUrl = "/uploads/" + PROPERTIES_SUB_DIR + "/" + uniqueFileName;

                 if (property.getFileUrl() != null && !property.getFileUrl().isEmpty() && !property.getFileUrl().equals(newImageUrl)) {
                    boolean fileUrlIsPartOfExistingImages = property.getImages().stream()
                                                                .anyMatch(img -> property.getFileUrl().equals(img.getImageUrl()) && (img.getPrimary() == null || !img.getPrimary()));
                    if (!fileUrlIsPartOfExistingImages) {
                        try {
                            String oldFileName = property.getFileUrl().substring(property.getFileUrl().lastIndexOf("/") + 1);
                            Path oldFilePath = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR, oldFileName);
                            Files.deleteIfExists(oldFilePath);
                        } catch (Exception e) {
                            System.err.println("Could not delete old fileUrl image: " + property.getFileUrl() + " - " + e.getMessage());
                        }
                    }
                }
                property.setFileUrl(newImageUrl);
                
                property.getImages().forEach(img -> img.setPrimary(false)); 

                Optional<PropertyImage> existingImageInList = property.getImages().stream()
                    .filter(img -> newImageUrl.equals(img.getImageUrl()))
                    .findFirst();

                if (existingImageInList.isPresent()) {
                    existingImageInList.get().setPrimary(true);
                } else {
                    PropertyImage newPrimaryImage = new PropertyImage();
                    newPrimaryImage.setImageUrl(newImageUrl);
                    newPrimaryImage.setPrimary(true);
                    newPrimaryImage.setProperty(property);
                    property.getImages().add(newPrimaryImage);
                }

            } catch (IOException e) {
                System.err.println("Failed to upload single property image during update (PropertyDTO): " + e.getMessage());
            }
        }

        // Nếu vẫn null (cả giá trị mới và cũ) thì sử dụng giá trị mặc định
        if (property.getCommissionRate() == null) {
            property.setCommissionRate(systemConfigService.getDefaultCommissionRate());
        }

        return propertyRepository.save(property);
    }

    @Override
    public void deleteProperty(Long id) {
        if (!propertyRepository.existsById(id)) {
            throw new EntityNotFoundException("Property not found");
        }
        propertyRepository.deleteById(id);
    }

    @Override
    public Optional<Property> getPropertyById(Long id) {
        return propertyRepository.findById(id);
    }

    @Override
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    @Override
    public Page<Property> getAllProperties(Pageable pageable) {
        return propertyRepository.findAll(pageable);
    }

    @Override
    public Page<Property> getAllAvailableProperties(Pageable pageable) {
        return propertyRepository.findByStatus(PropertyStatus.AVAILABLE, pageable);
    }

    @Override
    public List<Property> getPropertiesByType(PropertyType propertyType) {
        return propertyRepository.findByPropertyType(propertyType);
    }

    @Override
    public List<Property> getPropertiesByOwner(User owner) {
        return propertyRepository.findByOwner(owner);
    }

    @Override
    public List<Property> getPropertiesByRealtor(User realtor) {
        return propertyRepository.findByRealtor(realtor);
    }

    @Override
    public List<Property> getPropertiesByStatus(PropertyStatus status) {
        return propertyRepository.findByStatus(status);
    }

    @Override
    public List<Property> searchProperties(String keyword) {
        return propertyRepository.search(keyword);
    }

    @Override
    public List<Property> searchAvailableProperties(String keyword) {
        return propertyRepository.searchByStatus(keyword, PropertyStatus.AVAILABLE);
    }

    @Override
    public List<Property> getPropertiesByPriceRange(double minPrice, double maxPrice) {
        return propertyRepository.findByPriceRange(minPrice, maxPrice);
    }

    @Override
    public List<Property> getPropertiesByAreaRange(double minArea, double maxArea) {
        return propertyRepository.findByAreaRange(minArea, maxArea);
    }

    @Override
    public void updatePropertyStatus(Long id, PropertyStatus status) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        property.setStatus(status);
        propertyRepository.save(property);
    }

    @Override
    public String uploadPropertyImage(MultipartFile imageFile) {
        try {
            String uniqueFileName = generateUniqueFileName(imageFile.getOriginalFilename());
            Path uploadPathDir = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR);
            Files.createDirectories(uploadPathDir);
            Path filePath = uploadPathDir.resolve(uniqueFileName);
            Files.copy(imageFile.getInputStream(), filePath);
            return "/uploads/" + PROPERTIES_SUB_DIR + "/" + uniqueFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + imageFile.getOriginalFilename(), e);
        }
    }

    @Override
    public boolean isPropertyOwner(Long propertyId, Long userId) {
        return propertyRepository.findById(propertyId)
                .map(property -> property.getOwner().getId().equals(userId))
                .orElse(false);
    }

    @Override
    public boolean isPropertyRealtor(Long propertyId, Long userId) {
        return propertyRepository.findById(propertyId)
                .map(property -> property.getRealtor() != null && 
                     property.getRealtor().getId().equals(userId))
                .orElse(false);
    }

    @Override
    public void updatePropertyMetaInfo(Long id, String metaTitle, String metaDescription, String socialImageUrl) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        property.setMetaTitle(metaTitle);
        property.setMetaDescription(metaDescription);
        property.setSocialImageUrl(socialImageUrl);
        propertyRepository.save(property);
    }

    @Override
    public void updatePropertyLanguage(Long id, String languageCode) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        property.setLanguageCode(languageCode);
        propertyRepository.save(property);
    }

    @Override
    public void incrementViewCount(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        property.setViewCount(property.getViewCount() + 1);
        propertyRepository.save(property);
    }

    @Override
    public void toggleFeatured(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        property.setFeatured(!property.getFeatured());
        propertyRepository.save(property);
    }

    @Override
    public void updatePropertyLocation(Long id, Double latitude, Double longitude) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        property.setLatitude(latitude);
        property.setLongitude(longitude);
        propertyRepository.save(property);
    }

    @Override
    @Transactional
    public void addPropertyImage(Long propertyId, MultipartFile imageFile, boolean isPrimary) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + propertyId));
        
        if (property.getImages() == null) {
            property.setImages(new ArrayList<>());
        }

        try {
            String uniqueFileName = generateUniqueFileName(imageFile.getOriginalFilename());
            Path uploadPathDir = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR);
            Files.createDirectories(uploadPathDir);
            Path filePath = uploadPathDir.resolve(uniqueFileName);
            Files.copy(imageFile.getInputStream(), filePath);
            String imageUrl = "/uploads/" + PROPERTIES_SUB_DIR + "/" + uniqueFileName;

        PropertyImage propertyImage = new PropertyImage();
            propertyImage.setImageUrl(imageUrl);
        propertyImage.setProperty(property);
        
        if (isPrimary) {
            property.getImages().forEach(img -> img.setPrimary(false));
                propertyImage.setPrimary(true);
                property.setFileUrl(imageUrl); 
            } else {
                 if (property.getImages().stream().noneMatch(img -> img.getPrimary() != null && img.getPrimary())) {
                    propertyImage.setPrimary(true);
                    property.setFileUrl(imageUrl);
                } else {
                    propertyImage.setPrimary(false);
                }
            }
        property.getImages().add(propertyImage);
        propertyRepository.save(property);
        } catch (IOException e) {
            System.err.println("Error adding property image: " + e.getMessage());
            throw new RuntimeException("Could not store image file for property " + propertyId, e);
        }
    }

    @Override
    @Transactional
    public void removePropertyImage(Long propertyId, Long imageId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + propertyId));
        
        if (property.getImages() == null || property.getImages().isEmpty()) return;

        Optional<PropertyImage> imageToRemoveOpt = property.getImages().stream()
            .filter(img -> img.getId().equals(imageId))
            .findFirst();

        if (imageToRemoveOpt.isPresent()) {
            PropertyImage imageToRemove = imageToRemoveOpt.get();
            boolean wasPrimary = imageToRemove.getPrimary() != null && imageToRemove.getPrimary();
            String removedImageUrl = imageToRemove.getImageUrl();
            
            property.getImages().remove(imageToRemove); 
            
            try {
                if (removedImageUrl != null && !removedImageUrl.startsWith("http")) {
                    String fileName = removedImageUrl.substring(removedImageUrl.lastIndexOf("/") + 1);
                    Path filePath = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR, fileName);
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException e) {
                System.err.println("Failed to delete physical image file during removePropertyImage: " + removedImageUrl + " - " + e.getMessage());
            }

            if (wasPrimary) { 
                Optional<PropertyImage> newPrimary = property.getImages().stream().findFirst();
                if (newPrimary.isPresent()) {
                    newPrimary.get().setPrimary(true);
                    property.setFileUrl(newPrimary.get().getImageUrl());
                } else {
                    property.setFileUrl(null); 
                }
            }
        propertyRepository.save(property);
        }
    }

    @Override
    public void updateImageOrder(Long propertyId, Long imageId, Integer newOrder) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        
        if (property.getImages() == null) return;
        
        property.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .ifPresent(img -> img.setOrder(newOrder));
        propertyRepository.save(property);
    }

    @Override
    @Transactional
    public void setPrimaryImage(Long propertyId, Long imageId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + propertyId));
        
        if (property.getImages() == null || property.getImages().isEmpty()) return;

        String newPrimaryUrl = null;
        boolean primarySet = false;
        for (PropertyImage img : property.getImages()) {
            boolean isTargetImage = img.getId().equals(imageId);
            img.setPrimary(isTargetImage);
            if (isTargetImage) {
                newPrimaryUrl = img.getImageUrl();
                primarySet = true;
            }
        }
        
        if (!primarySet && !property.getImages().isEmpty()) { 
             property.getImages().get(0).setPrimary(true);
             newPrimaryUrl = property.getImages().get(0).getImageUrl();
        }

        if (newPrimaryUrl != null) {
            property.setFileUrl(newPrimaryUrl);
        } else {
             property.setFileUrl(null); 
        }
        propertyRepository.save(property);
    }

    @Override
    public List<Property> getFeaturedProperties() {
        return propertyRepository.findByFeaturedTrueAndStatus(PropertyStatus.AVAILABLE, PageRequest.of(0,10)).getContent();
    }

    @Override
    public List<Property> getPropertiesByLocation(Double latitude, Double longitude, Double radius) {
        System.err.println("getPropertiesByLocation not fully implemented in service/repository yet. Using basic findAll as fallback.");
        return propertyRepository.findAll(PageRequest.of(0,10)).getContent(); 

    }

    @Override
    public List<Property> getPropertiesByLanguage(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) return Collections.emptyList();
        return propertyRepository.findByLanguageCodeAndStatus(languageCode.trim(), PropertyStatus.AVAILABLE);
    }

    @Override
    public List<Property> getSimilarProperties(Property property, int limit) {
        if (property == null || property.getPropertyType() == null || property.getId() == null) {
            return Collections.emptyList();
        }
        Pageable pageable = PageRequest.of(0, limit);
        return propertyRepository.findByPropertyTypeAndStatusAndIdNot(property.getPropertyType(), PropertyStatus.AVAILABLE, property.getId(), pageable);
    }

    @Override
    public List<Property> getRecentProperties(int limit) {
        Page<Property> page = propertyRepository.findByStatusOrderByCreatedAtDesc(PropertyStatus.AVAILABLE, PageRequest.of(0, limit));
        return page.getContent();
    }

    @Override
    public List<Property> getPropertiesByTypeName(String typeName, int limit) {
        if (typeName == null || typeName.trim().isEmpty()) return Collections.emptyList();
        PropertyType propertyType = propertyTypeRepository.findByName(typeName.trim())
                .orElse(null); 
        if (propertyType == null) return Collections.emptyList();
        
        Page<Property> page = propertyRepository.findByPropertyTypeAndStatus(propertyType, PropertyStatus.AVAILABLE, PageRequest.of(0, limit));
        return page.getContent();
    }

    @Override
    public Page<Property> getPropertiesByTypeNamePaginated(String typeName, Pageable pageable) {
        if (typeName == null || typeName.trim().isEmpty()) return Page.empty(pageable);
        return propertyRepository.findByPropertyTypeNameAndStatus(typeName.trim(), PropertyStatus.AVAILABLE, pageable);
    }

    @Override
    public Page<Property> advancedSearch(String keyword, String typeName, Double minPrice, Double maxPrice, Double minArea, Double maxArea, PropertyStatus status, TransactionType transactionType, Pageable pageable) {
        System.out.println(String.format(
            "Advanced Search Params (Impl): keyword='%s', typeName='%s', minPrice=%s, maxPrice=%s, minArea=%s, maxArea=%s, status=%s, transactionType=%s, page=%s, size=%s",
            keyword, typeName, minPrice, maxPrice, minArea, maxArea, status, transactionType, pageable.getPageNumber(), pageable.getPageSize()
        ));
        return propertyRepository.advancedSearch(keyword, typeName, minPrice, maxPrice, minArea, maxArea, status, transactionType, pageable);
    }

    @Override
    public List<PropertySummaryDTO> getRecommendedProperties(Long userId, int limit) {
        List<Property> properties = propertyRepository.findByFeaturedTrueAndStatus(PropertyStatus.AVAILABLE, PageRequest.of(0, limit)).getContent();
        
        if (properties.size() < limit) {
            int remainingLimit = limit - properties.size();
            List<Long> existingIds = properties.stream().map(Property::getId).collect(Collectors.toList());
            
            Page<Property> recentPropertiesPage;
            if (existingIds.isEmpty()) {
                recentPropertiesPage = propertyRepository.findByStatusOrderByCreatedAtDesc(
                    PropertyStatus.AVAILABLE, 
                    PageRequest.of(0, remainingLimit)
                );
            } else {
                 recentPropertiesPage = propertyRepository.findByIdNotInAndStatusOrderByCreatedAtDesc(
                    existingIds,
                    PropertyStatus.AVAILABLE, 
                    PageRequest.of(0, remainingLimit)
                );
            }
            properties.addAll(recentPropertiesPage.getContent());
        }
        
        if (properties.size() > limit) {
            properties = properties.subList(0, limit);
        }

        return properties.stream()
                .map(p -> new PropertySummaryDTO(
                        p.getId(),
                        p.getName(),
                        p.getAddress(),
                        p.getFileUrl() != null ? p.getFileUrl() : (p.getImages() != null && !p.getImages().isEmpty() && p.getImages().get(0).getImageUrl() != null ? p.getImages().get(0).getImageUrl() : "/assets/images/property-placeholder.jpg"),
                        p.getPropertyType() != null ? p.getPropertyType().getName() : "N/A",
                        p.getPrice(),
                        p.getStatus()
                ))
            .collect(Collectors.toList());
    }
    
    @Override
    public void softDeleteProperty(Long id, Long realtorId) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with id: " + id));

        if (property.getRealtor() == null || !property.getRealtor().getId().equals(realtorId)) {
            throw new SecurityException("Realtor with id " + realtorId + " is not authorized to delete property with id " + id);
        }
        property.setStatus(PropertyStatus.INACTIVE);
        propertyRepository.save(property);
    }

    @Override
    @Transactional
    public Property createProperty(Property property, List<MultipartFile> imageFiles) {
        // Ensure the incoming property object is not null
        if (property == null) {
            throw new IllegalArgumentException("Property cannot be null");
        }

        // Validate or set realtor if not already set (assuming it's set by controller)
        // User realtor = userRepository.findById(property.getRealtor().getId())
        // .orElseThrow(() -> new EntityNotFoundException("Realtor not found"));
        // property.setRealtor(realtor);

        // PropertyType propertyType = propertyTypeRepository.findById(property.getPropertyType().getId())
        // .orElseThrow(() -> new EntityNotFoundException("Property type not found"));
        // property.setPropertyType(propertyType);

        // Ensure images list is initialized
        if (property.getImages() == null) {
            property.setImages(new ArrayList<>());
        }

        if (imageFiles != null && !imageFiles.isEmpty()) {
            Path uploadPathDir = Paths.get(physicalUploadDirBase, PROPERTIES_SUB_DIR);
            try {
                Files.createDirectories(uploadPathDir);
            } catch (IOException e) {
                System.err.println("Could not create directory for property images: " + uploadPathDir.toString() + " - " + e.getMessage());
                // Consider re-throwing or handling more gracefully
            }

            boolean primaryImageSet = false;
            for (MultipartFile imageFile : imageFiles) {
                if (imageFile != null && !imageFile.isEmpty()) {
                    try {
                        String uniqueFileName = generateUniqueFileName(imageFile.getOriginalFilename());
                        Path filePath = uploadPathDir.resolve(uniqueFileName);
                        Files.copy(imageFile.getInputStream(), filePath);
                        String imageUrl = "/uploads/" + PROPERTIES_SUB_DIR + "/" + uniqueFileName;

                        PropertyImage propertyImage = new PropertyImage();
                        propertyImage.setImageUrl(imageUrl);
                        propertyImage.setProperty(property);

                        if (!primaryImageSet) {
                            propertyImage.setPrimary(true);
                            property.setFileUrl(imageUrl); // Set main image URL for the property
                            primaryImageSet = true;
                        } else {
                            propertyImage.setPrimary(false);
                        }
                        property.getImages().add(propertyImage);
                    } catch (IOException e) {
                        System.err.println("Failed to upload property image: " + imageFile.getOriginalFilename() + " - " + e.getMessage());
                        // Optionally, collect errors and decide if the whole transaction should fail
                    }
                }
            }
        }
        
        // If no images were uploaded but property.fileUrl was somehow set, clear it or ensure consistency.
        // If images were uploaded but none got set as primary (e.g. all failed), handle accordingly.
        if (property.getImages().isEmpty()) {
            property.setFileUrl(null);
        } else if (property.getImages().stream().noneMatch(img -> img.getPrimary() != null && img.getPrimary()) && !property.getImages().isEmpty()) {
            // If no primary image was set (e.g. first image failed to upload, but subsequent ones succeeded)
            // set the first available image as primary.
            PropertyImage firstImage = property.getImages().get(0);
            firstImage.setPrimary(true);
            property.setFileUrl(firstImage.getImageUrl());
        }


        // Check for duplicate property by the same realtor (name and address)
        // This assumes realtor is already set on the property object
        if (property.getRealtor() != null && property.getRealtor().getId() != null) {
            Optional<Property> existingProperty = propertyRepository.findByNameAndAddressAndRealtorId(
                property.getName(), 
                property.getAddress(), 
                property.getRealtor().getId()
            );
            if (existingProperty.isPresent()) {
                // Handle duplicate property for the same realtor
                // For example, throw an exception or return an error indicator
                // This is a simple example, you might want a more specific exception
                throw new IllegalStateException("Property with the same name and address already exists for this realtor.");
            }
        }


        return propertyRepository.save(property);
    }

    @Override
    public long countAllProperties() {
        return propertyRepository.count();
    }

    @Override
    public long countAvailableProperties() {
        return propertyRepository.countByStatus(PropertyStatus.AVAILABLE);
    }

    @Override
    public List<PropertyType> getAllPropertyTypes() {
        return propertyTypeRepository.findAll();
    }

    @Override
    public java.util.Map<String, Long> getPropertyTypeDistributionForChart() {
        List<java.util.Map<String, Object>> results = propertyRepository.countPropertiesByTypeName();
        java.util.Map<String, Long> distribution = new java.util.LinkedHashMap<>();
        for (java.util.Map<String, Object> result : results) {
            String typeName = (String) result.get("typeName");
            Long count = (Long) result.get("propertyCount"); // COUNT trả về Long
            if (typeName != null && count != null) {
                distribution.put(typeName, count);
            }
        }
        return distribution;
    }

    @Override
    public Property saveProperty(Property property) {
        return propertyRepository.save(property);
    }

    @Override
    public Page<Property> getFilteredProperties(String search, String type, String status, Double minPrice, Double maxPrice, Pageable pageable) {
        // Create a specification for dynamic filtering
        Specification<Property> spec = Specification.where(null);
        
        // Add search filter (name, description, address)
        if (search != null && !search.trim().isEmpty()) {
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("name")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern),
                    cb.like(cb.lower(root.get("address")), searchPattern)
                )
            );
        }
        
        // Add property type filter
        if (type != null && !type.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(cb.lower(root.get("propertyType").get("name")), type.trim().toLowerCase())
            );
        }
        
        // Add status filter
        if (status != null && !status.trim().isEmpty()) {
            PropertyStatus propertyStatus = null;
            
            // Map from string parameter to PropertyStatus enum
            switch (status.trim().toLowerCase()) {
                case "available":
                    propertyStatus = PropertyStatus.AVAILABLE;
                    break;
                case "sold":
                    propertyStatus = PropertyStatus.SOLD;
                    break;
                case "rented":
                    propertyStatus = PropertyStatus.RENTED;
                    break;
                case "inactive":
                    propertyStatus = PropertyStatus.INACTIVE;
                    break;
                default:
                    // No status filter if invalid status
                    break;
            }
            
            if (propertyStatus != null) {
                PropertyStatus finalPropertyStatus = propertyStatus;
                spec = spec.and((root, query, cb) -> 
                    cb.equal(root.get("status"), finalPropertyStatus)
                );
            }
        }
        
        // Add price range filter
        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("price"), minPrice)
            );
        }
        
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("price"), maxPrice)
            );
        }
        
        // Execute the query with all filters
        return propertyRepository.findAll(spec, pageable);
    }

    /**
     * Cập nhật tỷ lệ hoa hồng cho tất cả các bất động sản có commissionRate = null
     * @return Số lượng bất động sản đã được cập nhật
     */
    @Override
    public int updateNullCommissionRates() {
        Double defaultRate = systemConfigService.getDefaultCommissionRate();
        List<Property> propertiesWithNullCommission = propertyRepository.findByCommissionRateIsNull();
        
        if (propertiesWithNullCommission.isEmpty()) {
            return 0;
        }
        
        for (Property property : propertiesWithNullCommission) {
            property.setCommissionRate(defaultRate);
        }
        
        propertyRepository.saveAll(propertiesWithNullCommission);
        return propertiesWithNullCommission.size();
    }
    
    /**
     * Cập nhật tỷ lệ hoa hồng cho tất cả các bất động sản
     * @param newRate Tỷ lệ hoa hồng mới
     * @return Số lượng bất động sản đã được cập nhật
     */
    @Override
    public int updateAllCommissionRates(Double newRate) {
        if (newRate == null) {
            return 0;
        }
        
        List<Property> allProperties = propertyRepository.findAll();
        for (Property property : allProperties) {
            property.setCommissionRate(newRate);
        }
        
        propertyRepository.saveAll(allProperties);
        return allProperties.size();
    }

    private String generateUniqueFileName(String originalFileName) {
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + fileExtension;
    }
} 