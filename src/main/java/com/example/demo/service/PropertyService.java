package com.example.demo.service;

import com.example.demo.dto.PropertyDTO;
import com.example.demo.dto.PropertySummaryDTO;
import com.example.demo.dto.PropertyUpdateRequestDTO;
import com.example.demo.model.Property;
import com.example.demo.model.PropertyType;
import com.example.demo.model.User;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface PropertyService {
    Property createProperty(PropertyDTO propertyDTO, MultipartFile imageFile);
    Property createProperty(Property property, List<MultipartFile> images);
    Property updateProperty(Long id, PropertyUpdateRequestDTO propertyUpdateRequestDTO, List<MultipartFile> newImages);
    Property updateProperty(Long id, PropertyDTO propertyDTO, MultipartFile imageFile);
    void deleteProperty(Long id);
    void softDeleteProperty(Long id, Long realtorId);
    Optional<Property> getPropertyById(Long id);
    List<Property> getAllProperties();
    Page<Property> getAllProperties(Pageable pageable);
    List<Property> getPropertiesByType(PropertyType propertyType);
    List<Property> getPropertiesByOwner(User owner);
    List<Property> getPropertiesByRealtor(User realtor);
    List<Property> getPropertiesByStatus(PropertyStatus status);
    List<Property> searchProperties(String keyword);
    List<Property> getPropertiesByPriceRange(double minPrice, double maxPrice);
    List<Property> getPropertiesByAreaRange(double minArea, double maxArea);
    void updatePropertyStatus(Long id, PropertyStatus status);
    String uploadPropertyImage(MultipartFile imageFile);
    boolean isPropertyOwner(Long propertyId, Long userId);
    boolean isPropertyRealtor(Long propertyId, Long userId);

    // New methods for SEO and social sharing
    void updatePropertyMetaInfo(Long id, String metaTitle, String metaDescription, String socialImageUrl);
    void updatePropertyLanguage(Long id, String languageCode);
    void incrementViewCount(Long id);
    void toggleFeatured(Long id);
    void updatePropertyLocation(Long id, Double latitude, Double longitude);
    
    // New methods for property images
    void addPropertyImage(Long propertyId, MultipartFile imageFile, boolean isPrimary);
    void removePropertyImage(Long propertyId, Long imageId);
    void updateImageOrder(Long propertyId, Long imageId, Integer newOrder);
    void setPrimaryImage(Long propertyId, Long imageId);
    
    // New search methods
    List<Property> getFeaturedProperties();
    List<Property> getPropertiesByLocation(Double latitude, Double longitude, Double radius);
    List<Property> getPropertiesByLanguage(String languageCode);
    List<Property> getSimilarProperties(Property property, int limit);
    
    // Additional methods for the homepage
    List<Property> getRecentProperties(int limit);
    List<Property> getPropertiesByTypeName(String typeName, int limit);
    
    // New paginated method for property type pages
    Page<Property> getPropertiesByTypeNamePaginated(String typeName, Pageable pageable);
    
    // Advanced search method with multiple filters
    Page<Property> advancedSearch(
        String keyword,
        String typeName,
        Double minPrice,
        Double maxPrice,
        Double minArea,
        Double maxArea,
        PropertyStatus status,
        TransactionType transactionType,
        Pageable pageable
    );

    Page<Property> getAllAvailableProperties(Pageable pageable);
    List<Property> searchAvailableProperties(String keyword);

    // Recommended Properties
    List<PropertySummaryDTO> getRecommendedProperties(Long userId, int limit);

    long countAllProperties();

    // Dữ liệu cho biểu đồ
    java.util.Map<String, Long> getPropertyTypeDistributionForChart();

    long countAvailableProperties(); // Đếm các bất động sản đang "Available"
    
    // Thêm phương thức lấy tất cả loại bất động sản
    List<PropertyType> getAllPropertyTypes();

    /**
     * Lưu đối tượng Property đã chỉnh sửa
     * @param property Đối tượng Property cần lưu
     * @return Đối tượng Property đã lưu
     */
    Property saveProperty(Property property);

    /**
     * Lọc bất động sản theo các tiêu chí
     * @param search Từ khóa tìm kiếm theo tên hoặc địa chỉ
     * @param type Loại bất động sản
     * @param status Trạng thái
     * @param minPrice Giá tối thiểu
     * @param maxPrice Giá tối đa
     * @param pageable Thông tin phân trang
     * @return Trang kết quả bất động sản thỏa mãn các điều kiện lọc
     */
    Page<Property> getFilteredProperties(String search, String type, String status, Double minPrice, Double maxPrice, Pageable pageable);
    
    /**
     * Cập nhật tỷ lệ hoa hồng cho tất cả các bất động sản có commissionRate = null
     * @return Số lượng bất động sản đã được cập nhật
     */
    int updateNullCommissionRates();
    
    /**
     * Cập nhật tỷ lệ hoa hồng cho tất cả các bất động sản
     * @param newRate Tỷ lệ hoa hồng mới
     * @return Số lượng bất động sản đã được cập nhật
     */
    int updateAllCommissionRates(Double newRate);
} 