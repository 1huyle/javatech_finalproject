package com.example.demo.service;

import com.example.demo.model.TransactionRequest;
import com.example.demo.model.User;
import jakarta.mail.MessagingException;
import com.example.demo.dto.RealtorContactDTO;
import com.example.demo.model.Property;

public interface EmailService {

    /**
     * Gửi email thông báo cho realtor khi có yêu cầu mới (mua hoặc thuê).
     * @param request Yêu cầu (PurchaseRequest hoặc LeaseRequest)
     * @param realtor Người môi giới được gán
     * @param siteUrl URL cơ sở của trang web (ví dụ: http://localhost:8080)
     */
    void sendNewRequestNotificationToRealtor(TransactionRequest request, User realtor, String siteUrl);

    /**
     * Gửi email thông báo cho người dùng khi trạng thái yêu cầu của họ được cập nhật.
     * @param request Yêu cầu đã được cập nhật
     * @param user Người dùng (khách hàng)
     * @param siteUrl URL cơ sở của trang web
     */
    void sendRequestStatusUpdateToUser(TransactionRequest request, User user, String siteUrl);

    /**
     * Gửi email thông báo cho môi giới khi bất động sản của họ bị vô hiệu hóa
     * @param property Bất động sản bị vô hiệu hóa
     * @param realtor Môi giới của bất động sản
     */
    void sendPropertyDeactivationNotification(Property property, User realtor);

    /**
     * Gửi email thông báo cho môi giới khi bất động sản của họ được kích hoạt lại
     * @param property Bất động sản được kích hoạt lại
     * @param realtor Môi giới của bất động sản
     */
    void sendPropertyReactivationNotification(Property property, User realtor);

    void sendVerificationEmail(String to, String token) throws MessagingException;
    void sendPasswordResetEmail(String to, String newPassword) throws MessagingException;
    void sendTransactionConfirmationEmail(String to, String propertyName, double amount) throws MessagingException;

    void sendRealtorContactEmail(String realtorEmail, String propertyName, RealtorContactDTO contactDTO, String senderIpAddress);
    void sendContactFormConfirmationEmail(String userEmail, String propertyName, String realtorName, RealtorContactDTO contactDTO);
} 