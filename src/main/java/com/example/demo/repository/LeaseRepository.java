package com.example.demo.repository;

import com.example.demo.model.Lease;
import com.example.demo.model.Property;
import com.example.demo.model.User;
import com.example.demo.model.enums.LeaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaseRepository extends JpaRepository<Lease, Long> {
    List<Lease> findByTenant(User tenant);
    List<Lease> findByProperty(Property property);
    List<Lease> findByStatus(LeaseStatus status);
    List<Lease> findByEndDateBefore(LocalDate date);
    List<Lease> findByEndDateAfter(LocalDate date);
    List<Lease> findByTenantAndStatus(User tenant, LeaseStatus status);
    List<Lease> findByPropertyAndStatus(Property property, LeaseStatus status);
    boolean existsByPropertyAndStatus(Property property, LeaseStatus status);
} 