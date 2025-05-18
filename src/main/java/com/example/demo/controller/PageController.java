package com.example.demo.controller;

import com.example.demo.dto.RealtorContactDTO;
import com.example.demo.model.Property;
import com.example.demo.model.PropertyType;
import com.example.demo.model.User;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.service.EmailService;
import com.example.demo.service.PropertyService;
import com.example.demo.service.PropertyTypeService;
import com.example.demo.service.SystemConfigService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class PageController {
    
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    private PropertyService propertyService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PropertyTypeService propertyTypeService;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @GetMapping("/")
    public String index(Model model) {
        // Lấy danh sách bất động sản nổi bật
        List<Property> featuredProperties = propertyService.getFeaturedProperties();
        model.addAttribute("featuredProperties", featuredProperties);
        
        // Lấy danh sách bất động sản mới nhất
        List<Property> recentProperties = propertyService.getRecentProperties(3);
        model.addAttribute("recentProperties", recentProperties);
        
        // Lấy danh sách bất động sản theo loại
        model.addAttribute("apartments", propertyService.getPropertiesByTypeName("Căn hộ", 3));
        model.addAttribute("houses", propertyService.getPropertiesByTypeName("Nhà phố", 3));
        model.addAttribute("villas", propertyService.getPropertiesByTypeName("Biệt thự", 3));
        model.addAttribute("lands", propertyService.getPropertiesByTypeName("Đất nền", 3));
        
        return "index"; 
    }
    
    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/contact")
    public String contact() { return "contact"; }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/listings")
    public String listings(Model model,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long propertyTypeId,
                          @RequestParam(required = false) Long minPrice,
                          @RequestParam(required = false) Long maxPrice,
                          @RequestParam(required = false) Integer minArea,
                          @RequestParam(required = false) Integer maxArea,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "9") int size) {
        
        // Get all property types for the filter dropdown
        List<PropertyType> propertyTypes = propertyTypeService.getAllPropertyTypes();
        model.addAttribute("propertyTypes", propertyTypes);
        
        // Get typeName if propertyTypeId is provided
        String typeName = null;
        if (propertyTypeId != null) {
            typeName = propertyTypeService.getPropertyTypeById(propertyTypeId)
                    .map(PropertyType::getName)
                    .orElse(null);
        }
        
        // Convert to Double for the repository
        Double minPriceDouble = minPrice != null ? minPrice.doubleValue() : null;
        Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;
        Double minAreaDouble = minArea != null ? minArea.doubleValue() : null;
        Double maxAreaDouble = maxArea != null ? maxArea.doubleValue() : null;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // Perform the search with all parameters
        Page<Property> propertyPage = propertyService.advancedSearch(
                keyword, typeName, minPriceDouble, maxPriceDouble, minAreaDouble, maxAreaDouble,
                PropertyStatus.AVAILABLE, null, pageable);
        
        // Add attributes to the model
        model.addAttribute("properties", propertyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", propertyPage.getTotalPages());
        model.addAttribute("totalItems", propertyPage.getTotalElements());
        model.addAttribute("size", size);
        
        // Add search parameters to the model for form values
        model.addAttribute("keyword", keyword);
        model.addAttribute("propertyTypeId", propertyTypeId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minArea", minArea);
        model.addAttribute("maxArea", maxArea);
        
        return "listings";
    }

    @GetMapping("/rentals")
    public String rentals(Model model,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String propertyType,
                         @RequestParam(required = false) Long minPrice,
                         @RequestParam(required = false) Long maxPrice,
                         @RequestParam(required = false) Integer minArea,
                         @RequestParam(required = false) Integer maxArea,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "9") int size,
                         @RequestParam(defaultValue = "createdAt") String sortBy,
                         @RequestParam(defaultValue = "desc") String direction) {
        
        // Get all property types for the filter dropdown
        List<PropertyType> propertyTypes = propertyTypeService.getAllPropertyTypes();
        model.addAttribute("propertyTypes", propertyTypes);
        
        // Get typeName if propertyType is provided
        String typeName = propertyType;
        
        // Convert to Double for the repository
        Double minPriceDouble = minPrice != null ? minPrice.doubleValue() : null;
        Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;
        Double minAreaDouble = minArea != null ? minArea.doubleValue() : null;
        Double maxAreaDouble = maxArea != null ? maxArea.doubleValue() : null;
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Perform the search with all parameters
        Page<Property> propertyPage = propertyService.advancedSearch(
                keyword, typeName, minPriceDouble, maxPriceDouble, minAreaDouble, maxAreaDouble,
                PropertyStatus.AVAILABLE, TransactionType.RENTAL, pageable);
        
        // Add attributes to the model
        model.addAttribute("properties", propertyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", propertyPage.getTotalPages());
        model.addAttribute("totalItems", propertyPage.getTotalElements());
        model.addAttribute("sortField", sortBy);
        model.addAttribute("sortDir", direction);
        model.addAttribute("reverseSortDir", direction.equals("asc") ? "desc" : "asc");
        model.addAttribute("size", size);
        
        // Add search parameters to the model
        model.addAttribute("keyword", keyword);
        model.addAttribute("propertyType", propertyType);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minArea", minArea);
        model.addAttribute("maxArea", maxArea);
        
        // Reconstruct priceRange and areaRange for th:selected attribute in the template
        String priceRangeValue = "";
        if (minPrice != null || maxPrice != null) {
            if (minPrice != null && maxPrice != null) {
                priceRangeValue = (minPrice / 1000000) + "-" + (maxPrice / 1000000);
            } else if (minPrice != null) {
                priceRangeValue = (minPrice / 1000000) + "+";
            } else { // maxPrice != null
                // Assuming 0 as min for ranges like "0-10"
                priceRangeValue = "0-" + (maxPrice / 1000000);
            }
        }
        model.addAttribute("priceRange", priceRangeValue);

        String areaRangeValue = "";
        if (minArea != null || maxArea != null) {
            if (minArea != null && maxArea != null) {
                areaRangeValue = minArea + "-" + maxArea;
            } else if (minArea != null) {
                areaRangeValue = minArea + "+";
            } else { // maxArea != null
                 areaRangeValue = "0-" + maxArea; // Assuming 0 as min for ranges like "0-X"
            }
        }
        model.addAttribute("areaRange", areaRangeValue);
        
        return "rentals";
    }

    @GetMapping("/sales")
    public String sales(Model model,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String propertyType,
                       @RequestParam(required = false) Long minPrice,
                       @RequestParam(required = false) Long maxPrice,
                       @RequestParam(required = false) Integer minArea,
                       @RequestParam(required = false) Integer maxArea,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "9") int size,
                       @RequestParam(defaultValue = "createdAt") String sortBy,
                       @RequestParam(defaultValue = "desc") String direction) {
        
        // Get all property types for the filter dropdown
        List<PropertyType> propertyTypes = propertyTypeService.getAllPropertyTypes();
        model.addAttribute("propertyTypes", propertyTypes);
        
        // Get typeName if propertyType is provided
        String typeName = propertyType;
        
        // Convert to Double for the repository
        Double minPriceDouble = minPrice != null ? minPrice.doubleValue() : null;
        Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;
        Double minAreaDouble = minArea != null ? minArea.doubleValue() : null;
        Double maxAreaDouble = maxArea != null ? maxArea.doubleValue() : null;
        
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Perform the search with all parameters
        Page<Property> propertyPage = propertyService.advancedSearch(
                keyword, typeName, minPriceDouble, maxPriceDouble, minAreaDouble, maxAreaDouble,
                PropertyStatus.AVAILABLE, TransactionType.SALE, pageable);
        
        // Add attributes to the model
        model.addAttribute("properties", propertyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", propertyPage.getTotalPages());
        model.addAttribute("totalItems", propertyPage.getTotalElements());
        model.addAttribute("sortField", sortBy);
        model.addAttribute("sortDir", direction);
        model.addAttribute("reverseSortDir", direction.equals("asc") ? "desc" : "asc");
        model.addAttribute("size", size);
        
        // Add search parameters to the model
        model.addAttribute("keyword", keyword);
        model.addAttribute("propertyType", propertyType);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minArea", minArea);
        model.addAttribute("maxArea", maxArea);
        
        // Keep these for backward compatibility with the form
        model.addAttribute("priceRange", "");
        model.addAttribute("areaRange", "");
        
        return "sales";
    }

    @GetMapping("/property/{id}")
    public String propertyDetail(@PathVariable Long id, Model model) {
        try {
            var propertyOpt = propertyService.getPropertyById(id);
            if (propertyOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Không tìm thấy bất động sản");
                return "error/404";
            }
            
            var property = propertyOpt.get();
            
            // Ensure commission rate uses the system default if not already set
            if (property.getCommissionRate() == null) {
                property.setCommissionRate(systemConfigService.getDefaultCommissionRate());
            }
            
            model.addAttribute("property", property);
            model.addAttribute("defaultCommissionRate", systemConfigService.getDefaultCommissionRate());
            
            // Load featured properties of the same type
            List<Property> similarProperties = propertyService.getSimilarProperties(property, 4);
            model.addAttribute("similarProperties", similarProperties);
            
            // Thêm thuộc tính để biết đây là loại hình giao dịch nào
            model.addAttribute("transactionType", property.getTransactionType());
            
            // Sử dụng template có sẵn thay vì các template chưa tồn tại
            return "listing-detail";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading property: " + e.getMessage());
            return "error/500";
        }
    }

    @GetMapping("/payment")
    public String payment() { return "payment"; }

    @GetMapping("/payment-success")
    public String paymentSuccess() { return "payment-success"; }

    @GetMapping("/register")
    public String register() { return "register"; }

    @GetMapping("/auth-redirect")
    public String authRedirect() { return "auth-redirect"; }

    @GetMapping("/property/{id}/lease")
    public String propertyLeaseForm(@PathVariable Long id, Model model) {
        try {
            var propertyOpt = propertyService.getPropertyById(id);
            if (propertyOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Không tìm thấy bất động sản");
                return "error/404";
            }
            
            var property = propertyOpt.get();
            if (property.getTransactionType() != TransactionType.RENTAL) {
                model.addAttribute("errorMessage", "Bất động sản này không có sẵn để thuê");
                return "error/400";
            }
            
            model.addAttribute("property", property);
            return "lease-form";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading property: " + e.getMessage());
            return "error/500";
        }
    }

    @GetMapping("/property/{id}/purchase")
    public String propertyPurchaseForm(@PathVariable Long id, Model model) {
        try {
            var propertyOpt = propertyService.getPropertyById(id);
            if (propertyOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Không tìm thấy bất động sản");
                return "error/404";
            }
            
            var property = propertyOpt.get();
            
            // Ensure commission rate uses the system default if not already set
            if (property.getCommissionRate() == null) {
                property.setCommissionRate(systemConfigService.getDefaultCommissionRate());
            }
            
            model.addAttribute("property", property);
            model.addAttribute("defaultCommissionRate", systemConfigService.getDefaultCommissionRate());
            model.addAttribute("transactionType", property.getTransactionType());
            model.addAttribute("isSale", property.getTransactionType() == TransactionType.SALE);
            model.addAttribute("isRental", property.getTransactionType() == TransactionType.RENTAL);

            return "purchase-form";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error loading property: " + e.getMessage());
            return "error/500";
        }
    }

    @PostMapping("/property/{propertyId}/contact-realtor")
    @ResponseBody
    public ResponseEntity<?> handleRealtorContact(@PathVariable Long propertyId, 
                                                @Valid @RequestBody RealtorContactDTO contactDTO,
                                                HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        logger.info("Received contact request for property ID: {} from IP: {} with details: {}", propertyId, clientIp, contactDTO.getName());
        Optional<Property> propertyOptional = propertyService.getPropertyById(propertyId);
        if (propertyOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Bất động sản không tồn tại."));
        }

        Property property = propertyOptional.get();
        User realtor = property.getRealtor();

        if (realtor == null || realtor.getEmail() == null || realtor.getEmail().trim().isEmpty()) {
            logger.warn("Realtor or realtor email not found for property ID: {}", propertyId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Không tìm thấy thông tin người môi giới cho bất động sản này."));
        }

        try {
            // Send email to realtor
            emailService.sendRealtorContactEmail(realtor.getEmail(), property.getName(), contactDTO, clientIp);
            
            // Send confirmation email to the sender
            String realtorFullName = (realtor.getFirstName() != null ? realtor.getFirstName() : "") + " " + (realtor.getLastName() != null ? realtor.getLastName() : "");
            emailService.sendContactFormConfirmationEmail(contactDTO.getEmail(), property.getName(), realtorFullName.trim(), contactDTO);
            
            logger.info("Successfully processed contact request for property {} and sent emails.", propertyId);
            return ResponseEntity.ok(Map.of("message", "Tin nhắn của bạn đã được gửi thành công đến người môi giới và bạn sẽ nhận được email xác nhận."));
        } catch (Exception e) {
            logger.error("Error processing contact request for property ID: {}", propertyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Có lỗi xảy ra khi gửi tin nhắn. Vui lòng thử lại."));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }
} 