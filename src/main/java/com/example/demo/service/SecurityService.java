package com.example.demo.service;

import com.example.demo.model.CustomUserDetail;

/**
 * Service cung cấp các phương thức kiểm tra bảo mật cho các yêu cầu giao dịch.
 */
public interface SecurityService {

    /**
     * Kiểm tra xem người dùng hiện tại có phải là chủ sở hữu của yêu cầu giao dịch hay không.
     *
     * @param requestId ID của yêu cầu giao dịch
     * @param userDetail Thông tin người dùng hiện tại
     * @return true nếu người dùng là chủ sở hữu, false nếu không phải
     */
    boolean isOwner(Long requestId, CustomUserDetail userDetail);
} 