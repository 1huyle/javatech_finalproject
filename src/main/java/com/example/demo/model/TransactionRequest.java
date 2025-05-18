package com.example.demo.model;

import com.example.demo.model.base.BaseEntity;
import com.example.demo.model.enums.RequestStatus;
import com.example.demo.model.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lớp cơ sở trừu tượng cho các yêu cầu giao dịch bất động sản.
 * Được kế thừa bởi PurchaseRequest (mua) và LeaseRequest (thuê).
 */
@Entity
@Table(name = "transaction_request")
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public abstract class TransactionRequest extends BaseEntity {
    
    // Thông tin cơ bản của yêu cầu
    
    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "transactions", "properties", "createdAt", "updatedAt", "createdBy", "updatedBy", "deletedAt", "enabled", "verificationToken", "tokenExpiryDate"})
    private User user;
    
    @NotNull
    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    @JsonIgnoreProperties({"owner", "realtor", "transactions", "createdAt", "updatedAt", "createdBy", "updatedBy", "deletedAt"})
    private Property property;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    
    // Ngày dự kiến hoàn thành giao dịch
    @Column(name = "expected_date")
    private LocalDate expectedDate;
    
    // Trạng thái và các mốc thời gian
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    // Ghi chú và thông tin bổ sung
    
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
    
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    // Mối quan hệ
    
    @ManyToOne
    @JoinColumn(name = "assigned_realtor_id")
    @JsonIgnoreProperties({"password", "transactions", "properties", "createdAt", "updatedAt", "createdBy", "updatedBy", "deletedAt", "enabled", "verificationToken", "tokenExpiryDate"})
    private User assignedRealtor;
    
    @OneToOne(mappedBy = "request")
    @JsonIgnoreProperties("request")
    private Transaction transaction;
    
    // Constructors
    
    public TransactionRequest() {
        super();
    }
    
    public TransactionRequest(User user, Property property, TransactionType type, LocalDate expectedDate) {
        this.user = user;
        this.property = property;
        this.type = type;
        this.expectedDate = expectedDate;
        this.status = RequestStatus.PENDING;
    }
    
    // Getters và setters
    
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
    }
    
    public RequestStatus getStatus() {
        return status;
    }
    
    public void setStatus(RequestStatus status) {
        this.status = status;
        
        // Tự động cập nhật các trường thời gian dựa theo trạng thái
        LocalDateTime now = LocalDateTime.now();
        if (status == RequestStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = now;
        } else if (status == RequestStatus.CANCELLED && this.cancelledAt == null) {
            this.cancelledAt = now;
        }
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public void setType(TransactionType type) {
        this.type = type;
    }
    
    public LocalDate getExpectedDate() {
        return expectedDate;
    }
    
    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public User getAssignedRealtor() {
        return assignedRealtor;
    }
    
    public void setAssignedRealtor(User assignedRealtor) {
        this.assignedRealtor = assignedRealtor;
    }
    
    public Transaction getTransaction() {
        return transaction;
    }
    
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
    
    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
    
    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public String getAdminNote() {
        return adminNote;
    }
    
    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    // Các phương thức tiện ích
    
    /**
     * Kiểm tra xem yêu cầu có đang ở trạng thái chờ xử lý hay không
     */
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }
    
    /**
     * Kiểm tra xem yêu cầu đã hoàn thành hay chưa
     */
    public boolean isCompleted() {
        return status == RequestStatus.COMPLETED;
    }
    
    /**
     * Kiểm tra xem yêu cầu đã bị hủy hay chưa
     */
    public boolean isCancelled() {
        return status == RequestStatus.CANCELLED;
    }
    
    /**
     * Kiểm tra xem yêu cầu có đang hoạt động hay không
     */
    public boolean isActive() {
        return status == RequestStatus.PENDING;
    }
} 