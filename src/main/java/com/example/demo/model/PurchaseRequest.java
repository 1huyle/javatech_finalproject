package com.example.demo.model;

import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.TransactionType;
import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Yêu cầu mua bất động sản.
 * Mở rộng từ TransactionRequest với các thông tin liên quan đến giao dịch mua.
 */
@Entity
@Table(name = "purchase_request")
public class PurchaseRequest extends TransactionRequest {
    
    //--------------------------------------------------
    // Thông tin thanh toán
    //--------------------------------------------------
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;
    
    @Column(name = "purchase_price")
    private Double purchasePrice;
    
    //--------------------------------------------------
    // Thông tin khoản vay (nếu có)
    //--------------------------------------------------
    
    @Column(name = "loan_amount")
    private Double loanAmount;
    
    @Column(name = "loan_term")
    private Integer loanTerm;
    
    //--------------------------------------------------
    // Thông tin hoa hồng
    //--------------------------------------------------
    
    @Column(name = "commission_amount")
    private Double commissionAmount;
    
    @Column(name = "commission_rate")
    private Double commissionRate;
    
    //--------------------------------------------------
    // Thông tin thương lượng
    //--------------------------------------------------
    
    @Column(name = "is_negotiable")
    private Boolean isNegotiable = false;
    
    //--------------------------------------------------
    // Constructors
    //--------------------------------------------------
    
    /**
     * Constructor mặc định - tự động thiết lập loại giao dịch là SALE
     */
    public PurchaseRequest() {
        super();
        setType(TransactionType.SALE);
    }
    
    /**
     * Constructor đầy đủ - khởi tạo yêu cầu mua với thông tin cơ bản
     */
    public PurchaseRequest(User user, Property property, LocalDate expectedDate, 
                          PaymentMethod paymentMethod, Double purchasePrice) {
        super(user, property, TransactionType.SALE, expectedDate);
        this.paymentMethod = paymentMethod;
        this.purchasePrice = purchasePrice;
        
        // Tính toán hoa hồng nếu thông tin bất động sản có sẵn
        if (property != null) {
            this.commissionRate = property.getCommissionRate();
            this.commissionAmount = property.getCommissionAmount();
        }
    }
    
    //--------------------------------------------------
    // Phương thức nghiệp vụ
    //--------------------------------------------------
    
    /**
     * Tính toán số tiền hoa hồng dựa trên giá mua và tỷ lệ hoa hồng
     */
    private void calculateCommission() {
        if (purchasePrice != null && commissionRate != null) {
            this.commissionAmount = purchasePrice * (commissionRate / 100);
        }
    }
    
    /**
     * Kiểm tra xem giao dịch có sử dụng khoản vay hay không
     * @return true nếu sử dụng khoản vay
     */
    public boolean isLoan() {
        return paymentMethod == PaymentMethod.LOAN;
    }
    
    //--------------------------------------------------
    // Getters và Setters
    //--------------------------------------------------
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public Double getLoanAmount() {
        return loanAmount;
    }
    
    public void setLoanAmount(Double loanAmount) {
        this.loanAmount = loanAmount;
    }
    
    public Integer getLoanTerm() {
        return loanTerm;
    }
    
    public void setLoanTerm(Integer loanTerm) {
        this.loanTerm = loanTerm;
    }
    
    public Double getPurchasePrice() {
        return purchasePrice;
    }
    
    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
        calculateCommission();
    }
    
    public Double getCommissionAmount() {
        return commissionAmount;
    }
    
    public void setCommissionAmount(Double commissionAmount) {
        this.commissionAmount = commissionAmount;
    }
    
    public Double getCommissionRate() {
        return commissionRate;
    }
    
    public void setCommissionRate(Double commissionRate) {
        this.commissionRate = commissionRate;
        calculateCommission();
    }
    
    public Boolean getIsNegotiable() {
        return isNegotiable;
    }
    
    public void setIsNegotiable(Boolean isNegotiable) {
        this.isNegotiable = isNegotiable;
    }
} 