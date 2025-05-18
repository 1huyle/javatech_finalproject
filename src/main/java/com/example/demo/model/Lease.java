package com.example.demo.model;

import com.example.demo.model.base.BaseEntity;
import com.example.demo.model.enums.LeaseStatus;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Đối tượng đại diện cho một hợp đồng thuê bất động sản.
 * Được tạo ra sau khi LeaseRequest được phê duyệt.
 */
@Entity
@Table(name = "lease")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) 
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Lease extends BaseEntity {
    
    //--------------------------------------------------
    // Các bên liên quan trong hợp đồng
    //--------------------------------------------------
    
    @NotNull
    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
    
    //--------------------------------------------------
    // Thông tin thời gian thuê
    //--------------------------------------------------

    @NotNull
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaseStatus status;
    
    //--------------------------------------------------
    // Thông tin tài chính
    //--------------------------------------------------

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private double monthlyRent;

    @Min(0)
    @Column(nullable = false)
    private double deposit;
    
    //--------------------------------------------------
    // Thông tin thời gian kết thúc hợp đồng
    //--------------------------------------------------
    
    // Thời điểm chấm dứt hợp đồng (nếu có)
    private LocalDateTime terminatedAt;
    
    //--------------------------------------------------
    // Mối quan hệ
    //--------------------------------------------------

    @OneToOne(mappedBy = "lease", cascade = CascadeType.ALL)
    private Transaction transaction;

    @OneToOne
    @JoinColumn(name = "lease_request_id")
    private LeaseRequest leaseRequest;

    //--------------------------------------------------
    // Constructors
    //--------------------------------------------------
    
    /**
     * Constructor mặc định
     */
    public Lease() {
        super();
    }

    /**
     * Constructor đầy đủ
     */
    public Lease(User tenant, Property property, 
               LocalDate startDate, LocalDate endDate, 
               LeaseStatus status, double monthlyRent, double deposit) {
        this.tenant = tenant;
        this.property = property;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.monthlyRent = monthlyRent;
        this.deposit = deposit;
    }

    /**
     * Constructor từ LeaseRequest
     */
    public Lease(LeaseRequest leaseRequest, LeaseStatus status) {
        this.tenant = leaseRequest.getUser();
        this.property = leaseRequest.getProperty();
        this.startDate = leaseRequest.getStartDate();
        this.endDate = leaseRequest.getEndDate();
        this.status = status;
        this.monthlyRent = leaseRequest.getMonthlyRent();
        this.deposit = leaseRequest.getDeposit();
        this.leaseRequest = leaseRequest;
    }
    
    //--------------------------------------------------
    // Phương thức nghiệp vụ
    //--------------------------------------------------
    
    /**
     * Kiểm tra xem hợp đồng thuê đã hết hạn chưa
     */
    public boolean isExpired() {
        return endDate.isBefore(LocalDate.now());
    }

    /**
     * Tính toán thời hạn thuê theo tháng
     */
    public int getDurationInMonths() {
        return (endDate.getYear() - startDate.getYear()) * 12 
             + endDate.getMonthValue() - startDate.getMonthValue();
    }

    /**
     * Tính tổng số tiền thuê cho toàn bộ thời hạn hợp đồng
     */
    public double getTotalRentAmount() {
        return monthlyRent * getDurationInMonths();
    }
    
    //--------------------------------------------------
    // Getters và Setters
    //--------------------------------------------------

    public User getTenant() {
        return tenant;
    }

    public void setTenant(User tenant) {
        this.tenant = tenant;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LeaseStatus getStatus() {
        return status;
    }

    public void setStatus(LeaseStatus status) {
        this.status = status;
        
        // Nếu chấm dứt hợp đồng, lưu thời điểm
        if (status == LeaseStatus.TERMINATED && this.terminatedAt == null) {
            this.terminatedAt = LocalDateTime.now();
        }
    }

    public double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public double getDeposit() {
        return deposit;
    }

    public void setDeposit(double deposit) {
        this.deposit = deposit;
    }

    public LocalDateTime getTerminatedAt() {
        return terminatedAt;
    }

    public void setTerminatedAt(LocalDateTime terminatedAt) {
        this.terminatedAt = terminatedAt;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public LeaseRequest getLeaseRequest() {
        return leaseRequest;
    }

    public void setLeaseRequest(LeaseRequest leaseRequest) {
        this.leaseRequest = leaseRequest;
    }
} 