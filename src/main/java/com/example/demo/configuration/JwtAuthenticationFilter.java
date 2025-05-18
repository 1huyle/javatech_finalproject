package com.example.demo.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.service.JwtService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Kiểm tra URI nếu là trang đăng ký hoặc đăng nhập, cho phép đi qua mà không check auth
            String requestURI = request.getRequestURI();
            if (requestURI.equals("/api/auth/register") || requestURI.equals("/register") ||
                requestURI.equals("/api/auth/login") || requestURI.equals("/login")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Ghi log cookie và Authorization header
            logRequestDetails(request);
            
            // Kiểm tra Authorization header
            final String authHeader = request.getHeader("Authorization");
            String jwt = null;
            boolean isFromCookie = false;
            
            // Nếu có header, lấy token từ header
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                log.debug("JWT from Authorization header: Found (length: {})", jwt.length());
            } else {
                // Nếu không có header, kiểm tra cookie
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                            log.debug("Cookie found: {} = {}", cookie.getName(), 
                                cookie.getValue().substring(0, Math.min(10, cookie.getValue().length())) + "...");
                            if ("jwt".equals(cookie.getName())) {
                                jwt = cookie.getValue();
                                isFromCookie = true;
                                log.debug("JWT from cookie: Found (length: {})", jwt.length());
                                break;
                            }
                        }
                    }
                } else {
                    log.debug("No cookies found in request");
                }
            }
            
            // Nếu không tìm thấy token, cho phép request tiếp tục
            if (jwt == null) {
                log.debug("No JWT token found, continuing filter chain");
                filterChain.doFilter(request, response);
                return;
            }
            
            try {
                final String userEmail = jwtService.extractUsername(jwt);
                
                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    try {
                        UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                        if (jwtService.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                            authToken.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request)
                            );
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            log.debug("User authenticated: {}", userEmail);
                        } else {
                            // Token không hợp lệ, xóa cookie nếu token đến từ cookie
                            if (isFromCookie) {
                                clearJwtCookie(response);
                                log.warn("Invalid token, cookie cleared");
                            }
                        }
                    } catch (UsernameNotFoundException e) {
                        // Người dùng không tồn tại, xóa cookie nếu token đến từ cookie
                        if (isFromCookie) {
                            clearJwtCookie(response);
                            log.warn("User not found: {}, cookie cleared", userEmail);
                        }
                        log.error("User not found: {}", userEmail);
                    }
                }
            } catch (Exception e) {
                // Lỗi khi xử lý token, xóa cookie nếu token đến từ cookie
                if (isFromCookie) {
                    clearJwtCookie(response);
                    log.warn("Error processing token, cookie cleared: {}", e.getMessage());
                }
                log.error("JWT processing error: {}", e.getMessage());
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            clearJwtCookie(response);
            log.error("JWT Authentication failed: {}", e.getMessage());
            
            // Ghi log chi tiết hơn
            if (e instanceof UsernameNotFoundException) {
                log.error("User not found in database");
            } else {
                log.error("Authentication error", e);
            }
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại");
        }
    }
    
    private void clearJwtCookie(HttpServletResponse response) {
        Cookie deleteCookie = new Cookie("jwt", null);
        deleteCookie.setMaxAge(0);
        deleteCookie.setPath("/");
        response.addCookie(deleteCookie);
    }
    
    private void logRequestDetails(HttpServletRequest request) {
        log.debug("Request URI: {}", request.getRequestURI());
        log.debug("Authorization header: {}", request.getHeader("Authorization") != null ? "Present" : "Not present");
        
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            log.debug("Cookies count: {}", cookies.length);
            for (Cookie cookie : cookies) {
                if (cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    String valuePreview = cookie.getValue().length() > 10 ? 
                        cookie.getValue().substring(0, Math.min(10, cookie.getValue().length())) + "..." : cookie.getValue();
                    log.debug("Cookie: {} = {} (path: {}, httpOnly: {}, secure: {})", 
                        cookie.getName(), valuePreview, cookie.getPath(), 
                        cookie.isHttpOnly(), cookie.getSecure());
                } else {
                    log.debug("Cookie: {} with null or empty value", cookie.getName());
                }
            }
        } else {
            log.debug("No cookies in request");
        }
    }
} 