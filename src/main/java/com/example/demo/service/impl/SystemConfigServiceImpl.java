package com.example.demo.service.impl;

import com.example.demo.model.SystemConfig;
import com.example.demo.repository.SystemConfigRepository;
import com.example.demo.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SystemConfigServiceImpl implements SystemConfigService {

    // Key cho tỷ lệ hoa hồng mặc định
    public static final String DEFAULT_COMMISSION_RATE_KEY = "default_commission_rate";
    
    @Autowired
    private SystemConfigRepository systemConfigRepository;
    
    @Override
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }
    
    @Override
    public Optional<SystemConfig> getConfigByKey(String key) {
        return systemConfigRepository.findByKey(key);
    }
    
    @Override
    public String getConfigValue(String key, String defaultValue) {
        return getConfigByKey(key)
                .map(SystemConfig::getValue)
                .orElse(defaultValue);
    }
    
    @Override
    public Double getConfigDoubleValue(String key, Double defaultValue) {
        try {
            String value = getConfigValue(key, defaultValue != null ? defaultValue.toString() : null);
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    @Override
    public SystemConfig updateConfig(String key, String value, String description) {
        SystemConfig config = getConfigByKey(key)
                .orElse(new SystemConfig(key, value, description));
        
        config.setValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        
        return systemConfigRepository.save(config);
    }
    
    @Override
    public SystemConfig updateConfigDouble(String key, Double doubleValue, String description) {
        String value = doubleValue != null ? doubleValue.toString() : "0.0";
        return updateConfig(key, value, description);
    }
    
    @Override
    public void deleteConfig(String key) {
        getConfigByKey(key).ifPresent(systemConfigRepository::delete);
    }
    
    @Override
    public Double getDefaultCommissionRate() {
        // Tỷ lệ hoa hồng mặc định là 5.0% nếu không có cấu hình
        return getConfigDoubleValue(DEFAULT_COMMISSION_RATE_KEY, 5.0);
    }
    
    @Override
    public SystemConfig updateDefaultCommissionRate(Double rate) {
        return updateConfigDouble(
                DEFAULT_COMMISSION_RATE_KEY, 
                rate, 
                "Tỷ lệ hoa hồng mặc định (%)"
        );
    }
} 