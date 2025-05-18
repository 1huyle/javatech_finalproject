package com.example.demo.service;

import com.example.demo.model.SystemConfig;
import java.util.List;
import java.util.Optional;

public interface SystemConfigService {
    
    /**
     * Lấy tất cả cấu hình hệ thống
     * @return Danh sách cấu hình
     */
    List<SystemConfig> getAllConfigs();
    
    /**
     * Lấy cấu hình theo key
     * @param key Key cần tìm
     * @return Optional chứa cấu hình tìm được
     */
    Optional<SystemConfig> getConfigByKey(String key);
    
    /**
     * Lấy giá trị cấu hình theo key
     * @param key Key cần lấy giá trị
     * @param defaultValue Giá trị mặc định nếu không tìm thấy
     * @return Giá trị cấu hình hoặc giá trị mặc định
     */
    String getConfigValue(String key, String defaultValue);
    
    /**
     * Lấy giá trị cấu hình dạng Double theo key
     * @param key Key cần lấy giá trị
     * @param defaultValue Giá trị mặc định nếu không tìm thấy
     * @return Giá trị Double hoặc giá trị mặc định
     */
    Double getConfigDoubleValue(String key, Double defaultValue);
    
    /**
     * Cập nhật hoặc tạo mới cấu hình
     * @param key Key cần cập nhật
     * @param value Giá trị mới
     * @param description Mô tả (có thể null)
     * @return Cấu hình đã cập nhật
     */
    SystemConfig updateConfig(String key, String value, String description);
    
    /**
     * Cập nhật giá trị cấu hình dạng Double
     * @param key Key cần cập nhật
     * @param doubleValue Giá trị Double mới
     * @param description Mô tả (có thể null)
     * @return Cấu hình đã cập nhật
     */
    SystemConfig updateConfigDouble(String key, Double doubleValue, String description);
    
    /**
     * Xóa cấu hình theo key
     * @param key Key cần xóa
     */
    void deleteConfig(String key);
    
    /**
     * Lấy giá trị tỷ lệ hoa hồng mặc định
     * @return Tỷ lệ hoa hồng mặc định (%)
     */
    Double getDefaultCommissionRate();
    
    /**
     * Cập nhật tỷ lệ hoa hồng mặc định
     * @param rate Tỷ lệ hoa hồng mới (%)
     * @return Cấu hình đã cập nhật
     */
    SystemConfig updateDefaultCommissionRate(Double rate);
} 