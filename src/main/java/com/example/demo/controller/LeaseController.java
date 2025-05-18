package com.example.demo.controller;

import com.example.demo.model.Lease;
import com.example.demo.model.User;
import com.example.demo.model.enums.LeaseStatus;
import com.example.demo.service.LeaseService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leases")
public class LeaseController {

    @Autowired
    private LeaseService leaseService;

    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR', 'USER')")
    public ResponseEntity<Lease> createLease(
            @RequestParam Long tenantId,
            @RequestParam Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam double monthlyRent,
            @RequestParam double deposit) {
        
        return ResponseEntity.ok(leaseService.createLease(tenantId, propertyId, startDate, endDate, monthlyRent, deposit));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Lease> updateLeaseStatus(
            @PathVariable Long id,
            @RequestParam LeaseStatus status) {
        
        return ResponseEntity.ok(leaseService.updateLeaseStatus(id, status));
    }

    @PutMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> renewLease(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newEndDate,
            @RequestParam double newMonthlyRent) {
        
        leaseService.renewLease(id, newEndDate, newMonthlyRent);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<Void> terminateLease(
            @PathVariable Long id) {
        
        leaseService.terminateLease(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLease(@PathVariable Long id) {
        leaseService.deleteLease(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR', 'USER')")
    public ResponseEntity<Lease> getLeaseById(@PathVariable Long id) {
        return leaseService.getLeaseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-leases")
    @PreAuthorize("hasAnyRole('USER')")
    public ResponseEntity<List<Lease>> getMyLeases() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        
        return ResponseEntity.ok(leaseService.getLeasesByTenant(user));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<List<Lease>> getLeasesByStatus(
            @PathVariable LeaseStatus status) {
        
        return ResponseEntity.ok(leaseService.getLeasesByStatus(status));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<List<Lease>> getActiveLeases() {
        return ResponseEntity.ok(leaseService.getActiveLeases());
    }

    @GetMapping("/expired")
    @PreAuthorize("hasAnyRole('ADMIN', 'REALTOR')")
    public ResponseEntity<List<Lease>> getExpiredLeases() {
        return ResponseEntity.ok(leaseService.getExpiredLeases());
    }

    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkPropertyAvailability(
            @RequestParam Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return ResponseEntity.ok(leaseService.isPropertyAvailableForRent(propertyId, startDate, endDate));
    }
} 