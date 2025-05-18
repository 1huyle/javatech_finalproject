package com.example.demo.service;

import com.example.demo.model.LeaseRequest;
import com.example.demo.model.Property;
import com.example.demo.model.User;
import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.RequestStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaseRequestService {
    LeaseRequest createLeaseRequest(Long userId, Long propertyId, LocalDate expectedDate, 
                                  LocalDate startDate, LocalDate endDate, 
                                  Double monthlyRent, Double deposit, String note);
    
    LeaseRequest createLeaseRequest(Long userId, Long propertyId, LocalDate expectedDate, 
                                  LocalDate startDate, LocalDate endDate, 
                                  Double monthlyRent, Double deposit, String note,
                                  PaymentMethod paymentMethod);
    
    Optional<LeaseRequest> getRequestById(Long id);
    List<LeaseRequest> getRequestsByUser(User user);
    List<LeaseRequest> getRequestsByProperty(Property property);
    List<LeaseRequest> getPendingRequests();
    
    LeaseRequest updateRequestStatus(Long id, RequestStatus status);
    LeaseRequest assignRealtor(Long id, Long realtorId);
    void deleteLeaseRequest(Long id);
    
    // Check if property is available for rent in a given period
    boolean isPropertyAvailableForRent(Long propertyId, LocalDate startDate, LocalDate endDate);
    
    // Complete the lease process (create Lease entity)
    void completeLease(Long requestId);

    boolean hasActiveRequest(Long userId, Long propertyId);
} 