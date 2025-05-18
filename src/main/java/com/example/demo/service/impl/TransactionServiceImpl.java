package com.example.demo.service.impl;

import com.example.demo.model.Property;
import com.example.demo.model.Transaction;
import com.example.demo.model.TransactionRequest;
import com.example.demo.model.User;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.RequestStatus;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.repository.PropertyRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.TransactionRequestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;
    
    @Autowired
    private TransactionRequestRepository transactionRequestRepository;

    @Override
    public Transaction createTransaction(Long userId, Long propertyId, double amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bất động sản"));

        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new IllegalStateException("Bất động sản không khả dụng để giao dịch");
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setProperty(property);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setType(TransactionType.SALE);

        // Tính toán hoa hồng tự động
        transaction.calculateCommission();

        return transactionRepository.save(transaction);
    }
    
    @Override
    public Transaction createTransactionFromRequest(Long requestId) {
        TransactionRequest request = transactionRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu giao dịch với ID: " + requestId));
                
        // This method is called when a request is being processed towards completion,
        // typically when it's PENDING and a transaction needs to be formally recorded.
        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.COMPLETED) {
             // Allow calling if request is already COMPLETED for idempotency if a transaction was missed.
            throw new IllegalStateException("Yêu cầu giao dịch phải ở trạng thái PENDING hoặc COMPLETED để tạo/lấy giao dịch. Hiện tại: " + request.getStatus());
        }
        
        // If a transaction already exists for this request, return it (or handle as an update if needed)
        if (request.getTransaction() != null) {
            System.out.println("INFO: Transaction already exists for request ID: " + requestId + ". Returning existing transaction ID: " + request.getTransaction().getId());
            return request.getTransaction();
        }

        Property property = request.getProperty();
        // Property availability should be checked by the calling service before this point typically.
        // However, a re-check can be a safeguard.
        if (property.getStatus() != PropertyStatus.AVAILABLE && request.getStatus() == RequestStatus.PENDING) {
             // If request is already COMPLETED, property might be SOLD/RENTED, which is fine.
            throw new IllegalStateException("Bất động sản không còn khả dụng để giao dịch (Yêu cầu ID: " + requestId + ")");
        }
        
        // Sử dụng constructor mới từ TransactionRequest
        Transaction transaction = new Transaction(request);
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Link transaction back to request
        request.setTransaction(savedTransaction);
        // The request's status (e.g. to COMPLETED) should be handled by the calling service method (completePurchase/completeLease)
        transactionRequestRepository.save(request);
        
        System.out.println("INFO: Created new transaction ID: " + savedTransaction.getId() + " for request ID: " + requestId);
        return savedTransaction;
    }

    @Override
    public Transaction updateTransactionStatus(Long id, TransactionStatus status) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giao dịch"));

        // Prevent re-opening a finalized transaction without specific logic
        if ((transaction.getStatus() == TransactionStatus.COMPLETED || transaction.getStatus() == TransactionStatus.CANCELLED) && 
            (status == TransactionStatus.PENDING)) {
            throw new IllegalStateException("Không thể đặt lại giao dịch đã hoàn thành hoặc đã hủy về trạng thái đang chờ xử lý.");
        }

        transaction.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        
        if (status == TransactionStatus.COMPLETED) {
            transaction.setCompletedAt(now);
            Property property = transaction.getProperty();
            if (transaction.getType() == TransactionType.SALE) {
                property.setStatus(PropertyStatus.SOLD);
                property.setOwner(transaction.getUser());
            } else if (transaction.getType() == TransactionType.RENTAL) {
                property.setStatus(PropertyStatus.RENTED);
            }
            propertyRepository.save(property);
            
            if (transaction.getRequest() != null && transaction.getRequest().getStatus() != RequestStatus.COMPLETED) {
                transaction.getRequest().setStatus(RequestStatus.COMPLETED);
                transaction.getRequest().setCompletedAt(now); // Ensure request completed time is also set
                transactionRequestRepository.save(transaction.getRequest());
            }
        } else if (status == TransactionStatus.CANCELLED) {
            // If transaction is cancelled, request should also be cancelled
            if (transaction.getRequest() != null && transaction.getRequest().getStatus() != RequestStatus.CANCELLED) {
                transaction.getRequest().setStatus(RequestStatus.CANCELLED);
                transaction.getRequest().setCancelledAt(now); // Ensure request cancelled time is also set
                transactionRequestRepository.save(transaction.getRequest());
            }
        } else if (status == TransactionStatus.PENDING) {
            transaction.setCompletedAt(null); // Clear completion date if moved back to pending
        }

        return transactionRepository.save(transaction);
    }

    @Override
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy giao dịch");
        }
        // Consider implications for linked request if a transaction is deleted.
        transactionRepository.deleteById(id);
    }

    @Override
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    @Override
    public List<Transaction> getTransactionsByUser(User user) {
        return transactionRepository.findByUser(user);
    }

    @Override
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    @Override
    public List<Transaction> getTransactionsByUserAndStatus(User user, TransactionStatus status) {
        return transactionRepository.findByUserAndStatus(user, status);
    }

    @Override
    public void completeTransaction(Long id) {
        updateTransactionStatus(id, TransactionStatus.COMPLETED);
    }

    @Override
    public void cancelTransaction(Long id) {
        updateTransactionStatus(id, TransactionStatus.CANCELLED);
    }

    @Override
    public boolean isUserInvolved(Long transactionId, Long userId) {
        return transactionRepository.findById(transactionId)
                .map(transaction -> 
                    transaction.getUser().getId().equals(userId) ||
                    (transaction.getProperty() != null && transaction.getProperty().getOwner() != null && transaction.getProperty().getOwner().getId().equals(userId)) ||
                    (transaction.getProperty() != null && transaction.getProperty().getRealtor() != null && 
                     transaction.getProperty().getRealtor().getId().equals(userId)))
                .orElse(false);
    }
    
    @Override
    public List<Transaction> getRecentTransactions(int limit) {
        // Ensure repository method exists or use Pageable for limiting results
        List<Transaction> allTransactions = transactionRepository.findAllByOrderByCreatedAtDesc(); // Reverted to CreatedAt
        if (allTransactions.isEmpty()) {
            return List.of(); // Return an empty list if no transactions found
        }
        return allTransactions.subList(0, Math.min(limit, allTransactions.size()));
    }
} 