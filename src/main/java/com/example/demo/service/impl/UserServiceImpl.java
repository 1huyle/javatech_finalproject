package com.example.demo.service.impl;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserProfileUpdateDTO;
import com.example.demo.exception.EmailException;
import com.example.demo.model.CustomUserDetail;
import com.example.demo.model.User;
import com.example.demo.model.enums.UserRole;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // Helper method to convert User to UserDTO
    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        // Do NOT set password in DTO for responses
        dto.setRole(user.getRole());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setEnabled(user.isEnabled());
        dto.setBirthDate(user.getBirthDate());
        dto.setAvatarUrl(user.getAvatarUrl());
        // token is usually not needed in general user DTO responses
        return dto;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new CustomUserDetail(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public void InitDatabase() {
        if (userRepository.count() == 0) {
            UserDTO adminDTO = new UserDTO();
            adminDTO.setEmail("admin@example.com");
            adminDTO.setPassword("admin123"); // Password will be hashed in registerUser
            adminDTO.setFirstName("Admin");
            adminDTO.setLastName("User");
            adminDTO.setPhone("1234567890");
            adminDTO.setAddress("Admin Address");
            adminDTO.setRole(UserRole.ADMIN);
            // For InitDatabase, we directly create and save an enabled user
            // or call a simplified registration that enables the user by default.
            // The current registerUser sets enabled=false and sends verification.
            // For simplicity here, we'll adjust registerUser or make a specific admin setup.
            // For now, let's assume registerUser handles it, or we adjust it later if needed for admin init.
            User registeredAdmin = registerUser(adminDTO); //This will send verification email
            // Manually enable admin for now if registerUser doesn't do it by default for ADMIN
            if (registeredAdmin != null && !registeredAdmin.isEnabled()) {
                registeredAdmin.setEnabled(true);
                registeredAdmin.setVerificationToken(null); // Clear token as it's manually verified
                userRepository.save(registeredAdmin);
            }
        }
    }

    @Override
    public User registerUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists - Email này đã được sử dụng trong hệ thống");
        }
        if (userDTO.getPhone() != null && userRepository.existsByPhone(userDTO.getPhone())){
            throw new IllegalArgumentException("Phone number already exists - Số điện thoại này đã được sử dụng trong hệ thống");
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole() != null ? userDTO.getRole() : UserRole.USER);
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhone(userDTO.getPhone());
        user.setAddress(userDTO.getAddress());
        user.setBirthDate(userDTO.getBirthDate());
        // For admin, enable directly. For others, require verification.
        if (userDTO.getRole() == UserRole.ADMIN) {
            user.setEnabled(true);
            user.setVerificationToken(null);
        } else {
            user.setEnabled(false);
            user.setVerificationToken(UUID.randomUUID().toString());
            user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));
        }
        
        user = userRepository.save(user);

        // Send verification email only if not admin and not auto-enabled
        if (userDTO.getRole() != UserRole.ADMIN && !user.isEnabled()) {
            try {
                emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
            } catch (MessagingException e) {
                // Log error, but don't necessarily roll back user creation
                // Or decide on a strategy: e.g., mark user for manual verification
                System.err.println("Failed to send verification email for user: " + user.getEmail() + " - " + e.getMessage());
                // throw new EmailException("Failed to send verification email", e); // Making this non-fatal for registration
            }
        }
        return user;
    }

    @Override
    public UserDTO updateUser(Long id, UserProfileUpdateDTO profileUpdateDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        // Update fields from DTO
        user.setFirstName(profileUpdateDTO.getFirstName());
        user.setLastName(profileUpdateDTO.getLastName());
        user.setAddress(profileUpdateDTO.getAddress());
        if (profileUpdateDTO.getBirthDate() != null) {
            user.setBirthDate(profileUpdateDTO.getBirthDate());
        }

        // Phone update logic with check for uniqueness (excluding current user)
        String newPhone = profileUpdateDTO.getPhone();
        if (newPhone != null && !newPhone.equals(user.getPhone())) {
            if (userRepository.existsByPhoneAndIdNot(newPhone, id)) {
                throw new RuntimeException("Phone number '" + newPhone + "' already exists for another user.");
            }
            user.setPhone(newPhone);
        }
        
        User updatedUser = userRepository.save(user);
        return convertToUserDTO(updatedUser);
    }

    @Override
    public UserDTO updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        user.setAvatarUrl(avatarUrl);
        User updatedUser = userRepository.save(user);
        return convertToUserDTO(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    @Override
    public Page<User> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    @Override
    public void enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid old password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(email, newPassword);
        } catch (MessagingException e) {
            throw new EmailException("Failed to send password reset email", e);
        }
    }

    @Override
    public boolean verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invalid verification token"));

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);

        return true;
    }

    @Override
    public long countNewCustomers() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return userRepository.countByRoleAndCreatedAtAfter(UserRole.USER, sevenDaysAgo);
    }
} 