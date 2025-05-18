package com.example.demo.service.impl;

import com.example.demo.model.Property;
import com.example.demo.model.TransactionRequest;
import com.example.demo.model.User;
import com.example.demo.model.enums.RequestStatus;
import com.example.demo.model.enums.UserRole;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.repository.PropertyRepository;
import com.example.demo.repository.TransactionRequestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.TransactionRequestService;
import com.example.demo.dto.AdminRecentOrderDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionRequestServiceImpl implements TransactionRequestService {

    @Autowired
    private TransactionRequestRepository transactionRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Override
    public List<TransactionRequest> getAllRequests() {
        return transactionRequestRepository.findAll();
    }

    @Override
    public Page<TransactionRequest> getAllRequests(Pageable pageable) {
        return transactionRequestRepository.findAll(pageable);
    }

    @Override
    public Optional<TransactionRequest> getRequestById(Long id) {
        return transactionRequestRepository.findById(id);
    }

    @Override
    public List<TransactionRequest> getRequestsByUser(User user) {
        return transactionRequestRepository.findByUser(user);
    }

    @Override
    public List<TransactionRequest> getRequestsByUserId(Long userId) {
        return transactionRequestRepository.findByUserId(userId);
    }

    @Override
    public List<TransactionRequest> getRequestsByUserAndStatus(User user, RequestStatus status) {
        return transactionRequestRepository.findByUserAndStatus(user, status);
    }

    @Override
    public List<TransactionRequest> getRequestsByProperty(Property property) {
        return transactionRequestRepository.findByProperty(property);
    }

    @Override
    public List<TransactionRequest> getPendingRequests() {
        return transactionRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    @Override
    public List<TransactionRequest> getActiveRequests() {
        List<RequestStatus> activeStatuses = Arrays.asList(
                RequestStatus.PENDING
        );
        return transactionRequestRepository.findByStatusIn(activeStatuses);
    }

    @Override
    public List<TransactionRequest> getCompletedRequests() {
        return transactionRequestRepository.findByStatus(RequestStatus.COMPLETED);
    }

    @Override
    public List<TransactionRequest> getRequestsByRealtorId(Long realtorId) {
        User realtor = userRepository.findById(realtorId)
                .orElseThrow(() -> new EntityNotFoundException("Realtor not found with ID: " + realtorId));
        if (realtor.getRole() != UserRole.REALTOR && realtor.getRole() != UserRole.ADMIN) {
            return Collections.emptyList();
        }
        return transactionRequestRepository.findByPropertyRealtor(realtor);
    }

    @Override
    public List<TransactionRequest> getRequestsByAssignedRealtorAndStatus(User realtor, RequestStatus status) {
        return transactionRequestRepository.findByAssignedRealtorAndStatus(realtor, status);
    }

    @Override
    public Page<TransactionRequest> getRequestsByStatus(RequestStatus status, Pageable pageable) {
        return transactionRequestRepository.findByStatus(status, pageable);
    }

    @Override
    public TransactionRequest updateRequestStatus(Long id, RequestStatus status) {
        Optional<TransactionRequest> optionalRequest = transactionRequestRepository.findById(id);
        if (optionalRequest.isPresent()) {
            TransactionRequest request = optionalRequest.get();
            
            // Không cho phép cập nhật từ trạng thái COMPLETED/CANCELLED về PENDING
            if ((request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) 
                && status == RequestStatus.PENDING) {
                throw new IllegalStateException("Không thể chuyển trạng thái yêu cầu đã hoàn thành hoặc đã hủy về chờ xử lý.");
            }
            
            // Cập nhật trạng thái yêu cầu
            request.setStatus(status);
            
            // Xử lý logic khi hoàn thành yêu cầu
            if (status == RequestStatus.COMPLETED && request.getProperty() != null) {
                Property property = request.getProperty();
                
                // Cập nhật trạng thái bất động sản tùy theo loại giao dịch
                if (request.getType() == TransactionType.SALE) {
                    // Nếu là giao dịch mua bán, đánh dấu bất động sản là đã bán
                    property.setStatus(PropertyStatus.SOLD);
                    // Cập nhật chủ sở hữu mới nếu có
                    if (request.getUser() != null) {
                        property.setOwner(request.getUser());
                    }
                } else if (request.getType() == TransactionType.RENTAL) {
                    // Nếu là giao dịch thuê, đánh dấu bất động sản là đã cho thuê
                    property.setStatus(PropertyStatus.RENTED);
                }
                
                // Lưu thông tin bất động sản cập nhật
                propertyRepository.save(property);
            } else if (status == RequestStatus.CANCELLED && request.getProperty() != null) {
                // Nếu hủy yêu cầu, có thể cập nhật trạng thái bất động sản về khả dụng nếu cần
                // Tùy theo logic nghiệp vụ của ứng dụng
            }
            
            // Lưu thông tin yêu cầu cập nhật
            return transactionRequestRepository.save(request);
        }
        
        throw new EntityNotFoundException("Không tìm thấy yêu cầu với ID: " + id);
    }

    @Override
    public void deleteRequest(Long id) {
        transactionRequestRepository.deleteById(id);
    }

    @Override
    public long countRequestsByStatus(RequestStatus status) {
        return transactionRequestRepository.countByStatus(status);
    }

    @Override
    public long countActiveRequestsByUser(User user) {
        List<RequestStatus> activeStatuses = Arrays.asList(
                RequestStatus.PENDING
        );
        return transactionRequestRepository.findByStatusInAndUser(activeStatuses, user).size();
    }

    @Override
    public String calculateTotalRevenue() {
        List<TransactionRequest> completedRequests = transactionRequestRepository.findByStatus(RequestStatus.COMPLETED);
        double totalRevenueSum = 0.0;
        for (TransactionRequest request : completedRequests) {
            if (request.getProperty() != null) {
                Double price = request.getProperty().getPrice();
                if (price != null) {
                    totalRevenueSum += price;
                }
            }
        }
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return currencyFormatter.format(totalRevenueSum);
    }

    @Override
    public long countNewOrders() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return transactionRequestRepository.countByStatusAndCreatedAtAfter(RequestStatus.COMPLETED, sevenDaysAgo);
    }

    @Override
    public List<AdminRecentOrderDTO> findRecentOrdersForAdmin(int limit) {
        // The 'limit' parameter is not strictly used here as we fetch for the last 7 days.
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<TransactionRequest> recentRequests = transactionRequestRepository.findAllByStatusAndCreatedAtAfterOrderByCreatedAtDesc(RequestStatus.COMPLETED, sevenDaysAgo);
        
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        return recentRequests.stream()
            .map(req -> {
                String customerName = "N/A";
                if (req.getUser() != null) {
                    customerName = (req.getUser().getFirstName() != null ? req.getUser().getFirstName() : "") + 
                                   " " + 
                                   (req.getUser().getLastName() != null ? req.getUser().getLastName() : "");
                    customerName = customerName.trim();
                    if (customerName.isEmpty()) customerName = "N/A";
                }

                String propertyName = "N/A";
                String amount = "0 VNĐ";
                if (req.getProperty() != null) {
                    propertyName = req.getProperty().getName() != null ? req.getProperty().getName() : "N/A";
                    Double price = req.getProperty().getPrice();
                    if (price != null) {
                        amount = currencyFormatter.format(price);
                    }
                }
                
                String statusName = "N/A";
                String statusDisplayName = "N/A";
                if (req.getStatus() != null) {
                    statusName = req.getStatus().name();
                    statusDisplayName = req.getStatus().getDisplayName();
                }

                return new AdminRecentOrderDTO(
                    req.getId(),
                    customerName,
                    propertyName,
                    amount,
                    statusName,
                    statusDisplayName, 
                    req.getCreatedAt()
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Double> getMonthlyRevenueDataForChart(int numberOfMonths) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(numberOfMonths - 1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<Map<String, Object>> monthlyData = transactionRequestRepository.findMonthlyRevenueSince(RequestStatus.COMPLETED, startDate);
        
        Map<String, Double> revenueByMonth = new LinkedHashMap<>(); // Giữ thứ tự chèn
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // Khởi tạo tất cả các tháng trong khoảng thời gian với doanh thu bằng 0
        YearMonth currentMonthIter = YearMonth.from(startDate.toLocalDate()); //Sửa ở đây
        YearMonth endMonth = YearMonth.now();
        while (!currentMonthIter.isAfter(endMonth)) {
            revenueByMonth.put(currentMonthIter.format(formatter), 0.0);
            currentMonthIter = currentMonthIter.plusMonths(1);
        }

        for (Map<String, Object> data : monthlyData) {
            Integer year = (Integer) data.get("year");
            Integer month = (Integer) data.get("month");
            // SUM(p.price) có thể trả về Long hoặc Double tùy theo DB và JPA provider
            // Chúng ta sẽ xử lý cả hai trường hợp
            Double monthRevenue = 0.0;
            Object revenueObj = data.get("totalRevenue");
            if (revenueObj instanceof Number) {
                 monthRevenue = ((Number) revenueObj).doubleValue();
            }

            if (year != null && month != null) {
                String monthKey = String.format("%d-%02d", year, month);
                revenueByMonth.put(monthKey, revenueByMonth.getOrDefault(monthKey, 0.0) + monthRevenue);
            }
        }
        return revenueByMonth;
    }
} 