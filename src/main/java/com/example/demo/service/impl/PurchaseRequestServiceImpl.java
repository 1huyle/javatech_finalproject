package com.example.demo.service.impl;

import com.example.demo.model.*;
import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.RequestStatus;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.repository.PropertyRepository;
import com.example.demo.repository.PurchaseRequestRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PurchaseRequestService;
import com.example.demo.service.TransactionService;
import com.example.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PropertyRepository propertyRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public PurchaseRequest createPurchaseRequest(Long userId, Long propertyId, LocalDate expectedDate,
                                              PaymentMethod paymentMethod, Double purchasePrice,
                                              Double loanAmount, Integer loanTerm, Boolean isNegotiable, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
        
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bất động sản với ID: " + propertyId));
                
        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new IllegalStateException("Bất động sản không khả dụng để mua bán");
        }
        
        PurchaseRequest request = new PurchaseRequest(user, property, expectedDate, paymentMethod, purchasePrice);
        request.setLoanAmount(loanAmount);
        request.setLoanTerm(loanTerm);
        request.setIsNegotiable(isNegotiable);
        request.setNote(note);
        request.setStatus(RequestStatus.PENDING); // Default to PENDING
        
        if (property.getRealtor() != null) {
            request.setAssignedRealtor(property.getRealtor());
        }
        
        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);
        
        System.out.println("Đã tạo purchase request với ID: " + savedRequest.getId() + " và trạng thái: " + savedRequest.getStatus());
        
        if (savedRequest.getAssignedRealtor() != null && emailService != null) {
            try {
                emailService.sendNewRequestNotificationToRealtor(savedRequest, savedRequest.getAssignedRealtor(), null);
            } catch (Exception e) {
                System.err.println("Lỗi khi gửi email thông báo yêu cầu mua mới cho realtor: " + e.getMessage());
            }
        }
        
        // Create a PENDING transaction linked to this PENDING request
        // This transaction will be updated to COMPLETED when the request is COMPLETED
        try {
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setProperty(property);
            transaction.setAmount(purchasePrice);
            transaction.setTransactionDate(LocalDateTime.now()); // Or null until completion
            transaction.setStatus(TransactionStatus.PENDING); // Transaction also starts as PENDING
            transaction.setType(TransactionType.SALE);
            transaction.setRequest(savedRequest);
            transaction.setPaymentMethod(paymentMethod);
            transaction.calculateCommission();
            
            Transaction savedTransaction = transactionRepository.save(transaction);
            System.out.println("Đã tạo transaction với ID: " + savedTransaction.getId() + " cho PurchaseRequest ID: " + savedRequest.getId());
            
            savedRequest.setTransaction(savedTransaction);
            purchaseRequestRepository.save(savedRequest); // Save again to link transaction
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo transaction PENDING cho purchase request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return savedRequest;
    }

    @Override
    public boolean hasActiveRequest(Long userId, Long propertyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found with id: " + userId));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Property not found with id: " + propertyId));

        List<RequestStatus> activeStatuses = List.of(RequestStatus.PENDING);
        return purchaseRequestRepository.existsByUserAndPropertyAndStatusIn(user, property, activeStatuses);
    }

    @Override
    public Optional<PurchaseRequest> getRequestById(Long id) {
        return purchaseRequestRepository.findById(id);
    }

    @Override
    public List<PurchaseRequest> getRequestsByUser(User user) {
        return purchaseRequestRepository.findByUser(user);
    }

    @Override
    public List<PurchaseRequest> getRequestsByProperty(Property property) {
        return purchaseRequestRepository.findByProperty(property);
    }

    @Override
    public List<PurchaseRequest> getPendingRequests() {
        return purchaseRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    @Override
    @Transactional
    public PurchaseRequest updateRequestStatus(Long id, RequestStatus status) {
        PurchaseRequest request = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu mua với ID: " + id));
        
        // Prevent updating to PENDING if it's already COMPLETED or CANCELLED
        if ((request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) && status == RequestStatus.PENDING) {
            throw new IllegalStateException("Không thể chuyển trạng thái yêu cầu đã hoàn thành hoặc đã hủy về đang chờ xử lý.");
        }

        request.setStatus(status);
        
        if (status == RequestStatus.COMPLETED) {
            request.setCompletedAt(LocalDateTime.now());
            // The completePurchase method called by Controller handles property status and transaction finalization.
            // If called directly, ensure those are handled.
            // For now, this method primarily updates the status field.
            // The main completion logic is in completePurchase.
        } else if (status == RequestStatus.CANCELLED) {
            request.setCancelledAt(LocalDateTime.now());
            // If there's an associated transaction, it might also need to be cancelled.
            Transaction transaction = request.getTransaction();
            if (transaction != null && transaction.getStatus() == TransactionStatus.PENDING) {
                transaction.setStatus(TransactionStatus.CANCELLED); // Or a more specific status
                transactionRepository.save(transaction);
            }
        }
        
        return purchaseRequestRepository.save(request);
    }

    @Override
    public PurchaseRequest negotiatePrice(Long id, Double newPrice, String note) {
        PurchaseRequest request = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu mua với ID: " + id));
        
        if (request.getStatus() != RequestStatus.PENDING) {
             throw new IllegalStateException("Chỉ có thể thương lượng giá cho các yêu cầu đang chờ xử lý.");
        }

        if (!request.getIsNegotiable()) {
            throw new RuntimeException("Yêu cầu mua này không cho phép thương lượng giá");
        }
        
        request.setPurchasePrice(newPrice);
        // Update transaction amount if negotiation happens after transaction creation
        Transaction transaction = request.getTransaction();
        if (transaction != null) {
            transaction.setAmount(newPrice);
            transaction.calculateCommission(); // Recalculate commission
            transactionRepository.save(transaction);
        }

        request.setAdminNote(note); // Append or set admin note
        return purchaseRequestRepository.save(request);
    }

    @Override
    public PurchaseRequest assignRealtor(Long id, Long realtorId) {
        PurchaseRequest request = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu mua với ID: " + id));
        
        User realtor = userRepository.findById(realtorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môi giới với ID: " + realtorId));
        
        request.setAssignedRealtor(realtor);
        return purchaseRequestRepository.save(request);
    }

    @Override
    public void deletePurchaseRequest(Long id) {
        // Consider implications: what happens to linked transaction?
        // For now, just deletes the request.
        purchaseRequestRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void completePurchase(Long requestId) {
        PurchaseRequest request = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu mua với ID: " + requestId));

        if (request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalStateException("Không thể hoàn thành yêu cầu đã bị hủy.");
        }

        // Idempotency: if already COMPLETED, do nothing or ensure consistency.
        if (request.getStatus() == RequestStatus.COMPLETED) {
            System.out.println("Info: Yêu cầu mua ID: " + requestId + " đã ở trạng thái HOÀN THÀNH.");
            // Optionally, ensure property and transaction status are also correct.
            // For now, we assume if request is COMPLETED, underlying objects are also COMPLETED.
            return; 
        }
        
        request.setStatus(RequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        
        Property property = request.getProperty();
        property.setStatus(PropertyStatus.SOLD); // Mark property as SOLD
        property.setOwner(request.getUser()); // Change owner
        propertyRepository.save(property);
        
        Transaction transaction = request.getTransaction();
        if (transaction != null) {
            transaction.setStatus(TransactionStatus.COMPLETED); // Mark transaction as COMPLETED
            transaction.setTransactionDate(LocalDateTime.now()); // Confirm transaction date
            // Potentially update other transaction details if needed
            transactionRepository.save(transaction);
        } else {
            // This case should ideally not happen if a transaction is created with the request
            System.err.println("WARN: Không tìm thấy giao dịch liên kết với yêu cầu mua ID: " + requestId + " khi hoàn thành.");
            // Optionally create a new COMPLETED transaction here, but it indicates an inconsistency earlier.
        }
        
        purchaseRequestRepository.save(request);
        System.out.println("Đã hoàn thành yêu cầu mua ID: " + requestId + ". Bất động sản ID: " + property.getId() + " đã được bán.");
    }
} 