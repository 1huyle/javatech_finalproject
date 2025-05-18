package com.example.demo.model;

import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.TransactionType;
import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Yêu cầu thuê bất động sản.
 * Mở rộng từ TransactionRequest với các thông tin liên quan đến giao dịch thuê.
 */
@Entity
@Table(name = "lease_request")
public class LeaseRequest extends TransactionRequest {
    
    //--------------------------------------------------
    // Thông tin thời gian thuê
    //--------------------------------------------------
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    //--------------------------------------------------
    // Thông tin chi phí thuê
    //--------------------------------------------------
    
    @Column(name = "monthly_rent", nullable = false)
    private Double monthlyRent;
    
    @Column(name = "deposit", nullable = false)
    private Double deposit;
    
    @Column(name = "rental_period")
    private String rentalPeriod = "Tháng";
    
    //--------------------------------------------------
    // Thông tin thanh toán
    //--------------------------------------------------
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;
    
    //--------------------------------------------------
    // Thông tin hoa hồng
    //--------------------------------------------------
    
    @Column(name = "commission_amount")
    private Double commissionAmount;
    
    @Column(name = "commission_rate")
    private Double commissionRate;
    
    //--------------------------------------------------
    // Mối quan hệ
    //--------------------------------------------------
    
    @OneToOne(mappedBy = "leaseRequest")
    private Lease lease;
    
    //--------------------------------------------------
    // Constructors
    //--------------------------------------------------
    
    /**
     * Constructor mặc định - tự động thiết lập loại giao dịch là RENTAL
     */
    public LeaseRequest() {
        super();
        setType(TransactionType.RENTAL);
    }
    
    /**
     * Constructor đầy đủ - khởi tạo yêu cầu thuê với thông tin cơ bản
     */
    public LeaseRequest(User user, Property property, LocalDate expectedDate, 
                      LocalDate startDate, LocalDate endDate, Double monthlyRent, Double deposit) {
        super(user, property, TransactionType.RENTAL, expectedDate);
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.deposit = deposit;
        
        // Thiết lập kỳ hạn thuê từ thông tin bất động sản nếu có
        if (property != null && property.getRentalPeriod() != null) {
            this.rentalPeriod = property.getRentalPeriod();
        }
        
        // Tính toán hoa hồng nếu thông tin bất động sản có sẵn
        if (property != null) {
            this.commissionRate = property.getCommissionRate();
            calculateCommission();
        }
    }
    
    //--------------------------------------------------
    // Phương thức nghiệp vụ
    //--------------------------------------------------
    
    /**
     * Tính toán thời hạn thuê theo tháng
     * @return Số tháng thuê
     */
    public int getDurationInMonths() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (endDate.getYear() - startDate.getYear()) * 12 
             + endDate.getMonthValue() - startDate.getMonthValue();
    }
    
    /**
     * Tính tổng số tiền thuê cho toàn bộ thời hạn hợp đồng
     * @return Tổng tiền thuê 
     */
    public Double getTotalRentAmount() {
        int months = getDurationInMonths();
        return monthlyRent != null ? monthlyRent * months : 0;
    }
    
    /**
     * Tính toán số tiền hoa hồng dựa trên giá thuê và tỷ lệ hoa hồng
     */
    private void calculateCommission() {
        if (monthlyRent != null && commissionRate != null) {
            // Tính hoa hồng dựa trên tổng tiền thuê (thay vì chỉ một tháng)
            int durationInMonths = getDurationInMonths();
            // Đảm bảo thời gian thuê ít nhất là 1 tháng để tránh hoa hồng bằng 0
            durationInMonths = Math.max(1, durationInMonths);
            
            // Tổng tiền thuê = giá thuê hàng tháng * số tháng thuê
            Double totalRent = monthlyRent * durationInMonths;
            
            // Hoa hồng = tổng tiền thuê * tỷ lệ hoa hồng / 100
            this.commissionAmount = totalRent * (commissionRate / 100);
        }
    }
    
    //--------------------------------------------------
    // Getters và Setters
    //--------------------------------------------------
    
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
    
    public Double getMonthlyRent() {
        return monthlyRent;
    }
    
    public void setMonthlyRent(Double monthlyRent) {
        this.monthlyRent = monthlyRent;
        calculateCommission();
    }
    
    public Double getDeposit() {
        return deposit;
    }
    
    public void setDeposit(Double deposit) {
        this.deposit = deposit;
    }
    
    public String getRentalPeriod() {
        return rentalPeriod;
    }
    
    public void setRentalPeriod(String rentalPeriod) {
        this.rentalPeriod = rentalPeriod;
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
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public Lease getLease() {
        return lease;
    }
    
    public void setLease(Lease lease) {
        this.lease = lease;
    }
} 