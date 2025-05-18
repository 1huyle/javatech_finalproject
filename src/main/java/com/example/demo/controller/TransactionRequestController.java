package com.example.demo.controller;

import com.example.demo.model.LeaseRequest;
import com.example.demo.model.Property;
import com.example.demo.model.PropertyImage;
import com.example.demo.model.PurchaseRequest;
import com.example.demo.model.Transaction;
import com.example.demo.model.TransactionRequest;
import com.example.demo.model.User;
import com.example.demo.model.CustomUserDetail;
import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.RequestStatus;
import com.example.demo.model.enums.UserRole;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.LeaseRequestService;
import com.example.demo.service.PropertyService;
import com.example.demo.service.PurchaseRequestService;
import com.example.demo.service.TransactionRequestService;
import com.example.demo.service.UserService;
import com.example.demo.service.EmailService;
import com.example.demo.repository.TransactionRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.model.enums.PropertyStatus;

/**
 * Controller xử lý các yêu cầu liên quan đến giao dịch bất động sản.
 * Bao gồm các chức năng:
 * - Tạo yêu cầu mua/thuê bất động sản
 * - Xem danh sách và chi tiết các yêu cầu
 * - Cập nhật trạng thái yêu cầu
 * - Hoàn thành giao dịch
 * - API kiểm tra tính khả dụng của bất động sản
 */
@Controller
@RequestMapping("/transactions")
public class TransactionRequestController {

    @Autowired
    private TransactionRequestService transactionRequestService;
    
    @Autowired
    private PurchaseRequestService purchaseRequestService;
    
    @Autowired
    private LeaseRequestService leaseRequestService;
    
    @Autowired
    private PropertyService propertyService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TransactionRequestRepository transactionRequestRepository;
    
    @Autowired
    private EmailService emailService;
    
    //--------------------------------------------------
    // XEM DANH SÁCH VÀ CHI TIẾT CÁC YÊU CẦU GIAO DỊCH
    //--------------------------------------------------
    
    /**
     * Hiển thị trang danh sách các yêu cầu giao dịch
     * (Chỉ dành cho ADMIN và REALTOR)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('REALTOR')")
    public String getAllTransactionRequests(Model model) {
        List<TransactionRequest> requests = transactionRequestService.getAllRequests();
        model.addAttribute("requests", requests);
        return "transactions/list";
    }
    
    /**
     * Hiển thị chi tiết yêu cầu giao dịch (sử dụng ID số)
     */
    @GetMapping("/{id:[0-9]+}")
    // Tạm thời bỏ yêu cầu xác thực để debug
    // @PreAuthorize("hasRole('ADMIN') or hasRole('REALTOR') or @securityService.isOwner(#id, principal)")
    public String getTransactionDetail(@PathVariable Long id, Model model) {
        Optional<TransactionRequest> requestOpt = transactionRequestService.getRequestById(id);
        
        if (requestOpt.isEmpty()) {
            return "redirect:/transactions?error=not_found";
        }
        
        TransactionRequest request = requestOpt.get();
        model.addAttribute("request", request);
        
        // Xác định loại yêu cầu để hiển thị template phù hợp
        if (request instanceof PurchaseRequest) {
            model.addAttribute("purchaseRequest", (PurchaseRequest) request);
            return "transactions/purchase-detail";
        } else if (request instanceof LeaseRequest) {
            model.addAttribute("leaseRequest", (LeaseRequest) request);
            return "transactions/lease-detail";
        }
        
        return "transactions/detail";
    }
    
