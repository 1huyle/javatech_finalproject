package com.example.demo.service;

import com.example.demo.model.Property;
import com.example.demo.model.User;
import com.example.demo.model.Wishlist;

import java.util.List;
import java.util.Optional;

public interface WishlistService {
    Wishlist createWishlist(User user);
    Wishlist addPropertyToWishlist(Long wishlistId, Long propertyId);
    Wishlist removePropertyFromWishlist(Long wishlistId, Long propertyId);
    void deleteWishlist(Long id);
    Optional<Wishlist> getWishlistById(Long id);
    Optional<Wishlist> getWishlistByUser(User user);
    List<Long> getPropertyIdsByUserId(Long userId);
    boolean isPropertyInWishlist(Long wishlistId, Long propertyId);
    boolean isWishlistOwner(Long wishlistId, Long userId);
    Wishlist addToUserWishlist(Long userId, Long propertyId);
    Wishlist removeFromUserWishlist(Long userId, Long propertyId);
} 