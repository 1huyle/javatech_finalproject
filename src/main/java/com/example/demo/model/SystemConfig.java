package com.example.demo.model;

import com.example.demo.model.base.BaseEntity;
import jakarta.persistence.*;

/**
 * Lưu trữ cấu hình hệ thống
 */
@Entity
@Table(name = "system_config")
public class SystemConfig extends BaseEntity {
    
    @Column(name = "config_key", nullable = false, unique = true)
    private String key;
    
    @Column(name = "config_value", nullable = false)
    private String value;
    
    @Column(name = "description")
    private String description;
    
    public SystemConfig() {
        super();
    }
    
    public SystemConfig(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Lấy giá trị dưới dạng Double
     * @return Giá trị Double hoặc null nếu không thể chuyển đổi
     */
    public Double getDoubleValue() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Thiết lập giá trị từ Double
     * @param doubleValue Giá trị Double cần thiết lập
     */
    public void setDoubleValue(Double doubleValue) {
        this.value = doubleValue != null ? doubleValue.toString() : "0.0";
    }
} 