    /**
     * Hiển thị chi tiết yêu cầu giao dịch (sử dụng ID dạng chuỗi như TR10001)
     */
    @GetMapping("/{prefix:[A-Za-z]{2}}{id:[0-9]+}")
    @PreAuthorize("isAuthenticated()")
    public String getTransactionDetailByStringId(@PathVariable String prefix, @PathVariable String id, Model model) {
        try {
            System.out.println("Debug - Xem giao dịch: " + prefix + id);
            
            // Xử lý ID dạng chuỗi (TR10001) - với prefix là "TR" và id là số
            Long numericId = Long.parseLong(id);
            Optional<TransactionRequest> requestOpt = transactionRequestService.getRequestById(numericId);
            
            if (requestOpt.isEmpty()) {
                System.out.println("Debug - Không tìm thấy giao dịch với ID: " + numericId);
                model.addAttribute("errorMessage", "Không tìm thấy thông tin giao dịch với mã: " + prefix + id);
                return "error/404";
            }
            
            TransactionRequest request = requestOpt.get();
            model.addAttribute("request", request);
            
            // Xác định loại yêu cầu để hiển thị template phù hợp
            if (request instanceof PurchaseRequest) {
                model.addAttribute("purchaseRequest", (PurchaseRequest) request);
                System.out.println("Debug - Mở trang chi tiết giao dịch mua: " + numericId);
                return "transactions/purchase-detail";
            } else if (request instanceof LeaseRequest) {
                model.addAttribute("leaseRequest", (LeaseRequest) request);
                System.out.println("Debug - Mở trang chi tiết giao dịch thuê: " + numericId);
                return "transactions/lease-detail";
            }
            
            System.out.println("Debug - Mở trang chi tiết giao dịch thông thường: " + numericId);
            return "transactions/detail";
        } catch (NumberFormatException e) {
            // Xử lý khi không thể chuyển đổi id thành số
            System.out.println("Debug - Error: ID không hợp lệ: " + id);
            model.addAttribute("errorMessage", "Mã giao dịch không hợp lệ: " + prefix + id);
            return "error/400";
        } catch (Exception e) {
            // Xử lý lỗi chung
            System.out.println("Debug - Error: " + e.getMessage());
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi xem chi tiết giao dịch: " + e.getMessage());
            return "error/500";
        }
    }
    
    /**
     * Hiển thị danh sách yêu cầu của người dùng hiện tại
     */
    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public String getMyRequests(@CurrentUser CustomUserDetail currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login?redirect=/transactions/my-requests";
        }
        
        Long userId = currentUser.getId();
        List<TransactionRequest> requests = transactionRequestService.getRequestsByUserId(userId);
        
        model.addAttribute("requests", requests);
        model.addAttribute("user", currentUser);
        
