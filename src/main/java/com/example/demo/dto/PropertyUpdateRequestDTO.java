package com.example.demo.dto;

import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.TransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// Sử dụng Lombok nếu có trong dự án để giảm boilerplate code, nếu không thì tự tạo getter/setter
// @Data // Lombok
// @NoArgsConstructor // Lombok
// @AllArgsConstructor // Lombok
public class PropertyUpdateRequestDTO {

    @NotBlank(message = "Tên bất động sản không được để trống")
    @Size(min = 3, max = 100, message = "Tên bất động sản phải từ 3 đến 100 ký tự")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(min = 5, message = "Mô tả phải có ít nhất 5 ký tự")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá phải là số dương")
    private Double price;

    @NotNull(message = "Diện tích không được để trống")
    @Min(value = 0, message = "Diện tích phải là số dương")
    private Double area;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotNull(message = "Loại bất động sản không được để trống")
    private Long propertyTypeId;

    // Có thể để null nếu không muốn thay đổi chủ sở hữu khi cập nhật từ realtor
    private Long ownerId; 

    private PropertyStatus status;

    private TransactionType transactionType;

    private Integer bedrooms;

    private Integer bathrooms;

    private Integer yearBuilt;

    private Boolean featured;

    private Double latitude;
    private Double longitude;

    // Danh sách ID của các ảnh hiện tại muốn giữ lại (nếu null hoặc rỗng nghĩa là giữ tất cả ảnh cũ HOẶC thay thế hoàn toàn bằng ảnh mới, tùy logic xử lý)
    private List<Long> existingImageIds;

    // Thêm các trường meta nếu cần cập nhật
    private String metaTitle;
    private String metaDescription;
    private String socialImageUrl;
    private String languageCode;
    private Double commissionRate;
    private String rentalPeriod;

    // Getters and Setters
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getPropertyTypeId() {
        return propertyTypeId;
    }

    public void setPropertyTypeId(Long propertyTypeId) {
        this.propertyTypeId = propertyTypeId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
        this.status = status;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public Integer getYearBuilt() {
        return yearBuilt;
    }

    public void setYearBuilt(Integer yearBuilt) {
        this.yearBuilt = yearBuilt;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<Long> getExistingImageIds() {
        return existingImageIds;
    }

    public void setExistingImageIds(List<Long> existingImageIds) {
        this.existingImageIds = existingImageIds;
    }

    public String getMetaTitle() {
        return metaTitle;
    }

    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public String getSocialImageUrl() {
        return socialImageUrl;
    }

    public void setSocialImageUrl(String socialImageUrl) {
        this.socialImageUrl = socialImageUrl;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public String getRentalPeriod() {
        return rentalPeriod;
    }

    public void setRentalPeriod(String rentalPeriod) {
        this.rentalPeriod = rentalPeriod;
    }
} 