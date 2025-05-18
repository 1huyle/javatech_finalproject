package com.example.demo.model;

import com.example.demo.model.base.BaseEntity;
import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.model.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Đối tượng đại diện cho một giao dịch bất động sản.
 * Lưu trữ thông tin tài chính và trạng thái của giao dịch sau khi một yêu cầu
 * TransactionRequest được phê duyệt và tiến hành thực hiện.
 */
@Entity
@Table(name = "transaction")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Transaction extends BaseEntity {
    
    //--------------------------------------------------
    // Các bên liên quan trong giao dịch
    //--------------------------------------------------
    
    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
    
    //--------------------------------------------------
    // Thông tin chung về giao dịch
    //--------------------------------------------------
    
    @NotNull
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type = TransactionType.SALE; // Mặc định là giao dịch mua bán
    
    //--------------------------------------------------
    // Thông tin tài chính
    //--------------------------------------------------
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private double amount;

    @Min(0)
    @Column(name = "commission_amount")
    private double commissionAmount;
    
    @Column(name = "commission_rate")
    private Double commissionRate = 5.0; // Tỷ lệ hoa hồng mặc định 5%
    
    //--------------------------------------------------
    // Thời gian giao dịch
    //--------------------------------------------------
    
    private LocalDateTime completedAt;
    
    //--------------------------------------------------
    // Mối quan hệ
    //--------------------------------------------------
    
    @OneToOne
    @JoinColumn(name = "lease_id")
    private Lease lease;
    
    @OneToOne
    @JoinColumn(name = "request_id")
    private TransactionRequest request;
    
    //--------------------------------------------------
    // Constructors
    //--------------------------------------------------
    
    /**
     * Constructor mặc định
     */
    public Transaction() {
        super();
    }
    
    /**
     * Constructor đầy đủ cho giao dịch mới
     */
    public Transaction(User user, Property property, LocalDateTime transactionDate, 
                      TransactionStatus status, TransactionType type, double amount) {
        this.user = user;
        this.property = property;
        this.transactionDate = transactionDate;
        this.status = status;
        this.type = type;
        this.amount = amount;
        calculateCommission();
    }
    
    /**
     * Constructor từ một yêu cầu giao dịch (TransactionRequest)
     */
    public Transaction(TransactionRequest request) {
        this.user = request.getUser();
        this.property = request.getProperty();
        this.transactionDate = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
        this.type = request.getType();
        this.request = request;
        
        if (request instanceof PurchaseRequest) {
            PurchaseRequest purchaseRequest = (PurchaseRequest) request;
            this.amount = purchaseRequest.getPurchasePrice();
            this.paymentMethod = purchaseRequest.getPaymentMethod();
            this.commissionRate = purchaseRequest.getCommissionRate();
        } else if (request instanceof LeaseRequest) {
            LeaseRequest leaseRequest = (LeaseRequest) request;
            this.amount = leaseRequest.getDeposit();
            this.commissionRate = leaseRequest.getCommissionRate();
        }
        
        calculateCommission();
    }
    
    //--------------------------------------------------
    // Phương thức nghiệp vụ
    //--------------------------------------------------
    
    /**
     * Kiểm tra xem giao dịch đã hoàn thành chưa
     * @return true nếu giao dịch đã hoàn thành
     */
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }
    
    /**
     * Kiểm tra xem đây có phải là giao dịch thuê hay không
     * @return true nếu đây là giao dịch thuê
     */
    public boolean isRental() {
        return type == TransactionType.RENTAL;
    }
    
    /**
     * Tính toán số tiền hoa hồng dựa trên giá trị giao dịch và tỷ lệ hoa hồng
     */
    public void calculateCommission() {
        if (property != null && property.getCommissionRate() != null) {
            this.commissionRate = property.getCommissionRate();
        }
        this.commissionAmount = this.amount * (this.commissionRate / 100);
    }
    
    //--------------------------------------------------
    // Getters và Setters
    //--------------------------------------------------
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Property getProperty() {
        return property;
    }
    
    public void setProperty(Property property) {
        this.property = property;
        calculateCommission();
    }
    
    public Lease getLease() {
        return lease;
    }
    
    public void setLease(Lease lease) {
        this.lease = lease;
    }
    
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public TransactionStatus getStatus() {
        return status;
    }
    
    public void setStatus(TransactionStatus status) {
        this.status = status;
        if (status == TransactionStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public void setType(TransactionType type) {
        this.type = type;
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
        calculateCommission();
    }
    
    public double getCommissionAmount() {
        return commissionAmount;
    }
    
    public void setCommissionAmount(double commissionAmount) {
        this.commissionAmount = commissionAmount;
    }
    
    public Double getCommissionRate() {
        return commissionRate;
    }
    
    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
        calculateCommission();
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public TransactionRequest getRequest() {
        return request;
    }
    
    public void setRequest(TransactionRequest request) {
        this.request = request;
    }
} 