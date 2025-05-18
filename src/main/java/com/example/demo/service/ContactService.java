package com.example.demo.service;

import com.example.demo.dto.ContactMessageDTO;
import com.example.demo.model.Property;
import com.example.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserService userService;

    public void sendContactMessage(ContactMessageDTO messageDTO) {
        // Get property and realtor information
        Property property = propertyService.getPropertyById(messageDTO.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        User realtor = property.getRealtor();

        // Send email to realtor
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(realtor.getEmail());
        message.setSubject("Tin nhắn liên hệ từ " + messageDTO.getName());
        message.setText(String.format(
            "Bạn có tin nhắn mới từ khách hàng:\n\n" +
            "Tên: %s\n" +
            "Email: %s\n" +
            "Số điện thoại: %s\n\n" +
            "Tin nhắn: %s\n\n" +
            "Bất động sản: %s\n" +
            "Địa chỉ: %s",
            messageDTO.getName(),
            messageDTO.getEmail(),
            messageDTO.getPhone(),
            messageDTO.getMessage(),
            property.getName(),
            property.getAddress()
        ));

        mailSender.send(message);
    }
} 