package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserProfileUpdateDTO;
import com.example.demo.model.User;
import com.example.demo.model.enums.UserRole;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService extends UserDetailsService {
    User registerUser(UserDTO userDTO);
    UserDTO updateUser(Long id, UserProfileUpdateDTO profileUpdateDTO);
    UserDTO updateAvatar(Long userId, String avatarUrl);
    void deleteUser(Long id);
    Optional<User> getUserById(Long id);
    Optional<User> getUserByEmail(String email);
    User findByEmail(String email);
    List<User> getAllUsers();
    Page<User> getAllUsers(Pageable pageable);
    List<User> getUsersByRole(UserRole role);
    Page<User> getUsersByRole(UserRole role, Pageable pageable);
    void enableUser(Long id);
    void disableUser(Long id);
    void changePassword(Long id, String oldPassword, String newPassword);
    void resetPassword(String email);
    boolean verifyUser(String token);
    void InitDatabase();
    long countNewCustomers();
} 