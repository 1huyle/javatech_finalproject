package com.example.demo.repository;

import com.example.demo.model.Property;
import com.example.demo.model.PurchaseRequest;
import com.example.demo.model.User;
import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findByUser(User user);
    List<PurchaseRequest> findByProperty(Property property);
    List<PurchaseRequest> findByStatus(RequestStatus status);
    List<PurchaseRequest> findByPaymentMethod(PaymentMethod paymentMethod);
    List<PurchaseRequest> findByUserAndStatus(User user, RequestStatus status);
    List<PurchaseRequest> findByPropertyAndStatus(Property property, RequestStatus status);
    List<PurchaseRequest> findByExpectedDateBetween(LocalDate start, LocalDate end);
    List<PurchaseRequest> findByIsNegotiable(Boolean isNegotiable);
    
    // Tìm các yêu cầu mua với giá trong khoảng
    List<PurchaseRequest> findByPurchasePriceBetween(Double minPrice, Double maxPrice);
    
    // Tìm các yêu cầu mua bằng hình thức vay ngân hàng
    List<PurchaseRequest> findByPaymentMethodAndLoanAmountGreaterThan(PaymentMethod paymentMethod, Double minLoanAmount);

    boolean existsByUserAndPropertyAndStatusIn(User user, Property property, List<RequestStatus> statuses);
} 