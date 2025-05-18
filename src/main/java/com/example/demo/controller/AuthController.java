package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.model.User;
import com.example.demo.model.CustomUserDetail;
import com.example.demo.model.enums.UserRole;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.UserService;
import com.example.demo.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserDTO userDTO = new UserDTO();
            userDTO.setEmail(request.getEmail());
            userDTO.setPassword(request.getPassword());
            userDTO.setFirstName(request.getFirstName());
            userDTO.setLastName(request.getLastName());
            userDTO.setPhone(request.getPhone());
            userDTO.setAddress(request.getAddress());
            userDTO.setRole(request.getRole() != null ? request.getRole() : UserRole.USER);
            if (request.getBirthDate() != null) {
                userDTO.setBirthDate(request.getBirthDate());
            }

            User user = userService.registerUser(userDTO);
            return ResponseEntity.ok(convertToDTO(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userService.findByEmail(request.getEmail());
        UserDTO userDTO = convertToDTO(user);
        
        // Tạo JWT token
        String token = jwtService.generateToken(new CustomUserDetail(user));
        userDTO.setToken(token);
        
        // Lưu token vào cookie
        Cookie cookie = new Cookie("jwt", token);
        cookie.setMaxAge(24 * 60 * 60); // 24 giờ
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // Nếu sử dụng HTTPS, thêm dòng sau
        // cookie.setSecure(true);
        response.addCookie(cookie);
        
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Void> validateToken() {
        // Không cần làm gì ở đây, vì JwtAuthenticationFilter sẽ tự động xác thực token 
        // và trả về lỗi nếu token không hợp lệ
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Xóa cookie khi đăng xuất
        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            User user = userService.findByEmail(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(404).build();
            }
            return ResponseEntity.ok(convertToDTO(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> checkAuthStatus(@CurrentUser CustomUserDetail userDetail) {
        if (userDetail != null) {
            UserDTO userDTO = convertToDTO(userService.findByEmail(userDetail.getUsername()));
            return ResponseEntity.ok(userDTO);
        }
        return ResponseEntity.status(401).body("Unauthorized");
    }

    @GetMapping("/verify")
    public void verifyUser(@RequestParam String token, HttpServletResponse response) throws java.io.IOException {
        try {
            userService.verifyUser(token);
            response.sendRedirect("/login?verified=true");
        } catch (Exception e) {
            response.sendRedirect("/login?error=verification_failed");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestParam String email) {
        userService.resetPassword(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated. Please log in.");
        }

        try {
            userService.changePassword(currentUser.getId(), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok("Password changed successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while changing password.");
        }
    }

    /**
     * Endpoint đơn giản để kiểm tra xác thực chỉ trả về text
     */
    @GetMapping("/check")
    public ResponseEntity<String> checkAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Chưa đăng nhập");
        }
        
        return ResponseEntity.ok("Đã đăng nhập với username: " + authentication.getName());
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setEnabled(user.isEnabled());
        dto.setBirthDate(user.getBirthDate());
        dto.setAvatarUrl(user.getAvatarUrl());
        return dto;
    }
} 