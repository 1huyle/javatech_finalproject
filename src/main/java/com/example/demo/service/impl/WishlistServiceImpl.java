package com.example.demo.service.impl;

import com.example.demo.model.Property;
import com.example.demo.model.User;
import com.example.demo.model.Wishlist;
import com.example.demo.repository.PropertyRepository;
import com.example.demo.repository.WishlistRepository;
import com.example.demo.service.WishlistService;
import com.example.demo.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserService userService;

    @Override
    public Wishlist createWishlist(User user) {
        if (wishlistRepository.existsByUser(user)) {
            throw new IllegalStateException("User already has a wishlist");
        }

        Wishlist wishlist = new Wishlist(user);
        return wishlistRepository.save(wishlist);
    }

    @Override
    public Wishlist addPropertyToWishlist(Long wishlistId, Long propertyId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new EntityNotFoundException("Wishlist not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));

        if (!wishlist.containsProperty(property)) {
            wishlist.getProperties().add(property);
            return wishlistRepository.save(wishlist);
        }

        return wishlist;
    }

    @Override
    public Wishlist removePropertyFromWishlist(Long wishlistId, Long propertyId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new EntityNotFoundException("Wishlist not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));

        wishlist.getProperties().remove(property);
        return wishlistRepository.save(wishlist);
    }

    @Override
    public void deleteWishlist(Long id) {
        if (!wishlistRepository.existsById(id)) {
            throw new EntityNotFoundException("Wishlist not found");
        }
        wishlistRepository.deleteById(id);
    }

    @Override
    public Optional<Wishlist> getWishlistById(Long id) {
        return wishlistRepository.findById(id);
    }

    @Override
    public Optional<Wishlist> getWishlistByUser(User user) {
        return wishlistRepository.findByUser(user);
    }

    @Override
    public List<Long> getPropertyIdsByUserId(Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Optional<Wishlist> wishlistOpt = wishlistRepository.findByUser(user);
        if (wishlistOpt.isPresent()) {
            Wishlist wishlist = wishlistOpt.get();
            if (wishlist.getProperties() != null) {
                return wishlist.getProperties().stream()
                               .map(Property::getId)
                               .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    @Override
    public boolean isPropertyInWishlist(Long wishlistId, Long propertyId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new EntityNotFoundException("Wishlist not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));

        return wishlist.containsProperty(property);
    }

    @Override
    public boolean isWishlistOwner(Long wishlistId, Long userId) {
        return wishlistRepository.findById(wishlistId)
                .map(wishlist -> wishlist.getUser().getId().equals(userId))
                .orElse(false);
    }

    @Override
    @Transactional
    public Wishlist addToUserWishlist(Long userId, Long propertyId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        
        Wishlist wishlist = wishlistRepository.findByUser(user)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist(user);
                    return wishlistRepository.save(newWishlist);
                });

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with ID: " + propertyId));

        if (!wishlist.getProperties().contains(property)) {
            wishlist.getProperties().add(property);
        }
        return wishlistRepository.save(wishlist);
    }

    @Override
    public Wishlist removeFromUserWishlist(Long userId, Long propertyId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Wishlist wishlist = wishlistRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Wishlist not found for user: " + userId));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found with ID: " + propertyId));

        wishlist.getProperties().remove(property);
        return wishlistRepository.save(wishlist);
    }
} 