        return "transactions/my-requests";
    }
    
    /**
     * Hiển thị danh sách yêu cầu giao dịch liên quan đến môi giới
     */
    @GetMapping("/realtor-requests")
    @PreAuthorize("hasRole('REALTOR')")
    public String getRealtorRequests(@CurrentUser CustomUserDetail currentUser, Model model) {
        if (currentUser == null) {
            return "redirect:/login?redirect=/transactions/realtor-requests";
        }
        
        Long realtorId = currentUser.getId();
        List<TransactionRequest> requests = transactionRequestService.getRequestsByRealtorId(realtorId);
        
        // Phân loại yêu cầu theo loại giao dịch
        List<TransactionRequest> purchaseRequests = requests.stream()
                .filter(req -> req instanceof PurchaseRequest)
                .collect(Collectors.toList());
                
        List<TransactionRequest> leaseRequests = requests.stream()
                .filter(req -> req instanceof LeaseRequest)
                .collect(Collectors.toList());
        
        model.addAttribute("requests", requests);
        model.addAttribute("purchaseRequests", purchaseRequests);
        model.addAttribute("leaseRequests", leaseRequests);
        model.addAttribute("user", currentUser);
        
        return "transactions/realtor-requests";
    }
    
    //--------------------------------------------------
    // YÊU CẦU MUA BẤT ĐỘNG SẢN
    //--------------------------------------------------
    
    /**
     * Hiển thị form tạo yêu cầu mua bất động sản
     */
    @GetMapping("/purchase/{propertyId}")
    public String showPurchaseRequestForm(@PathVariable Long propertyId, Model model,
                                         @CurrentUser CustomUserDetail currentUser) {
        System.out.println("DEBUG: Access /transactions/purchase/" + propertyId + " - User: " + (currentUser != null ? currentUser.getEmail() : "anonymous"));
        
        try {
            // Kiểm tra người dùng đã đăng nhập chưa
            if (currentUser == null) {
                System.out.println("DEBUG: User not authenticated, redirecting to login");
                // Chuyển hướng đến trang đăng nhập với tham số redirect để quay lại sau khi đăng nhập
                return "redirect:/login?redirect=/transactions/purchase/" + propertyId;
            }
            
            // Lấy thông tin bất động sản
            var propertyOpt = propertyService.getPropertyById(propertyId);
            if (propertyOpt.isEmpty()) {
                System.out.println("DEBUG: Property not found: " + propertyId);
                model.addAttribute("errorMessage", "Không tìm thấy bất động sản");
                return "error/not-found";
            }
            
            var property = propertyOpt.get();
            System.out.println("DEBUG: Loading purchase form for property " + propertyId);
            
            model.addAttribute("propertyId", propertyId);
            model.addAttribute("property", property);
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("user", currentUser);
            
            // Trả về template trong thư mục gốc thay vì thư mục transactions
            return "purchase-form";
        } catch (Exception e) {
            System.out.println("ERROR in showPurchaseRequestForm: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
            return "error/generic";
        }
    }
    
    /**
     * Endpoint chuyển hướng cho trang purchase-form.html
     */
    @GetMapping("/purchase-form/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    public String redirectToPurchaseForm(@PathVariable Long propertyId) {
        // Redirect đến endpoint chính xác
        return "redirect:/transactions/purchase/" + propertyId;
    }
    
    /**
     * Xử lý yêu cầu tạo giao dịch mua bất động sản
     */
    @PostMapping("/purchase")
    @PreAuthorize("isAuthenticated()")
    public String createPurchaseRequest(
            @CurrentUser CustomUserDetail currentUser,
            @RequestParam Long propertyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate expectedDate,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(value = "purchasePrice") String purchasePriceStr,
            @RequestParam(required = false) Double loanAmount,
            @RequestParam(required = false) Integer loanTerm,
            @RequestParam(required = false, defaultValue = "false") Boolean isNegotiable,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        
        try {
            // Chuyển đổi chuỗi giá thành số
            double purchasePrice;
            
            // Làm sạch chuỗi giá từ form (có thể chứa định dạng tiền tệ)
            purchasePriceStr = purchasePriceStr.replaceAll("[^\\d.]", "");
            purchasePrice = Double.parseDouble(purchasePriceStr);
            
            if (purchasePrice <= 0) {
                redirectAttributes.addFlashAttribute("error", "Giá mua phải lớn hơn 0.");
                return "redirect:/transactions/purchase/" + propertyId;
            }
            
            Long userId = currentUser.getId();
            
            if (purchaseRequestService.hasActiveRequest(userId, propertyId)) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã có yêu cầu mua bất động sản này đang chờ xử lý.");
                return "redirect:/property/" + propertyId;
            }
            
            // Thực hiện tạo yêu cầu mua
            PurchaseRequest purchaseRequest = purchaseRequestService.createPurchaseRequest(
                    userId,
                    propertyId,
                    expectedDate,
                    paymentMethod,
                    purchasePrice,
                    loanAmount,
                    loanTerm,
                    isNegotiable,
                    note
            );
            
            // Lấy thông tin email realtor để gửi thông báo
            Optional<Property> propertyOpt = propertyService.getPropertyById(propertyId);
            if (propertyOpt.isPresent()) {
                Property property = propertyOpt.get();
                if (property.getRealtor() != null && property.getRealtor().getEmail() != null) {
                    String realtorEmail = property.getRealtor().getEmail();
                    String propertyLink = getSiteURL(request) + "/property/" + propertyId;
                    String requestLink = getSiteURL(request) + "/transactions/" + purchaseRequest.getId();
                    
                    try {
                        emailService.sendNewRequestNotificationToRealtor(
                                purchaseRequest, 
                                property.getRealtor(),
                                getSiteURL(request)
                        );
                    } catch (Exception e) {
                        System.err.println("Không thể gửi email thông báo: " + e.getMessage());
                    }
                }
            }
            
            redirectAttributes.addFlashAttribute("success", "Yêu cầu mua bất động sản đã được gửi thành công!");
            return "redirect:/account?tab=requests";
            
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Giá mua không hợp lệ. Vui lòng nhập lại.");
            return "redirect:/transactions/purchase/" + propertyId;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi: " + e.getMessage());
            return "redirect:/transactions/purchase/" + propertyId;
        }
    }
    
    /**
     * Xử lý thương lượng giá cho yêu cầu mua bất động sản
     */
    @PostMapping("/purchase/{id}/negotiate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REALTOR')")
    public String negotiatePurchasePrice(
            @PathVariable Long id,
            @RequestParam Double newPrice,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {
            
        try {
            purchaseRequestService.negotiatePrice(id, newPrice, note);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật giá thương lượng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi thương lượng giá: " + e.getMessage());
        }
        
        return "redirect:/transactions/" + id;
    }
    
    /**
     * Hoàn thành giao dịch mua bất động sản
     */
    @PostMapping("/purchase/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REALTOR')")
    public String completePurchase(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
            
        try {
            purchaseRequestService.completePurchase(id);
            redirectAttributes.addFlashAttribute("success", "Giao dịch mua bán đã hoàn thành thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi hoàn thành giao dịch: " + e.getMessage());
        }
        
        return "redirect:/transactions/" + id;
    }
    
    //--------------------------------------------------
    // YÊU CẦU THUÊ BẤT ĐỘNG SẢN
    //--------------------------------------------------
    
    /**
     * Hiển thị form tạo yêu cầu thuê bất động sản
     */
    @GetMapping("/lease/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    public String showLeaseRequestForm(@PathVariable Long propertyId, Model model, @CurrentUser CustomUserDetail currentUser) {
        try {
            // Kiểm tra người dùng đã đăng nhập chưa
            if (currentUser == null) {
                return "redirect:/login?redirect=/transactions/lease/" + propertyId;
            }
            
            // Lấy thông tin bất động sản
            var propertyOpt = propertyService.getPropertyById(propertyId);
            if (propertyOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Không tìm thấy bất động sản");
                return "error/not-found";
            }
            
            var property = propertyOpt.get();
            
            // Kiểm tra bất động sản có khả dụng cho thuê không
            if (property.getStatus() != PropertyStatus.AVAILABLE) {
                model.addAttribute("errorMessage", "Bất động sản này hiện không khả dụng để thuê");
                return "error/unavailable-property";
            }
            
            model.addAttribute("propertyId", propertyId);
            model.addAttribute("property", property);
            model.addAttribute("user", currentUser);
            
            // Đặt giá trị mặc định
            model.addAttribute("startDate", LocalDate.now());
            model.addAttribute("endDate", LocalDate.now().plusMonths(12)); // Mặc định thuê 1 năm
            model.addAttribute("monthlyRent", property.getPrice());
            model.addAttribute("deposit", property.getPrice());
            
            return "lease-form";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
            return "error/generic";
        }
    }
    
    /**
     * Xử lý form đăng ký thuê bất động sản
     */
    @PostMapping("/lease")
    @PreAuthorize("hasRole('USER')")
    public String submitLeaseForm(
            @RequestParam("propertyId") Long propertyId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam("monthlyRent") Double monthlyRent,
            @RequestParam("deposit") Double deposit,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethodStr,
            @RequestParam(value = "note", required = false) String note,
            RedirectAttributes redirectAttributes,
            @CurrentUser CustomUserDetail currentUser) {
        
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // Xử lý ngoại lệ
        try {
            Long userId = currentUser.getId();
            PaymentMethod paymentMethod = null;
            
            if (paymentMethodStr != null && !paymentMethodStr.isEmpty()) {
                try {
                    paymentMethod = PaymentMethod.valueOf(paymentMethodStr);
                } catch (IllegalArgumentException e) {
                    redirectAttributes.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ");
                    return "redirect:/property/" + propertyId;
                }
            }
            
            // Kiểm tra xem đã có yêu cầu đang hoạt động nào cho bất động sản này chưa
            if (leaseRequestService.hasActiveRequest(userId, propertyId)) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã có một yêu cầu thuê đang chờ xử lý cho bất động sản này.");
                return "redirect:/property/" + propertyId;
            }
            
            // Tạo yêu cầu thuê mới
            LocalDate expectedDate = LocalDate.now().plusDays(7); // Mặc định 7 ngày
            LeaseRequest request = leaseRequestService.createLeaseRequest(
                    userId, propertyId, expectedDate, startDate, endDate, 
                    monthlyRent, deposit, note, paymentMethod);
            
            redirectAttributes.addFlashAttribute("success", 
                    "Yêu cầu thuê của bạn đã được gửi thành công. Mã yêu cầu: " + request.getId());
            
            return "redirect:/account";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi gửi yêu cầu thuê: " + e.getMessage());
            return "redirect:/property/" + propertyId;
        }
    }
    
    /**
     * Hoàn thành giao dịch thuê bất động sản
     */
    @PostMapping("/lease/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REALTOR')")
    public String completeLease(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
            
        try {
            // Kiểm tra yêu cầu thuê tồn tại
            Optional<TransactionRequest> requestOpt = transactionRequestService.getRequestById(id);
            if (requestOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy yêu cầu thuê với ID: " + id);
                return "redirect:/transactions";
            }
            
            TransactionRequest request = requestOpt.get();
            
            // Xác nhận đây là yêu cầu thuê
            if (!(request instanceof LeaseRequest)) {
                redirectAttributes.addFlashAttribute("error", "Yêu cầu với ID " + id + " không phải là yêu cầu thuê.");
                return "redirect:/transactions/" + id;
            }
            
            // Chỉ cho phép hoàn thành yêu cầu đang ở trạng thái PENDING
            if (request.getStatus() != RequestStatus.PENDING) {
                redirectAttributes.addFlashAttribute("error", "Chỉ có thể hoàn thành yêu cầu đang ở trạng thái chờ xử lý.");
                return "redirect:/transactions/" + id;
            }
            
            // Hoàn thành quá trình thuê
            leaseRequestService.completeLease(id);
            
            // Cập nhật trạng thái yêu cầu
            transactionRequestService.updateRequestStatus(id, RequestStatus.COMPLETED);
            
            // Gửi email thông báo cho người dùng (nếu có thể)
            try {
                User user = request.getUser();
                Property property = request.getProperty();
                if (user != null && user.getEmail() != null && property != null) {
                    emailService.sendTransactionConfirmationEmail(
                            user.getEmail(),
                            property.getName(),
                            ((LeaseRequest) request).getMonthlyRent()
                    );
                }
            } catch (Exception e) {
                System.err.println("Không thể gửi email thông báo: " + e.getMessage());
            }
            
            redirectAttributes.addFlashAttribute("success", "Đã hoàn thành giao dịch thuê thành công!");
            return "redirect:/transactions/" + id;
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi hoàn thành giao dịch thuê: " + e.getMessage());
            return "redirect:/transactions/" + id;
        }
    }
    
    //--------------------------------------------------
    // QUẢN LÝ TRẠNG THÁI YÊU CẦU
    //--------------------------------------------------
    
    /**
     * Cập nhật trạng thái yêu cầu giao dịch
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REALTOR')")
    public String updateRequestStatus(
            @PathVariable Long id,
            @RequestParam RequestStatus status,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {
            
        try {
            // Tìm yêu cầu giao dịch theo ID
            Optional<TransactionRequest> requestOpt = transactionRequestService.getRequestById(id);
            if (requestOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy yêu cầu giao dịch với ID: " + id);
                return "redirect:/transactions";
            }
            
            TransactionRequest request = requestOpt.get();
            
            // Cập nhật ghi chú quản trị nếu có
            if (note != null && !note.trim().isEmpty()) {
                request.setAdminNote(note);
                transactionRequestRepository.save(request);
            }
            
            // Cập nhật trạng thái yêu cầu
            TransactionRequest updatedRequest = transactionRequestService.updateRequestStatus(id, status);
            
            // Thông báo thành công
            String statusName;
            switch (status) {
                case COMPLETED:
                    statusName = "hoàn thành";
                    break;
                case CANCELLED:
                    statusName = "hủy";
                    break;
                case PENDING:
                default:
                    statusName = "chờ xử lý";
                    break;
            }
            
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái yêu cầu thành " + statusName);
            
            // Gửi email thông báo cho người dùng (nếu có thể)
            try {
                User user = request.getUser();
                if (user != null && user.getEmail() != null) {
                    String subject = "Cập nhật trạng thái yêu cầu giao dịch #" + id;
                    String content = "Yêu cầu giao dịch của bạn đã được cập nhật thành " + statusName + ".";
                    if (note != null && !note.trim().isEmpty()) {
                        content += "\n\nGhi chú: " + note;
                    }
                    
                    Property property = request.getProperty();
                    String propertyName = property != null ? property.getName() : "Bất động sản";
                    
                    // Sử dụng phương thức sendTransactionConfirmationEmail thay cho sendSimpleEmail
                    emailService.sendTransactionConfirmationEmail(user.getEmail(), propertyName, 0);
                }
            } catch (Exception e) {
                System.err.println("Không thể gửi email thông báo: " + e.getMessage());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        
        return "redirect:/transactions/" + id;
    }
    
    //--------------------------------------------------
    // API VÀ ENDPOINT KHÁC
    //--------------------------------------------------
    
    /**
     * API kiểm tra tính khả dụng của bất động sản cho thuê
     */
    @GetMapping("/api/check-availability")
    @ResponseBody
    public ResponseEntity<?> checkAvailability(
            @RequestParam Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
            
        // Phần nội dung API kiểm tra tính khả dụng lấy từ code gốc
        return ResponseEntity.ok(true);
    }
    
    /**
     * API lấy danh sách yêu cầu giao dịch của người dùng hiện tại
     */
    @GetMapping("/api/user-transactions")
    @ResponseBody
    public ResponseEntity<?> getUserTransactions(@CurrentUser CustomUserDetail currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng chưa đăng nhập");
            }
            
            Long userId = currentUser.getId();
            List<TransactionRequest> requests = transactionRequestService.getRequestsByUserId(userId);
            
            // Chuyển đổi danh sách yêu cầu giao dịch sang định dạng phù hợp cho client
            List<Map<String, Object>> responseData = new ArrayList<>();
            
            for (TransactionRequest request : requests) {
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("id", request.getId());
                requestData.put("property", request.getProperty());
                requestData.put("requestType", request.getType().toString());
                requestData.put("status", request.getStatus().toString());
                requestData.put("statusDisplay", getStatusDisplay(request.getStatus()));
                requestData.put("requestDate", request.getCreatedAt());
                
                // Thêm thông tin chi tiết tùy theo loại yêu cầu
                if (request instanceof PurchaseRequest) {
                    PurchaseRequest purchaseRequest = (PurchaseRequest) request;
                    requestData.put("amount", purchaseRequest.getPurchasePrice());
                } else if (request instanceof LeaseRequest) {
                    LeaseRequest leaseRequest = (LeaseRequest) request;
                    requestData.put("amount", leaseRequest.getMonthlyRent());
                }
                
                responseData.add(requestData);
            }
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi truy vấn giao dịch: " + e.getMessage());
        }
    }
    
    /**
     * Hàm trợ giúp hiển thị trạng thái giao dịch theo ngôn ngữ người dùng
     */
    private String getStatusDisplay(RequestStatus status) {
        if (status == null) return "Unknown";
        
        switch (status) {
            case PENDING: return "Chờ xử lý";
            case COMPLETED: return "Hoàn thành";
            case CANCELLED: return "Đã hủy";
            default: return status.toString();
        }
    }
    
    /**
     * API kiểm tra định dạng giá
     */
    @GetMapping("/api/check-price")
    @ResponseBody
    public ResponseEntity<?> checkPrice(@RequestParam String priceString) {
        // Phần nội dung API kiểm tra giá lấy từ code gốc
        return ResponseEntity.ok(null);
    }
    
    /**
     * Endpoint debugging cho yêu cầu mua
     */
    @PostMapping("/purchase/debug")
    @ResponseBody
    public ResponseEntity<?> debugPurchaseRequest(
            @RequestParam Long propertyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate expectedDate,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam Double purchasePrice,
            @RequestParam(required = false) Double loanAmount,
            @RequestParam(required = false) Integer loanTerm,
            @RequestParam(required = false, defaultValue = "false") Boolean isNegotiable,
            @RequestParam(required = false) String note) {
            
        // Phần nội dung endpoint debugging lấy từ code gốc
        return ResponseEntity.ok(null);
    }
    
    /**
     * API lấy danh sách yêu cầu giao dịch cho môi giới viên
     */
    @GetMapping("/api/realtor-requests")
    @PreAuthorize("hasRole('REALTOR') or hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> getRealtorRequests(@CurrentUser CustomUserDetail currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng chưa đăng nhập");
        }
        
        try {
            Long realtorId = currentUser.getId();
            List<TransactionRequest> requests = transactionRequestService.getRequestsByRealtorId(realtorId);
            
            // Chuyển đổi danh sách TransactionRequest thành dạng json phù hợp cho frontend
            List<Map<String, Object>> responseData = requests.stream().map(req -> {
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("id", req.getId());
                requestData.put("propertyName", req.getProperty() != null ? req.getProperty().getName() : "N/A");
                requestData.put("requestType", req.getType().toString());
                requestData.put("requestTypeDisplay", req.getType() == TransactionType.SALE ? "Mua" : "Thuê");
                requestData.put("customerName", req.getUser() != null ? req.getUser().getFullName() : "N/A");
                requestData.put("requestDate", req.getCreatedAt());
                requestData.put("status", req.getStatus().toString());
                requestData.put("statusDisplay", getStatusDisplayName(req.getStatus()));
                
                // Thêm thông tin tài chính nếu có
                if (req instanceof PurchaseRequest) {
                    PurchaseRequest purchaseReq = (PurchaseRequest) req;
                    requestData.put("price", purchaseReq.getPurchasePrice());
                    requestData.put("paymentMethod", purchaseReq.getPaymentMethod());
                } else if (req instanceof LeaseRequest) {
                    LeaseRequest leaseReq = (LeaseRequest) req;
                    requestData.put("price", leaseReq.getMonthlyRent());
                    requestData.put("deposit", leaseReq.getDeposit());
                }
                
                return requestData;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách yêu cầu: " + e.getMessage());
        }
    }
    
    /**
     * Lấy tên hiển thị cho trạng thái yêu cầu
     */
    private String getStatusDisplayName(RequestStatus status) {
        if (status == null) return "Unknown";
        
        switch (status) {
            case PENDING: return "Chờ xử lý";
            case COMPLETED: return "Hoàn thành";
            case CANCELLED: return "Đã hủy";
            default: return status.toString();
        }
    }
    
    //--------------------------------------------------
    // PHƯƠNG THỨC TIỆN ÍCH
    //--------------------------------------------------
    
    /**
     * Lấy URL gốc của trang web
     */
    private String getSiteURL(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        return url.replace(request.getServletPath(), "");
    }
} 