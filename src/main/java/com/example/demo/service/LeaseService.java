package com.example.demo.service;

import com.example.demo.model.Lease;
import com.example.demo.model.Property;
import com.example.demo.model.User;
import com.example.demo.model.enums.LeaseStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaseService {
    Lease createLease(Long tenantId, Long propertyId, LocalDate startDate, LocalDate endDate, double monthlyRent, double deposit);
    Lease updateLeaseStatus(Long id, LeaseStatus status);
    void deleteLease(Long id);
    Optional<Lease> getLeaseById(Long id);
    List<Lease> getLeasesByTenant(User tenant);
    List<Lease> getLeasesByProperty(Property property);
    List<Lease> getLeasesByStatus(LeaseStatus status);
    List<Lease> getActiveLeases();
    List<Lease> getExpiredLeases();
    void renewLease(Long id, LocalDate newEndDate, double newMonthlyRent);
    void terminateLease(Long id);
    boolean isPropertyAvailableForRent(Long propertyId, LocalDate startDate, LocalDate endDate);
    boolean hasActiveLease(Property property);
} 