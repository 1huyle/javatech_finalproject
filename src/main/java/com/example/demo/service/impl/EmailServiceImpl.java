package com.example.demo.service.impl;

import com.example.demo.dto.RealtorContactDTO;
import com.example.demo.model.LeaseRequest;
import com.example.demo.model.Property;
import com.example.demo.model.PurchaseRequest;
import com.example.demo.model.TransactionRequest;
import com.example.demo.model.User;
import com.example.demo.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${site.base.url:http://localhost:8080}")
    private String siteBaseUrl;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    // --- Methods from original EmailService.java (74 lines) ---
    @Override
    public void sendVerificationEmail(String to, String token) throws MessagingException {
        Context context = new Context();
        context.setVariable("token", token);
        context.setVariable("siteBaseUrl", this.siteBaseUrl);
        // Giả sử template là "verification-email.html" trong thư mục templates
        String emailContent = templateEngine.process("verification-email", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailUsername);
        helper.setTo(to);
        helper.setSubject("Xác thực email của bạn");
        helper.setText(emailContent, true);
        mailSender.send(message);
        System.out.println("Đã gửi email xác thực tới: " + to);
    }

    @Override
    public void sendPasswordResetEmail(String to, String newPassword) throws MessagingException {
        Context context = new Context();
        context.setVariable("newPassword", newPassword);
        // Giả sử template là "password-reset-email.html" trong thư mục templates
        String emailContent = templateEngine.process("password-reset-email", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailUsername);
        helper.setTo(to);
        helper.setSubject("Yêu cầu đặt lại mật khẩu");
        helper.setText(emailContent, true);
        mailSender.send(message);
        System.out.println("Đã gửi email đặt lại mật khẩu tới: " + to);
    }

    @Override
    public void sendTransactionConfirmationEmail(String to, String propertyName, double amount) throws MessagingException {
        Context context = new Context();
        context.setVariable("propertyName", propertyName);
        context.setVariable("amount", amount);
        // Giả sử template là "transaction-confirmation-email.html" trong thư mục templates
        String emailContent = templateEngine.process("transaction-confirmation-email", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailUsername);
        helper.setTo(to);
        helper.setSubject("Xác nhận giao dịch thành công");
        helper.setText(emailContent, true);
        mailSender.send(message);
        System.out.println("Đã gửi email xác nhận giao dịch tới: " + to);
    }

    // --- Methods for new request notifications ---
    private static final String NEW_REQUEST_REALTOR_SUBJECT_FORMAT = "Thông báo yêu cầu mới: %s bất động sản '%s'";
    private static final String REQUEST_STATUS_USER_SUBJECT_FORMAT = "Cập nhật trạng thái yêu cầu '%s' cho bất động sản '%s'";

    @Async
    @Override
    public void sendNewRequestNotificationToRealtor(TransactionRequest request, User realtor, String siteUrlParam) {
        String actualSiteUrl = (siteUrlParam != null && !siteUrlParam.trim().isEmpty()) ? siteUrlParam.trim() : this.siteBaseUrl;
        if (realtor == null || realtor.getEmail() == null || realtor.getEmail().trim().isEmpty()) {
            System.err.println("Không thể gửi email: Realtor (" + (realtor != null ? "ID: " + realtor.getId() : "null") + ") hoặc email của realtor không hợp lệ.");
            return;
        }
        if (request == null || request.getProperty() == null) {
            System.err.println("Không thể gửi email cho yêu cầu " + (request != null ? "ID: "+ request.getId() : "null") + ": Thông tin yêu cầu hoặc bất động sản không tồn tại.");
            return;
        }

        String requestTypeDisplay = (request instanceof PurchaseRequest) ? "Đăng ký Mua" : "Đăng ký Thuê";
        String subject = String.format(NEW_REQUEST_REALTOR_SUBJECT_FORMAT, requestTypeDisplay, request.getProperty().getName());
        String requestDetailUrl = actualSiteUrl + "/account";

        Context context = new Context();
        context.setVariable("realtorName", (realtor.getFirstName() != null && !realtor.getFirstName().trim().isEmpty()) ? realtor.getFirstName() : realtor.getEmail());
        context.setVariable("requestTypeDisplay", requestTypeDisplay);
        context.setVariable("property", request.getProperty());
        context.setVariable("user", request.getUser());
        context.setVariable("requestDetailsLink", requestDetailUrl);
        context.setVariable("requestNote", request.getNote());
        
        if (request instanceof PurchaseRequest) {
            context.setVariable("purchaseRequest", (PurchaseRequest) request);
        } else if (request instanceof LeaseRequest) {
            context.setVariable("leaseRequest", (LeaseRequest) request);
        }

        String htmlContent;
        try {
            htmlContent = templateEngine.process("email/new-request-realtor-notification", context);
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý template email new-request-realtor-notification: " + e.getMessage());
            htmlContent = "<p>Kính gửi " + context.getVariable("realtorName") + ",</p>" +
                          "<p>Bạn có một yêu cầu " + requestTypeDisplay + " mới cho bất động sản <strong>" + request.getProperty().getName() + "</strong>.</p>" +
                          "<p>Người dùng: " + request.getUser().getFirstName() + " (" + request.getUser().getEmail() + ")</p>" +
                          "<p>Xem chi tiết tại: <a href='" + requestDetailUrl + "'>" + requestDetailUrl + "</a></p>"
                          + "<p>Ghi chú từ khách hàng: "+ (request.getNote() != null ? request.getNote() : "Không có") +"</p>";
        }
        
        sendHtmlEmail(realtor.getEmail(), subject, htmlContent);
        System.out.println("Đã xử lý gửi email thông báo yêu cầu ID " + request.getId() + " mới cho realtor: " + realtor.getEmail());
    }

    @Async
    @Override
    public void sendRequestStatusUpdateToUser(TransactionRequest request, User user, String siteUrlParam) {
        // Tham số siteUrlParam trong interface là String siteUrl, đổi tên ở đây để khớp.
        String actualSiteUrl = (siteUrlParam != null && !siteUrlParam.trim().isEmpty()) ? siteUrlParam.trim() : this.siteBaseUrl;
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            System.err.println("Không thể gửi email: User (" + (user != null ? "ID: " + user.getId() : "null") + ") hoặc email của user không hợp lệ.");
            return;
        }
        if (request == null || request.getProperty() == null) {
            System.err.println("Không thể gửi email cho yêu cầu " + (request != null ? "ID: "+ request.getId() : "null") + ": Thông tin yêu cầu hoặc bất động sản không tồn tại.");
            return;
        }

        String requestStatusDisplay = request.getStatus() != null ? request.getStatus().toString() : "Không xác định";
        String subject = String.format(REQUEST_STATUS_USER_SUBJECT_FORMAT, requestStatusDisplay, request.getProperty().getName());
        String requestDetailUrl = actualSiteUrl + "/account";

        Context context = new Context();
        context.setVariable("userName", (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) ? user.getFirstName() : user.getEmail());
        context.setVariable("requestTypeDisplay", (request instanceof PurchaseRequest) ? "Mua" : "Thuê");
        context.setVariable("property", request.getProperty());
        context.setVariable("requestStatusDisplay", requestStatusDisplay);
        context.setVariable("adminNote", request.getAdminNote());
        context.setVariable("requestDetailsLink", requestDetailUrl);

        String htmlContent;
        try {
            htmlContent = templateEngine.process("email/request-status-update-notification", context);
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý template email request-status-update-notification: " + e.getMessage());
            htmlContent = "<p>Chào " + context.getVariable("userName") + ",</p>" +
                          "<p>Yêu cầu của bạn cho bất động sản <strong>" + request.getProperty().getName() + "</strong> đã được cập nhật trạng thái thành: <strong>" + requestStatusDisplay + "</strong>.</p>" +
                          (request.getAdminNote() != null && !request.getAdminNote().trim().isEmpty() ? "<p>Ghi chú từ quản trị viên: " + request.getAdminNote() + "</p>" : "") +
                          "<p>Bạn có thể xem chi tiết tại: <a href='" + requestDetailUrl + "'>" + requestDetailUrl + "</a></p>";
        }

        sendHtmlEmail(user.getEmail(), subject, htmlContent);
        System.out.println("Đã xử lý gửi email cập nhật trạng thái yêu cầu ID " + request.getId() + " cho user: " + user.getEmail());
    }

    @Async
    @Override
    public void sendRealtorContactEmail(String realtorEmail, String propertyName, RealtorContactDTO contactDTO, String senderIpAddress) {
        if (realtorEmail == null || realtorEmail.trim().isEmpty()) {
            System.err.println("Không thể gửi email: Email người môi giới không hợp lệ.");
            return;
        }

        String subject = "Yêu cầu liên hệ mới cho bất động sản: " + propertyName;

        Context context = new Context();
        context.setVariable("propertyName", propertyName);
        context.setVariable("contactName", contactDTO.getName());
        context.setVariable("contactEmail", contactDTO.getEmail());
        context.setVariable("contactPhone", contactDTO.getPhone());
        context.setVariable("contactMessage", contactDTO.getMessage());
        context.setVariable("senderIpAddress", senderIpAddress);
        context.setVariable("siteBaseUrl", this.siteBaseUrl);

        String htmlContent;
        try {
            // Ensure you have this template: src/main/resources/templates/email/realtor-contact-notification.html
            htmlContent = templateEngine.process("email/realtor-contact-notification", context);
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý template email realtor-contact-notification: " + e.getMessage());
            // Fallback basic HTML email
            htmlContent = "<p>Xin chào,</p>"
                        + "<p>Bạn có một yêu cầu liên hệ mới cho bất động sản: <strong>" + propertyName + "</strong>.</p>"
                        + "<p><strong>Thông tin người liên hệ:</strong></p>"
                        + "<ul>"
                        + "<li>Họ tên: " + contactDTO.getName() + "</li>"
                        + "<li>Email: " + contactDTO.getEmail() + "</li>"
                        + "<li>Điện thoại: " + contactDTO.getPhone() + "</li>"
                        + "</ul>"
                        + "<p><strong>Nội dung tin nhắn:</strong></p>"
                        + "<p>" + contactDTO.getMessage().replace("\n", "<br>") + "</p>"
                        + "<p><em>(Tin nhắn được gửi từ IP: " + senderIpAddress + ")</em></p>";
        }

        sendHtmlEmail(realtorEmail, subject, htmlContent);
        System.out.println("Đã xử lý gửi email liên hệ cho realtor: " + realtorEmail + " về bất động sản: " + propertyName);
    }

    @Async
    @Override
    public void sendContactFormConfirmationEmail(String userEmail, String propertyName, String realtorName, RealtorContactDTO contactDTO) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            System.err.println("Không thể gửi email xác nhận: Email người dùng không hợp lệ.");
            return;
        }

        String subject = "Xác nhận: Yêu cầu liên hệ của bạn cho BĐS '" + propertyName + "' đã được gửi";

        Context context = new Context();
        context.setVariable("userName", contactDTO.getName());
        context.setVariable("propertyName", propertyName);
        context.setVariable("realtorName", realtorName != null ? realtorName : "Người môi giới");
        context.setVariable("contactMessage", contactDTO.getMessage());
        context.setVariable("siteBaseUrl", this.siteBaseUrl);
        context.setVariable("propertyLink", this.siteBaseUrl + "/listings"); // Generic link, adjust if property specific link is needed

        String htmlContent;
        try {
            // Ensure you have this template: src/main/resources/templates/email/user-contact-confirmation.html
            htmlContent = templateEngine.process("email/user-contact-confirmation", context);
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý template email user-contact-confirmation: " + e.getMessage());
            // Fallback basic HTML email
            htmlContent = "<p>Chào " + contactDTO.getName() + ",</p>"
                        + "<p>Chúng tôi đã nhận được yêu cầu liên hệ của bạn cho bất động sản: <strong>" + propertyName + "</strong>.</p>"
                        + "<p>Người môi giới, " + (realtorName != null ? realtorName : "Phòng kinh doanh") + ", sẽ sớm liên hệ với bạn.</p>"
                        + "<p><strong>Nội dung tin nhắn của bạn:</strong></p>"
                        + "<p><em>\"" + contactDTO.getMessage().replace("\n", "<br>") + "\"</em></p>"
                        + "<p>Cảm ơn bạn đã quan tâm!</p>"
                        + "<p>Trân trọng,<br>Đội ngũ " + this.siteBaseUrl.replace("http://","").replace("https://","") + "</p>";
        }

        sendHtmlEmail(userEmail, subject, htmlContent);
        System.out.println("Đã xử lý gửi email xác nhận cho người dùng: " + userEmail + " về bất động sản: " + propertyName);
    }

    @Async
    @Override
    public void sendPropertyDeactivationNotification(Property property, User realtor) {
        if (realtor == null || realtor.getEmail() == null || realtor.getEmail().trim().isEmpty()) {
            System.err.println("Không thể gửi email thông báo: Realtor (" + (realtor != null ? "ID: " + realtor.getId() : "null") + ") hoặc email của realtor không hợp lệ.");
            return;
        }
        if (property == null) {
            System.err.println("Không thể gửi email: Thông tin bất động sản không tồn tại.");
            return;
        }

        String subject = "Bất động sản của bạn đã bị vô hiệu hóa: " + property.getName();

        Context context = new Context();
        context.setVariable("realtorName", (realtor.getFirstName() != null && !realtor.getFirstName().trim().isEmpty()) ? realtor.getFirstName() : realtor.getEmail());
        context.setVariable("propertyName", property.getName());
        context.setVariable("propertyId", property.getId());
        context.setVariable("propertyAddress", property.getAddress());
        context.setVariable("siteBaseUrl", this.siteBaseUrl);
        String accountUrl = this.siteBaseUrl + "/account";
        context.setVariable("accountUrl", accountUrl);

        String htmlContent;
        try {
            // Ensure you have this template: src/main/resources/templates/email/property-deactivation-notification.html
            htmlContent = templateEngine.process("email/property-deactivation-notification", context);
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý template email property-deactivation-notification: " + e.getMessage());
            // Fallback basic HTML email
            htmlContent = "<p>Xin chào " + context.getVariable("realtorName") + ",</p>"
                    + "<p>Bất động sản <strong>" + property.getName() + "</strong> (ID: " + property.getId() + ") của bạn đã bị vô hiệu hóa bởi quản trị viên.</p>"
                    + "<p>Địa chỉ: " + property.getAddress() + "</p>"
                    + "<p>Vui lòng liên hệ với quản trị viên để biết thêm chi tiết.</p>"
                    + "<p>Bạn có thể xem thông tin tài khoản của mình tại: <a href='" + accountUrl + "'>" + accountUrl + "</a></p>"
                    + "<p>Trân trọng,<br/>Hệ thống Real Estate</p>";
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername);
            helper.setTo(realtor.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("Đã gửi email thông báo vô hiệu hóa bất động sản cho môi giới: " + realtor.getEmail());
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi email thông báo vô hiệu hóa BĐS: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendPropertyReactivationNotification(Property property, User realtor) {
        if (realtor == null || realtor.getEmail() == null || realtor.getEmail().trim().isEmpty()) {
            System.err.println("Không thể gửi email thông báo: Realtor (" + (realtor != null ? "ID: " + realtor.getId() : "null") + ") hoặc email của realtor không hợp lệ.");
            return;
        }
        if (property == null) {
            System.err.println("Không thể gửi email: Thông tin bất động sản không tồn tại.");
            return;
        }

        String subject = "Bất động sản của bạn đã được kích hoạt lại: " + property.getName();

        Context context = new Context();
        context.setVariable("realtorName", (realtor.getFirstName() != null && !realtor.getFirstName().trim().isEmpty()) ? realtor.getFirstName() : realtor.getEmail());
        context.setVariable("propertyName", property.getName());
        context.setVariable("propertyId", property.getId());
        context.setVariable("propertyAddress", property.getAddress());
        context.setVariable("siteBaseUrl", this.siteBaseUrl);
        String accountUrl = this.siteBaseUrl + "/account";
        context.setVariable("accountUrl", accountUrl);

        String htmlContent;
        try {
            // Ensure you have this template: src/main/resources/templates/email/property-reactivation-notification.html
            htmlContent = templateEngine.process("email/property-reactivation-notification", context);
        } catch (Exception e) {
            System.err.println("Lỗi khi xử lý template email property-reactivation-notification: " + e.getMessage());
            // Fallback basic HTML email
            htmlContent = "<p>Xin chào " + context.getVariable("realtorName") + ",</p>"
                    + "<p>Bất động sản <strong>" + property.getName() + "</strong> (ID: " + property.getId() + ") của bạn đã được kích hoạt lại bởi quản trị viên.</p>"
                    + "<p>Địa chỉ: " + property.getAddress() + "</p>"
                    + "<p>Bất động sản này đã được chuyển sang trạng thái <strong>AVAILABLE</strong> và hiện đang được hiển thị trên hệ thống.</p>"
                    + "<p>Bạn có thể xem thông tin tài khoản của mình tại: <a href='" + accountUrl + "'>" + accountUrl + "</a></p>"
                    + "<p>Trân trọng,<br/>Hệ thống Real Estate</p>";
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername);
            helper.setTo(realtor.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("Đã gửi email thông báo kích hoạt lại bất động sản cho môi giới: " + realtor.getEmail());
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi email thông báo kích hoạt lại BĐS: " + e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            System.err.println("Không thể gửi email: spring.mail.username chưa được cấu hình.");
            return;
        }
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailUsername);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            System.out.println("Email đã được gửi thành công tới: " + to + " với chủ đề: " + subject);
        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi HTML email đến " + to + " với chủ đề " + subject + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi gửi email đến " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
} 