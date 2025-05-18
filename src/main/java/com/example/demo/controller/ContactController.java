package com.example.demo.controller;

import com.example.demo.dto.ContactMessageDTO;
import com.example.demo.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public ResponseEntity<Void> sendContactMessage(@Valid @RequestBody ContactMessageDTO messageDTO) {
        contactService.sendContactMessage(messageDTO);
        return ResponseEntity.ok().build();
    }
} 