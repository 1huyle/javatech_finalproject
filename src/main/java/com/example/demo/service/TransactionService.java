package com.example.demo.service;

import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.model.enums.TransactionStatus;

import java.util.List;
import java.util.Optional;

public interface TransactionService {
    Transaction createTransaction(Long userId, Long propertyId, double amount);
    Transaction createTransactionFromRequest(Long requestId);
    Transaction updateTransactionStatus(Long id, TransactionStatus status);
    void deleteTransaction(Long id);
    Optional<Transaction> getTransactionById(Long id);
    List<Transaction> getTransactionsByUser(User user);
    List<Transaction> getTransactionsByStatus(TransactionStatus status);
    List<Transaction> getTransactionsByUserAndStatus(User user, TransactionStatus status);
    void completeTransaction(Long id);
    void cancelTransaction(Long id);
    boolean isUserInvolved(Long transactionId, Long userId);
    List<Transaction> getRecentTransactions(int limit);
} 