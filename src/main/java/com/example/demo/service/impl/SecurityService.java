package com.example.demo.service.impl;

import com.example.demo.model.CustomUserDetail;
import com.example.demo.model.TransactionRequest;
import com.example.demo.repository.TransactionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityService implements com.example.demo.service.SecurityService {

    private final TransactionRequestRepository transactionRequestRepository;

    /**
     * Kiểm tra xem người dùng hiện tại có phải là chủ sở hữu của yêu cầu giao dịch hay không.
     * @param requestId ID của yêu cầu giao dịch
     * @param userDetail Thông tin người dùng hiện tại
     * @return true nếu người dùng là chủ sở hữu, false nếu không phải
     */
    @Override
    public boolean isOwner(Long requestId, CustomUserDetail userDetail) {
        if (userDetail == null) {
            return false;
        }
        
        return transactionRequestRepository.findById(requestId)
                .map(request -> request.getUser().getId().equals(userDetail.getId()))
                .orElse(false);
    }
} 