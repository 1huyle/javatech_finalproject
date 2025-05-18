package com.example.demo.repository;

import com.example.demo.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    /**
     * Tìm cấu hình theo key
     * @param key Key cần tìm
     * @return Optional chứa cấu hình tìm được
     */
    Optional<SystemConfig> findByKey(String key);
    
    /**
     * Kiểm tra xem key có tồn tại không
     * @param key Key cần kiểm tra
     * @return true nếu key tồn tại
     */
    boolean existsByKey(String key);
} 