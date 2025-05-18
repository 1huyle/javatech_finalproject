package com.example.demo.controller;

import com.example.demo.service.PropertyService;
import com.example.demo.service.TransactionRequestService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.demo.model.Property;
import com.example.demo.model.User;
import com.example.demo.model.enums.UserRole;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;
import com.example.demo.model.TransactionRequest;
import com.example.demo.model.enums.RequestStatus;
import com.example.demo.service.SystemConfigService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')") // Đảm bảo chỉ ADMIN mới truy cập được
public class AdminController {

    private final UserService userService;
    private final PropertyService propertyService;
    private final TransactionRequestService transactionRequestService;
    private final SystemConfigService systemConfigService;
    // Thêm các service khác nếu cần (ví dụ: PaymentService)

    @Autowired
    public AdminController(UserService userService, 
                           PropertyService propertyService, 
                           TransactionRequestService transactionRequestService,
                           SystemConfigService systemConfigService) {
        this.userService = userService;
        this.propertyService = propertyService;
        this.transactionRequestService = transactionRequestService;
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalProperties", propertyService.countAvailableProperties());
        model.addAttribute("totalRevenue", transactionRequestService.calculateTotalRevenue()); 
        model.addAttribute("newOrders", transactionRequestService.countNewOrders()); 
        model.addAttribute("newCustomers", userService.countNewCustomers()); 
        
        model.addAttribute("recentOrders", transactionRequestService.findRecentOrdersForAdmin(7)); 

        // Dữ liệu cho biểu đồ
        model.addAttribute("monthlyRevenueData", transactionRequestService.getMonthlyRevenueDataForChart(6)); // Lấy 6 tháng gần nhất
        model.addAttribute("propertyTypeData", propertyService.getPropertyTypeDistributionForChart());

        return "admin/dashboard";
    }

