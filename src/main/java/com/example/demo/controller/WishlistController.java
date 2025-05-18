package com.example.demo.controller;

import com.example.demo.model.Property;
import com.example.demo.model.User;
import com.example.demo.model.Wishlist;
import com.example.demo.dto.PropertySummaryDTO;
import com.example.demo.dto.WishlistActionRequest;
import com.example.demo.model.CustomUserDetail;
import com.example.demo.service.PropertyService;
import com.example.demo.service.UserService;
import com.example.demo.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserService userService;

    @Autowired
    private PropertyService propertyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Wishlist> createWishlist(@RequestParam Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(wishlistService.createWishlist(user));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Wishlist> addPropertyToCurrentUserWishlist(
            @RequestBody WishlistActionRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        // Sử dụng userId từ currentUser để tăng bảo mật, bỏ qua request.getUserId()
        Wishlist updatedWishlist = wishlistService.addToUserWishlist(currentUser.getId(), request.getPropertyId());
        return ResponseEntity.ok(updatedWishlist);
    }

    @PostMapping("/remove")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Wishlist> removePropertyFromCurrentUserWishlist(
            @RequestBody WishlistActionRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        // Sử dụng userId từ currentUser để tăng bảo mật
        Wishlist updatedWishlist = wishlistService.removeFromUserWishlist(currentUser.getId(), request.getPropertyId());
        return ResponseEntity.ok(updatedWishlist);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWishlist(@PathVariable Long id) {
        wishlistService.deleteWishlist(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @wishlistService.isWishlistOwner(#id, authentication.principal.id)")
    public ResponseEntity<Wishlist> getWishlistById(@PathVariable Long id) {
        return wishlistService.getWishlistById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<PropertySummaryDTO>> getWishlistByUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        return wishlistService.getWishlistByUser(user)
                .map(wishlist -> {
                    List<PropertySummaryDTO> propertySummaries = wishlist.getProperties().stream()
                        .map(property -> new PropertySummaryDTO(
                            property.getId(),
                            property.getName(), 
                            property.getAddress(),
                            (property.getImages() != null && !property.getImages().isEmpty())
                                ? property.getImages().get(0).getImageUrl() 
                                : "/assets/images/property-placeholder.jpg",
                            property.getPropertyType() != null ? property.getPropertyType().getName() : "N/A",
                            property.getPrice(),
                            property.getStatus()
                        ))
                        .collect(Collectors.toList());
                    return ResponseEntity.ok(propertySummaries);
                })
                .orElse(ResponseEntity.ok(Collections.emptyList()));
    }

    @GetMapping("/user/{userId}/property-ids")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<Long>> getWishlistPropertyIdsByUser(@PathVariable Long userId) {
        // We don't need to fetch the User entity here if WishlistService can handle it by ID
        List<Long> propertyIds = wishlistService.getPropertyIdsByUserId(userId);
        return ResponseEntity.ok(propertyIds);
    }

    @GetMapping("/{wishlistId}/contains/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Boolean> isPropertyInWishlist(
            @PathVariable Long wishlistId,
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(wishlistService.isPropertyInWishlist(wishlistId, propertyId));
    }
} 