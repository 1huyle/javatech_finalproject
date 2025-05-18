package com.example.demo.model.enums;

public enum PaymentMethod {
    CASH("Tiền mặt"),
    BANK_TRANSFER("Chuyển khoản ngân hàng"),
    LOAN("Vay ngân hàng"),
    CREDIT_CARD("Thẻ tín dụng"),
    INSTALLMENT("Trả góp");
    
    private final String displayName;
    
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 