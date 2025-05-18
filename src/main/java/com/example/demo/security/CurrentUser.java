package com.example.demo.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.demo.model.CustomUserDetail;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
} 