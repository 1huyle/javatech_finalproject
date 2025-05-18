package com.example.demo.service;import com.example.demo.model.Property;import com.example.demo.model.TransactionRequest;import com.example.demo.model.User;import com.example.demo.model.enums.RequestStatus;import com.example.demo.dto.AdminRecentOrderDTO;import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;import java.util.List;import java.util.Map;import java.util.Optional;public interface TransactionRequestService {    List<TransactionRequest> getAllRequests();    Page<TransactionRequest> getAllRequests(Pageable pageable);    Optional<TransactionRequest> getRequestById(Long id);    List<TransactionRequest> getRequestsByUser(User user);    List<TransactionRequest> getRequestsByUserId(Long userId);    List<TransactionRequest> getRequestsByUserAndStatus(User user, RequestStatus status);    List<TransactionRequest> getRequestsByProperty(Property property);    List<TransactionRequest> getPendingRequests();    List<TransactionRequest> getActiveRequests();    List<TransactionRequest> getCompletedRequests();    List<TransactionRequest> getRequestsByRealtorId(Long realtorId);    List<TransactionRequest> getRequestsByAssignedRealtorAndStatus(User realtor, RequestStatus status);    Page<TransactionRequest> getRequestsByStatus(RequestStatus status, Pageable pageable);
    
    TransactionRequest updateRequestStatus(Long id, RequestStatus status);
    void deleteRequest(Long id);
    
    // Thống kê
    long countRequestsByStatus(RequestStatus status);
    long countActiveRequestsByUser(User user);
    String calculateTotalRevenue();
    long countNewOrders();
    List<AdminRecentOrderDTO> findRecentOrdersForAdmin(int limit);

    // Dữ liệu cho biểu đồ
    Map<String, Double> getMonthlyRevenueDataForChart(int numberOfMonths);
} 