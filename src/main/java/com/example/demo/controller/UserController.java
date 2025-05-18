package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserProfileUpdateDTO;
import com.example.demo.model.CustomUserDetail;
import com.example.demo.model.User;
import com.example.demo.model.enums.UserRole;
import com.example.demo.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${physical.upload-dir.base}")
    private String physicalUploadDirBase;

    private static final String AVATARS_SUB_DIR = "avatars";

    private UserDTO convertToDto(User user) {
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

    @PostConstruct
    public void InitUser(){
        userService.InitDatabase();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserProfileUpdateDTO profileUpdateDTO, @AuthenticationPrincipal CustomUserDetail currentUser) {
        try {
            Long principalId = currentUser.getId();

            if (!currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) && !id.equals(principalId)) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("You are not authorized to update this user"));
            }
            
            UserDTO updatedUserDTO = userService.updateUser(id, profileUpdateDTO);
            return ResponseEntity.ok(updatedUserDTO);
        } catch (jakarta.validation.ConstraintViolationException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid data: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                                          @AuthenticationPrincipal CustomUserDetail currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("User not authenticated."));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Please select a file to upload."));
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(new ErrorResponse("File size exceeds the limit of 2MB."));
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif"))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Only JPG, PNG, GIF images are allowed."));
        }

        try {
            Path uploadPathDir = Paths.get(physicalUploadDirBase, AVATARS_SUB_DIR);

            if (!Files.exists(uploadPathDir)) {
                Files.createDirectories(uploadPathDir);
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPathDir.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String relativeAvatarPath = "/uploads/" + AVATARS_SUB_DIR + "/" + uniqueFileName;

            UserDTO updatedUser = userService.updateAvatar(currentUser.getId(), relativeAvatarPath);
            
            return ResponseEntity.ok(updatedUser);

        } catch (IOException e) {
            System.err.println("Could not store avatar file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Could not store file. Please try again!"));
        } catch (Exception e) {
            System.err.println("Unexpected error during avatar upload for user " + currentUser.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.ok().build();
    }

    private static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
