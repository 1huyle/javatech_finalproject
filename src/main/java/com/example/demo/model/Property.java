package com.example.demo.model;

import com.example.demo.model.base.BaseEntity;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.model.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "property",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "address"})
    }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "transactions", "images"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Property extends BaseEntity {
    @NotBlank
    @Size(min = 3, max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Size(min = 5)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private double price;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private double area;

    @NotBlank
    @Column(nullable = false)
    private String address;

    @Column
    private String fileUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status = PropertyStatus.AVAILABLE;

    @ManyToOne
    @JoinColumn(name = "property_type_id", nullable = false)
    private PropertyType propertyType;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "realtor_id")
    private User realtor;

    @OneToMany(mappedBy = "property")
    private List<Transaction> transactions;

    @Transient
    private MultipartFile imageFile;

    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description")
    private String metaDescription;

    @Column(name = "social_image_url")
    private String socialImageUrl;

    @Column(name = "language_code", length = 5)
    private String languageCode = "en";

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "featured")
    private Boolean featured = false;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "year_built")
    private Integer yearBuilt;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyImage> images = new ArrayList<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType = TransactionType.SALE;

    @Column(name = "commission_rate")
    private Double commissionRate;

    @Column(name = "rental_period")
    private String rentalPeriod;

    @Column
    private Boolean adminDeactivated = false;

    @Column
    private Boolean realtorDeleted = false;

    public Property() {
        super();
    }

    public Property(String name, String description, double price, double area, String address, String fileUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.area = area;
        this.address = address;
        this.fileUrl = fileUrl;
    }

    public boolean isAvailable() {
        return status == PropertyStatus.AVAILABLE;
    }

    public boolean isForSale() {
        return transactionType == TransactionType.SALE;
    }

    public boolean isForRent() {
        return transactionType == TransactionType.RENTAL;
    }

    /**
     * Tính toán số tiền hoa hồng dựa trên giá và tỷ lệ hoa hồng
     */
    public Double getCommissionAmount() {
        // Kiểm tra nếu tỷ lệ hoa hồng là null thì không thể tính
        if (commissionRate == null) {
            return 0.0;
        }
        return price * (commissionRate / 100);
    }

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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public User getRealtor() {
        return realtor;
    }

    public void setRealtor(User realtor) {
        this.realtor = realtor;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
        this.status = status;
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

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
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

    public List<PropertyImage> getImages() {
        return images;
    }

    public void setImages(List<PropertyImage> images) {
        this.images = images;
    }

    public void addImage(PropertyImage image) {
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(image);
        image.setProperty(this);
    }

    public void removeImage(PropertyImage image) {
        if (images != null) {
            images.remove(image);
            image.setProperty(null);
        }
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
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

    public Boolean getAdminDeactivated() {
        return adminDeactivated;
    }

    public void setAdminDeactivated(Boolean adminDeactivated) {
        this.adminDeactivated = adminDeactivated;
    }

    public Boolean getRealtorDeleted() {
        return realtorDeleted;
    }

    public void setRealtorDeleted(Boolean realtorDeleted) {
        this.realtorDeleted = realtorDeleted;
    }
}
