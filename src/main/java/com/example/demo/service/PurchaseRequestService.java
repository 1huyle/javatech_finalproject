package com.example.demo.service;

import com.example.demo.model.Property;
import com.example.demo.model.PurchaseRequest;
import com.example.demo.model.User;
import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.RequestStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseRequestService {
    PurchaseRequest createPurchaseRequest(Long userId, Long propertyId, LocalDate expectedDate, 
                                        PaymentMethod paymentMethod, Double purchasePrice,
                                        Double loanAmount, Integer loanTerm, Boolean isNegotiable, String note);
    
    Optional<PurchaseRequest> getRequestById(Long id);
    List<PurchaseRequest> getRequestsByUser(User user);
    List<PurchaseRequest> getRequestsByProperty(Property property);
    List<PurchaseRequest> getPendingRequests();
    
    PurchaseRequest updateRequestStatus(Long id, RequestStatus status);
    PurchaseRequest negotiatePrice(Long id, Double newPrice, String note);
    PurchaseRequest assignRealtor(Long id, Long realtorId);
    void deletePurchaseRequest(Long id);
    
    // Complete the purchase process
    void completePurchase(Long requestId);

    boolean hasActiveRequest(Long userId, Long propertyId);
} 