package com.example.demo.repository;

import com.example.demo.model.Property;
import com.example.demo.model.PropertyType;
import com.example.demo.model.User;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    List<Property> findByOwner(User owner);
    List<Property> findByRealtor(User realtor);
    List<Property> findByPropertyType(PropertyType propertyType);
    List<Property> findByStatus(PropertyStatus status);
    List<Property> findByFeaturedTrue();
    List<Property> findByLanguageCode(String languageCode);
    List<Property> findByPropertyTypeAndIdNot(PropertyType propertyType, Long id);
    List<Property> findAllByOrderByCreatedAtDesc();
    
    // Find properties with null commission rate
    List<Property> findByCommissionRateIsNull();
    
    @Query("SELECT p FROM Property p WHERE p.price BETWEEN ?1 AND ?2")
    List<Property> findByPriceRange(double minPrice, double maxPrice);
    
    @Query("SELECT p FROM Property p WHERE p.area BETWEEN ?1 AND ?2")
    List<Property> findByAreaRange(double minArea, double maxArea);
    
    Page<Property> findAll(Pageable pageable);
    
    @Query("SELECT p FROM Property p WHERE p.name LIKE %?1% OR p.description LIKE %?1% OR p.address LIKE %?1%")
    List<Property> search(String keyword);

    @Query("SELECT p FROM Property p WHERE " +
           "6371 * acos(cos(radians(:latitude)) * cos(radians(p.latitude)) * " +
           "cos(radians(p.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(p.latitude))) <= :radius")
    List<Property> findByLocation(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("radius") Double radius
    );

    @Query("SELECT p FROM Property p WHERE " +
           "p.propertyType = :propertyType AND " +
           "p.id != :excludeId AND " +
           "ABS(p.price - :price) <= :price * 0.2 AND " +
           "ABS(p.area - :area) <= :area * 0.2 " +
           "ORDER BY ABS(p.price - :price) + ABS(p.area - :area) ASC")
    List<Property> findSimilarProperties(
        @Param("propertyType") PropertyType propertyType,
        @Param("price") double price,
        @Param("area") double area,
        @Param("excludeId") Long excludeId
    );
    
    @Query("SELECT p FROM Property p WHERE p.propertyType.name = :typeName")
    Page<Property> findByPropertyTypeName(@Param("typeName") String typeName, Pageable pageable);
    
    @Query("SELECT p FROM Property p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR p.name LIKE %:keyword% OR p.description LIKE %:keyword% OR p.address LIKE %:keyword%) AND " +
           "(:typeName IS NULL OR :typeName = '' OR p.propertyType.name = :typeName) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:minArea IS NULL OR p.area >= :minArea) AND " +
           "(:maxArea IS NULL OR p.area <= :maxArea) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:transactionType IS NULL OR p.transactionType = :transactionType)")
    Page<Property> advancedSearch(
        @Param("keyword") String keyword,
        @Param("typeName") String typeName,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        @Param("minArea") Double minArea,
        @Param("maxArea") Double maxArea,
        @Param("status") PropertyStatus status,
        @Param("transactionType") TransactionType transactionType,
        Pageable pageable
    );

    Page<Property> findByStatus(PropertyStatus status, Pageable pageable);

    @Query("SELECT p FROM Property p WHERE (p.name LIKE %:keyword% OR p.description LIKE %:keyword% OR p.address LIKE %:keyword%) AND p.status = :status")
    List<Property> searchByStatus(@Param("keyword") String keyword, @Param("status") PropertyStatus status);

    // For recommended properties - fetch top N available
    List<Property> findTop5ByStatusOrderByIdDesc(PropertyStatus status);

    List<Property> findByRealtorAndStatus(User realtor, PropertyStatus status);

    // For similar properties: by type, status, excluding self
    List<Property> findByPropertyTypeAndStatusAndIdNot(PropertyType propertyType, PropertyStatus status, Long id, Pageable pageable);

    Optional<Property> findByNameAndAddress(String name, String address);

    Page<Property> findByFeaturedTrueAndStatus(PropertyStatus status, Pageable pageable);

    Page<Property> findByRealtorIdAndStatusNot(Long realtorId, PropertyStatus statusNot, Pageable pageable);
    
    Optional<Property> findByNameAndAddressAndRealtorId(String name, String address, Long realtorId);

    List<Property> findByLanguageCodeAndStatus(String languageCode, PropertyStatus status);

    Page<Property> findByStatusOrderByCreatedAtDesc(PropertyStatus status, Pageable pageable);

    Page<Property> findByPropertyTypeAndStatus(PropertyType propertyType, PropertyStatus status, Pageable pageable);

    Page<Property> findByPropertyTypeNameAndStatus(String typeName, PropertyStatus status, Pageable pageable);

    Page<Property> findByIdNotInAndStatusOrderByCreatedAtDesc(@Param("ids") List<Long> ids, @Param("status") PropertyStatus status, Pageable pageable);

    @Query("SELECT new map(pt.name as typeName, COUNT(p) as propertyCount) " +
           "FROM Property p JOIN p.propertyType pt " +
           "GROUP BY pt.name")
    List<Map<String, Object>> countPropertiesByTypeName();

    long countByStatus(PropertyStatus status);
} 