    // Các phương thức xử lý cho từng trang admin sẽ được thêm vào đây
    @GetMapping("/properties")
    public String adminProperties(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String search,
                                @RequestParam(required = false) String type,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String price) {
        try {
        Pageable pageable = PageRequest.of(page, size);
            Page<Property> propertiesPage;
            
            // Kiểm tra nếu có bất kỳ tham số lọc nào
            if (search != null || type != null || status != null || price != null) {
                // Xử lý lọc theo giá (nếu có)
                Double minPrice = null;
                Double maxPrice = null;
                if (price != null && !price.isEmpty()) {
                    String[] priceRange = price.split("-");
                    if (price.contains("+")) {
                        // Trường hợp "10+"
                        minPrice = Double.parseDouble(price.replace("+", "")) * 1_000_000_000;
                    } else if (priceRange.length == 2) {
                        // Trường hợp "0-1", "1-2", vv.
                        minPrice = Double.parseDouble(priceRange[0]) * 1_000_000_000;
                        maxPrice = Double.parseDouble(priceRange[1]) * 1_000_000_000;
                    }
                }
                
                propertiesPage = propertyService.getFilteredProperties(
                    search, type, status, minPrice, maxPrice, pageable);
            } else {
                // Nếu không có tham số lọc nào, lấy tất cả bất động sản
                propertiesPage = propertyService.getAllProperties(pageable);
            }
            
            // Ensure all properties have adminDeactivated and realtorDeleted values
            propertiesPage.getContent().forEach(property -> {
                if (property.getAdminDeactivated() == null) {
                    property.setAdminDeactivated(false);
                }
                if (property.getRealtorDeleted() == null) {
                    property.setRealtorDeleted(false);
                }
            });
            
            // Lấy danh sách các loại bất động sản để hiển thị trong form lọc
            model.addAttribute("propertyTypes", propertyService.getAllPropertyTypes());
            
        model.addAttribute("propertiesPage", propertiesPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", propertiesPage.getTotalPages());
        model.addAttribute("totalItems", propertiesPage.getTotalElements());
            
            return "admin/properties";
        } catch (Exception e) {
            e.printStackTrace();
            // Xử lý lỗi và hiển thị thông báo
            model.addAttribute("error", "Có lỗi xảy ra khi tải danh sách bất động sản: " + e.getMessage());
        return "admin/properties";
        }
    }

    @GetMapping("/users")
    public String adminUsers(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userService.getUsersByRole(UserRole.USER, pageable);
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("totalItems", usersPage.getTotalElements());
        return "admin/users";
    }

    @GetMapping("/realtors")
    public String adminRealtors(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> realtorsPage = userService.getUsersByRole(UserRole.REALTOR, pageable);
        model.addAttribute("realtorsPage", realtorsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", realtorsPage.getTotalPages());
        model.addAttribute("totalItems", realtorsPage.getTotalElements());
        return "admin/realtors";
    }

    @GetMapping("/orders")
    public String adminOrders(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.example.demo.model.TransactionRequest> ordersPage = transactionRequestService.getRequestsByStatus(RequestStatus.PENDING, pageable);
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        model.addAttribute("totalItems", ordersPage.getTotalElements());
        return "admin/orders";
    }

    @GetMapping("/payments")
    public String adminPayments(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.example.demo.model.TransactionRequest> paymentsPage = transactionRequestService.getRequestsByStatus(RequestStatus.COMPLETED, pageable);
        model.addAttribute("paymentsPage", paymentsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", paymentsPage.getTotalPages());
        model.addAttribute("totalItems", paymentsPage.getTotalElements());
        
        // Thêm thông tin tỷ lệ hoa hồng mặc định
        Double defaultCommissionRate = systemConfigService.getDefaultCommissionRate();
        model.addAttribute("defaultCommissionRate", defaultCommissionRate);
        
        return "admin/payments";
    }

    @PostMapping("/settings/commission-rate")
    public String updateDefaultCommissionRate(
            @RequestParam Double commissionRate,
            @RequestParam(value = "updateAllProperties", required = false) Boolean updateAllProperties,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate giá trị
            if (commissionRate == null || commissionRate < 0) {
                redirectAttributes.addFlashAttribute("error", "Tỷ lệ hoa hồng không hợp lệ");
                return "redirect:/admin/payments";
            }
            
            // Cập nhật giá trị mặc định
            systemConfigService.updateDefaultCommissionRate(commissionRate);
            
            // Cập nhật tỷ lệ hoa hồng cho tất cả bất động sản nếu được yêu cầu
            if (updateAllProperties != null && updateAllProperties) {
                int updatedCount = propertyService.updateAllCommissionRates(commissionRate);
                redirectAttributes.addFlashAttribute("success", 
                    "Đã cập nhật tỷ lệ hoa hồng mặc định thành " + commissionRate + "% và áp dụng cho " + updatedCount + " bất động sản.");
            } else {
                // Chỉ cập nhật các bất động sản có giá trị null
                int updatedCount = propertyService.updateNullCommissionRates();
                if (updatedCount > 0) {
                    redirectAttributes.addFlashAttribute("success", 
                        "Đã cập nhật tỷ lệ hoa hồng mặc định thành " + commissionRate + "% và áp dụng cho " + updatedCount + " bất động sản chưa có tỷ lệ hoa hồng.");
                } else {
                    redirectAttributes.addFlashAttribute("success", 
                        "Đã cập nhật tỷ lệ hoa hồng mặc định thành " + commissionRate + "%.");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật tỷ lệ hoa hồng: " + e.getMessage());
        }
        
        return "redirect:/admin/payments";
    }

    @GetMapping("/users/{id}")
    public String viewUserDetails(@PathVariable Long id, Model model) {
        Optional<User> userOpt = userService.getUserById(id);
        
        if (!userOpt.isPresent() || userOpt.get().getRole() != UserRole.USER) {
            return "redirect:/admin/users?error=User+not+found";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        // Lấy danh sách đơn hàng đã hoàn thành của user
        List<TransactionRequest> completedOrders = 
            transactionRequestService.getRequestsByUserAndStatus(user, RequestStatus.COMPLETED);
        
        // Add debug logs
        System.out.println("DEBUG - User ID: " + user.getId() + ", Email: " + user.getEmail());
        System.out.println("DEBUG - Completed orders count: " + (completedOrders != null ? completedOrders.size() : "null"));
        if (completedOrders != null && !completedOrders.isEmpty()) {
            System.out.println("DEBUG - First order ID: " + completedOrders.get(0).getId());
        }
        
        // Check ALL requests for this user to see if any exist
        List<TransactionRequest> allRequests = transactionRequestService.getRequestsByUser(user);
        System.out.println("DEBUG - All requests for user: " + (allRequests != null ? allRequests.size() : "null"));
        if (allRequests != null && !allRequests.isEmpty()) {
            System.out.println("DEBUG - Request statuses:");
            for (TransactionRequest req : allRequests) {
                System.out.println("   - Request ID: " + req.getId() + ", Status: " + req.getStatus());
            }
        }
        
        model.addAttribute("completedOrders", completedOrders);
        
        return "admin/user-details";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.getUserById(id);
            
            if (!userOpt.isPresent() || userOpt.get().getRole() != UserRole.USER) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng");
                return "redirect:/admin/users";
            }
            
            User user = userOpt.get();
            boolean currentStatus = user.isEnabled();
            
            // Toggle status
            if (currentStatus) {
                userService.disableUser(id);
                redirectAttributes.addFlashAttribute("success", "Đã vô hiệu hóa người dùng thành công");
            } else {
                userService.enableUser(id);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt người dùng thành công");
            }
            
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi thay đổi trạng thái người dùng: " + e.getMessage());
            return "redirect:/admin/users/" + id;
        }
    }

    @GetMapping("/realtors/{id}")
    public String viewRealtorDetails(@PathVariable Long id, Model model) {
        Optional<User> userOpt = userService.getUserById(id);
        
        if (!userOpt.isPresent() || userOpt.get().getRole() != UserRole.REALTOR) {
            return "redirect:/admin/realtors?error=Realtor+not+found";
        }
        
        User realtor = userOpt.get();
        model.addAttribute("realtor", realtor);
        
        // Lấy danh sách bất động sản của realtor
        List<Property> realtorProperties = propertyService.getPropertiesByRealtor(realtor);
        model.addAttribute("properties", realtorProperties);
        
        // Lấy danh sách giao dịch đã hoàn thành của realtor
        List<TransactionRequest> completedOrders = transactionRequestService.getRequestsByAssignedRealtorAndStatus(realtor, RequestStatus.COMPLETED);
        model.addAttribute("completedOrders", completedOrders);
        
        return "admin/realtor-details";
    }
    
    @PostMapping("/realtors/{id}/toggle-status")
    public String toggleRealtorStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.getUserById(id);
            
            if (!userOpt.isPresent() || userOpt.get().getRole() != UserRole.REALTOR) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy môi giới");
                return "redirect:/admin/realtors";
            }
            
            User realtor = userOpt.get();
            boolean currentStatus = realtor.isEnabled();
            
            // Toggle status
            if (currentStatus) {
                userService.disableUser(id);
                redirectAttributes.addFlashAttribute("success", "Đã vô hiệu hóa môi giới thành công");
            } else {
                userService.enableUser(id);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt môi giới thành công");
            }
            
            return "redirect:/admin/realtors/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi thay đổi trạng thái môi giới: " + e.getMessage());
            return "redirect:/admin/realtors/" + id;
        }
    }

} 