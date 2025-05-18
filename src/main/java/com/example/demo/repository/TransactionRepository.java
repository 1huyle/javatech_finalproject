package com.example.demo.repository;

import com.example.demo.model.Lease;
import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.model.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Adding import for TransactionRequest
import com.example.demo.model.TransactionRequest;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByUserAndStatus(User user, TransactionStatus status);
    Optional<Transaction> findByLease(Lease lease);
    List<Transaction> findAllByOrderByCreatedAtDesc();
    
    // Method to find a transaction by its associated request
    Optional<Transaction> findByRequest(TransactionRequest request);
} 