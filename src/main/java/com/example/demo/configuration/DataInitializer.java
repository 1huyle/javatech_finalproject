package com.example.demo.configuration;

import com.example.demo.model.*;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.UserRole;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.demo.service.SystemConfigService;
import com.example.demo.service.impl.SystemConfigServiceImpl;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SystemConfigService systemConfigService;

    private User createUserIfNotFound(UserRepository userRepository, String email, String phone, String firstName, String lastName, String password, String address, UserRole role) {
        Optional<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.isPresent()) {
            return existingByEmail.get();
        }
        Optional<User> existingByPhone = userRepository.findByPhone(phone);
        if (existingByPhone.isPresent()) {
            System.out.println("Warning: User with phone " + phone + " already exists, but with a different email. Returning existing user by phone.");
            return existingByPhone.get(); 
        }
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setPhone(phone);
        newUser.setAddress(address);
        newUser.setRole(role);
        newUser.setEnabled(true);
        return userRepository.save(newUser);
    }

    @Bean
    @Transactional
    public CommandLineRunner initData(
            UserRepository userRepository,
            PropertyTypeRepository propertyTypeRepository,
            PropertyRepository propertyRepository,
            PropertyImageRepository propertyImageRepository
    ) {
        return args -> {
            try {
                System.out.println("Initializing fresh sample data...");
                
                // }
                
                User admin = createUserIfNotFound(userRepository, "admin2@gmail.com", "3333333333", "Admin", "User", "123456", "Quận 1, TP.HCM", UserRole.ADMIN);
                User realtorUser = createUserIfNotFound(userRepository, "pklinhbl9848@gmail.com", "0923456781", "Nguyễn", "Môi giới", "password", "Quận 2, TP.HCM", UserRole.REALTOR);
                User ownerUser = createUserIfNotFound(userRepository, "pklinh@gmail.com", "0934567892", "Trần", "Chủ nhà", "password", "Quận 3, TP.HCM", UserRole.USER);
                
                // Add the user that matches the JWT token in the cookie
                User tokenUser = createUserIfNotFound(userRepository, "khanhbl9848@gmail.com", "0912345678", "Linh", "Phạm", "password", "Quận 7, TP.HCM", UserRole.USER);

                PropertyType apartment = propertyTypeRepository.findByName("Căn hộ").orElseGet(() -> propertyTypeRepository.save(new PropertyType("Căn hộ", "Căn hộ chung cư cao cấp")));
                PropertyType house = propertyTypeRepository.findByName("Nhà phố").orElseGet(() -> propertyTypeRepository.save(new PropertyType("Nhà phố", "Nhà phố, nhà mặt tiền")));
                PropertyType villa = propertyTypeRepository.findByName("Biệt thự").orElseGet(() -> propertyTypeRepository.save(new PropertyType("Biệt thự", "Biệt thự cao cấp")));
                PropertyType land = propertyTypeRepository.findByName("Đất nền").orElseGet(() -> propertyTypeRepository.save(new PropertyType("Đất nền", "Đất nền dự án")));
                PropertyType condominiums = propertyTypeRepository.findByName("Chung cư").orElseGet(() -> propertyTypeRepository.save(new PropertyType("Chung cư", "Dự án chung cư")));
                PropertyType offices = propertyTypeRepository.findByName("Văn phòng").orElseGet(() -> propertyTypeRepository.save(new PropertyType("Văn phòng", "Văn phòng làm việc")));
    
                createProperty(propertyRepository, "Căn hộ cao cấp Quận 1", "Căn hộ cao cấp tại trung tâm Quận 1, thiết kế hiện đại, đầy đủ tiện nghi, gần các tiện ích như trung tâm thương mại, trường học, bệnh viện.", 2500000000.0, 100.0, "Quận 1, TP.HCM", "/assets/images/property1.jpg", apartment, ownerUser, realtorUser, true, 3, 2, 2020, 10.776389, 106.698079, PropertyStatus.AVAILABLE, TransactionType.SALE);
                createProperty(propertyRepository, "Nhà phố Thủ Đức", "Nhà phố tại Thủ Đức, thiết kế hiện đại, khu vực an ninh, gần trường học, bệnh viện và các tiện ích công cộng khác.", 3200000000.0, 150.0, "Thủ Đức, TP.HCM", "/assets/images/property2.jpg", house, ownerUser, realtorUser, false, 4, 3, 2019, 10.857910, 106.759470, PropertyStatus.AVAILABLE, TransactionType.SALE);
                createProperty(propertyRepository, "Biệt thự Bình Thạnh", "Biệt thự sang trọng tại Bình Thạnh, thiết kế theo phong cách hiện đại, sân vườn rộng rãi, hồ bơi riêng, khu vực an ninh 24/7.", 5800000000.0, 250.0, "Bình Thạnh, TP.HCM", "/assets/images/property3.jpg", villa, ownerUser, realtorUser, true, 5, 4, 2021, 10.807220, 106.721880, PropertyStatus.AVAILABLE, TransactionType.SALE);
                createProperty(propertyRepository, "Đất nền dự án Củ Chi", "Đất nền dự án tại Củ Chi, vị trí đắc địa, tiềm năng sinh lời cao, pháp lý rõ ràng, sổ đỏ trao tay.", 1500000000.0, 200.0, "Củ Chi, TP.HCM", "/assets/images/property4.jpg", land, ownerUser, realtorUser, false, null, null, null, 10.970520, 106.505920, PropertyStatus.AVAILABLE, TransactionType.SALE);
                createProperty(propertyRepository, "Căn hộ Phú Mỹ Hưng", "Căn hộ cao cấp tại Phú Mỹ Hưng, nội thất sang trọng, view sông thoáng mát, tiện ích đầy đủ, bảo vệ 24/7.", 3800000000.0, 120.0, "Phú Mỹ Hưng, Quận 7, TP.HCM", "/assets/images/property5.jpg", apartment, ownerUser, realtorUser, true, 3, 2, 2018, 10.728060, 106.722870, PropertyStatus.AVAILABLE, TransactionType.SALE);
                createProperty(propertyRepository, "Nhà phố Gò Vấp", "Nhà phố tại Gò Vấp, kết cấu 1 trệt 2 lầu, thiết kế hiện đại, nội thất cao cấp, khu dân cư an ninh.", 4200000000.0, 180.0, "Gò Vấp, TP.HCM", "/assets/images/property6.jpg", house, ownerUser, realtorUser, false, 4, 3, 2021, 10.815000, 106.665500, PropertyStatus.AVAILABLE, TransactionType.SALE);

                // Properties for Rent
                createProperty(propertyRepository, "Căn hộ dịch vụ cho thuê Quận 3", "Căn hộ dịch vụ đầy đủ nội thất, 1 phòng ngủ, cho thuê ngắn và dài hạn. Giá thuê theo tháng.", 15000000.0, 50.0, "Đường Nguyễn Thị Minh Khai, Quận 3, TP.HCM", "/assets/images/property_rent1.jpg", apartment, ownerUser, realtorUser, true, 1, 1, 2022, 10.779900, 106.690000, PropertyStatus.AVAILABLE, TransactionType.RENTAL);
                createProperty(propertyRepository, "Nhà cho thuê nguyên căn Bình Thạnh", "Nhà nguyên căn 3 lầu, 4 phòng ngủ, hẻm xe hơi, thích hợp làm văn phòng hoặc gia đình ở. Giá thuê theo tháng.", 25000000.0, 200.0, "Đường Hoàng Văn Thụ, Quận Tân Bình, TP.HCM", "/assets/images/property_rent2.jpg", house, ownerUser, realtorUser, false, 4, 3, 2015, 10.800000, 106.650000, PropertyStatus.AVAILABLE, TransactionType.RENTAL);
                createProperty(propertyRepository, "Văn phòng cho thuê Quận 1, view đẹp", "Văn phòng chuyên nghiệp, diện tích 80m2, tòa nhà hạng A, trung tâm Quận 1. Giá thuê theo tháng.", 35000000.0, 80.0, "Đường Lê Lợi, Phường Bến Nghé, Quận 1, TP.HCM", "/assets/images/property_rent3.jpg", offices, ownerUser, realtorUser, true, null, null, 2020, 10.775000, 106.702000, PropertyStatus.AVAILABLE, TransactionType.RENTAL);
                createProperty(propertyRepository, "Mặt bằng kinh doanh cho thuê Quận 10", "Mặt bằng kinh doanh vị trí đẹp, mặt tiền đường lớn, phù hợp nhiều ngành nghề.", 30000000.0, 100.0, "Quận 10, TP.HCM", "/assets/images/property4.jpg", offices, ownerUser, realtorUser, false, null, null, null, 10.776300, 106.665900, PropertyStatus.AVAILABLE, TransactionType.RENTAL);
                createProperty(propertyRepository, "Chung cư mini cho thuê gần trường đại học", "Chung cư mini đầy đủ tiện nghi, an ninh, gần các trường đại học lớn.", 5000000.0, 30.0, "Quận Thủ Đức (cũ), TP.HCM", "/assets/images/property5.jpg", condominiums, ownerUser, realtorUser, true, 1, 1, 2022, 10.853900, 106.755000, PropertyStatus.AVAILABLE, TransactionType.RENTAL);

                System.out.println("Sample data initialization complete.");
            } catch (Exception e) {
                System.err.println("Error during data initialization: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private void createProperty(
            PropertyRepository propertyRepository,
            String name,
            String description,
            double price,
            double area,
            String address,
            String fileUrl,
            PropertyType propertyType,
            User owner,
            User realtor,
            boolean featured,
            Integer bedrooms,
            Integer bathrooms,
            Integer yearBuilt,
            Double latitude,
            Double longitude,
            PropertyStatus status,
            TransactionType transactionType
    ) {
        
        Optional<Property> existingProperty = propertyRepository.findByNameAndAddress(name, address);
        if (existingProperty.isPresent()) {
            System.out.println("INFO: Property with name '" + name + "' and address '" + address + "' already exists. Skipping creation.");
            return;
        }

        Property property = new Property();
        property.setName(name);
        property.setDescription(description);
        property.setPrice(price);
        property.setArea(area);
        property.setAddress(address);
        property.setPropertyType(propertyType);
        property.setOwner(owner);
        property.setRealtor(realtor);
        property.setStatus(status);
        property.setFeatured(featured);
        property.setBedrooms(bedrooms);
        property.setBathrooms(bathrooms);
        property.setYearBuilt(yearBuilt);
        if (latitude != null && longitude != null) {
        property.setLatitude(latitude);
        property.setLongitude(longitude);
        }
        property.setTransactionType(transactionType);

        if (fileUrl != null && !fileUrl.isEmpty()) {
            PropertyImage image = new PropertyImage();
            image.setImageUrl(fileUrl);
            image.setPrimary(true);
            image.setProperty(property);
            property.getImages().add(image);
        }
        propertyRepository.save(property);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Khởi tạo cấu hình hệ thống
        initSystemConfigs();
    }
    
    /**
     * Khởi tạo cấu hình hệ thống
     */
    private void initSystemConfigs() {
        // Nếu chưa có cấu hình tỷ lệ hoa hồng mặc định, thì tạo với giá trị 5.0%
        if (systemConfigService.getConfigByKey(SystemConfigServiceImpl.DEFAULT_COMMISSION_RATE_KEY).isEmpty()) {
            systemConfigService.updateDefaultCommissionRate(5.0);
            log.info("Đã khởi tạo tỷ lệ hoa hồng mặc định: 5.0%");
        }
    }
} 