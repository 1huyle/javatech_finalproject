package com.example.demo.service.impl;

import com.example.demo.model.*;
import com.example.demo.model.enums.LeaseStatus;
import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.RequestStatus;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.repository.*;
import com.example.demo.service.LeaseRequestService;
import com.example.demo.service.TransactionService;
import com.example.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class LeaseRequestServiceImpl implements LeaseRequestService {

    @Autowired
    private LeaseRequestRepository leaseRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PropertyRepository propertyRepository;
    
    @Autowired
    private LeaseRepository leaseRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public LeaseRequest createLeaseRequest(Long userId, Long propertyId, LocalDate expectedDate,
                                        LocalDate startDate, LocalDate endDate,
                                        Double monthlyRent, Double deposit, String note, PaymentMethod paymentMethod) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
        
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bất động sản với ID: " + propertyId));
                
        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new IllegalStateException("Bất động sản không khả dụng để thuê");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }
        
        LeaseRequest request = new LeaseRequest(user, property, expectedDate, startDate, endDate, monthlyRent, deposit);
        request.setNote(note);
        request.setStatus(RequestStatus.PENDING); // Default to PENDING
        request.setPaymentMethod(paymentMethod);
        
        if (property.getRealtor() != null) {
            request.setAssignedRealtor(property.getRealtor());
        }
        
        LeaseRequest savedRequest = leaseRequestRepository.save(request);
        System.out.println("Đã tạo lease request với ID: " + savedRequest.getId() + " và trạng thái: " + savedRequest.getStatus());

        if (savedRequest.getAssignedRealtor() != null && emailService != null) {
            try {
                emailService.sendNewRequestNotificationToRealtor(savedRequest, savedRequest.getAssignedRealtor(), null);
            } catch (Exception e) {
                System.err.println("Lỗi khi gửi email thông báo yêu cầu thuê mới cho realtor: " + e.getMessage());
            }
        }
        
        // Unlike purchase, we don't auto-create a Transaction/Lease here.
        // That happens in completeLease.

        return savedRequest;
    }

    @Override
    @Transactional
    public LeaseRequest createLeaseRequest(Long userId, Long propertyId, LocalDate expectedDate,
                                        LocalDate startDate, LocalDate endDate,
                                        Double monthlyRent, Double deposit, String note) {
        // Gọi phương thức có PaymentMethod với giá trị null
        return createLeaseRequest(userId, propertyId, expectedDate, startDate, endDate, 
                                monthlyRent, deposit, note, null);
    }

    @Override
    public boolean hasActiveRequest(Long userId, Long propertyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found with id: " + userId));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Property not found with id: " + propertyId));

        List<RequestStatus> activeStatuses = List.of(RequestStatus.PENDING);
        return leaseRequestRepository.existsByUserAndPropertyAndStatusIn(user, property, activeStatuses);
    }

    @Override
    public Optional<LeaseRequest> getRequestById(Long id) {
        return leaseRequestRepository.findById(id);
    }

    @Override
    public List<LeaseRequest> getRequestsByUser(User user) {
        return leaseRequestRepository.findByUser(user);
    }

    @Override
    public List<LeaseRequest> getRequestsByProperty(Property property) {
        return leaseRequestRepository.findByProperty(property);
    }

    @Override
    public List<LeaseRequest> getPendingRequests() {
        return leaseRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    @Override
    @Transactional
    public LeaseRequest updateRequestStatus(Long id, RequestStatus status) {
        LeaseRequest request = leaseRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu thuê với ID: " + id));
        
        if ((request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) && status == RequestStatus.PENDING) {
            throw new IllegalStateException("Không thể chuyển trạng thái yêu cầu đã hoàn thành hoặc đã hủy về đang chờ xử lý.");
        }

        request.setStatus(status);

        if (status == RequestStatus.COMPLETED) {
            request.setCompletedAt(LocalDateTime.now());
            // The completeLease method called by Controller handles Lease creation and Transaction.
        } else if (status == RequestStatus.CANCELLED) {
            request.setCancelledAt(LocalDateTime.now());
            // If a transaction was somehow linked to a PENDING lease request (not current flow),
            // it might need cancellation here. For now, assuming transactions only form on COMPLETED.
            Transaction transaction = request.getTransaction(); // Assuming LeaseRequest might have a Transaction field
            if (transaction != null && transaction.getStatus() == TransactionStatus.PENDING) {
                transaction.setStatus(TransactionStatus.CANCELLED);
                transactionRepository.save(transaction);
            }
        }
        
        return leaseRequestRepository.save(request);
    }

    @Override
    public LeaseRequest assignRealtor(Long id, Long realtorId) {
        LeaseRequest request = leaseRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu thuê với ID: " + id));
        
        User realtor = userRepository.findById(realtorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môi giới với ID: " + realtorId));
        
        request.setAssignedRealtor(realtor);
        return leaseRequestRepository.save(request);
    }

    @Override
    public void deleteLeaseRequest(Long id) {
        leaseRequestRepository.deleteById(id);
    }

    @Override
    public boolean isPropertyAvailableForRent(Long propertyId, LocalDate startDate, LocalDate endDate) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bất động sản với ID: " + propertyId));
        
        List<RequestStatus> activeStatuses = List.of(RequestStatus.PENDING);
        
        boolean hasConflictingRequest = leaseRequestRepository
                .existsByPropertyAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        property, activeStatuses, endDate, startDate);
        
        if (hasConflictingRequest) {
            System.out.println("DEBUG: Property ID " + propertyId + " has conflicting PENDING lease requests for period " + startDate + " to " + endDate);
            return false;
        }
        
        // Check for active leases that might conflict
        List<Lease> activeLeases = leaseRepository.findByPropertyAndStatus(property, LeaseStatus.ACTIVE);
        for (Lease activeLease : activeLeases) {
            // Check for overlap: (StartA <= EndB) and (EndA >= StartB)
            // Requested period: [startDate, endDate]
            // Active lease period: [activeLease.getStartDate(), activeLease.getEndDate()]
            if (startDate.isBefore(activeLease.getEndDate()) && endDate.isAfter(activeLease.getStartDate())) {
                System.out.println("DEBUG: Property ID " + propertyId + " has an overlapping ACTIVE lease (ID: " + activeLease.getId() + ") for period " + startDate + " to " + endDate);
                return false; // Found an overlapping active lease
            }
        }
        
        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            System.out.println("DEBUG: Property ID " + propertyId + " is not AVAILABLE. Current status: " + property.getStatus());
            return false;
        }
        System.out.println("DEBUG: Property ID " + propertyId + " IS available for rent for period " + startDate + " to " + endDate);
        return true;
    }

    @Override
    @Transactional
    public void completeLease(Long requestId) {
        LeaseRequest request = leaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu thuê với ID: " + requestId));
        
        if (request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalStateException("Không thể hoàn thành yêu cầu thuê đã bị hủy.");
        }

        if (request.getStatus() == RequestStatus.COMPLETED) {
            System.out.println("Info: Yêu cầu thuê ID: " + requestId + " đã ở trạng thái HOÀN THÀNH.");
            return; 
        }
        
        request.setStatus(RequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        
        // Create Lease contract
        Lease lease = new Lease(
                request.getUser(),
                request.getProperty(),
                request.getStartDate(),
                request.getEndDate(),
                LeaseStatus.ACTIVE, // Lease becomes active
                request.getMonthlyRent(),
                request.getDeposit()
        );
        lease.setLeaseRequest(request);
        Lease savedLease = leaseRepository.save(lease);
        
        // Create and complete Transaction for the lease (e.g., deposit + first month)
        // This part might need more business logic for what amount constitutes the transaction
        Transaction transaction = request.getTransaction(); // Check if one was pre-associated (not in current flow)
        if (transaction == null) {
            transaction = new Transaction();
            transaction.setUser(request.getUser());
            transaction.setProperty(request.getProperty());
            // Example: Transaction amount could be deposit or deposit + first rent
            transaction.setAmount(request.getDeposit()); // Or request.getDeposit() + request.getMonthlyRent()
            transaction.setType(TransactionType.RENTAL);
            transaction.setRequest(request);
            // Payment method for lease might not be set at request time, could be set here or be a generic one.
        }
        transaction.setLease(savedLease); // Link transaction to the actual lease
        transaction.setStatus(TransactionStatus.COMPLETED); // Transaction for lease is completed
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.calculateCommission(); // If commission applies to leases
        transactionRepository.save(transaction);

        request.setTransaction(transaction); // Link back to request if it makes sense
        leaseRequestRepository.save(request);
        
        Property property = request.getProperty();
        property.setStatus(PropertyStatus.RENTED); // Mark property as RENTED
        propertyRepository.save(property);

        System.out.println("Đã hoàn thành yêu cầu thuê ID: " + requestId + ". Hợp đồng thuê ID: " + savedLease.getId() + " đã được tạo. Bất động sản ID: " + property.getId() + " đã được cho thuê.");
    }
} 