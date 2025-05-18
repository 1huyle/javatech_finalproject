package com.example.demo.service.impl;

import com.example.demo.model.Lease;
import com.example.demo.model.Property;
import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.model.enums.LeaseStatus;
import com.example.demo.model.enums.PropertyStatus;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.model.enums.TransactionType;
import com.example.demo.repository.LeaseRepository;
import com.example.demo.repository.PropertyRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.LeaseService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LeaseServiceImpl implements LeaseService {

    @Autowired
    private LeaseRepository leaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public Lease createLease(Long tenantId, Long propertyId, LocalDate startDate, LocalDate endDate, double monthlyRent, double deposit) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người thuê"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bất động sản"));

        // Kiểm tra xem bất động sản có sẵn để cho thuê không
        if (!isPropertyAvailableForRent(propertyId, startDate, endDate)) {
            throw new IllegalStateException("Bất động sản không khả dụng cho thuê trong khoảng thời gian này");
        }

        // Tạo đối tượng Lease
        Lease lease = new Lease();
        lease.setTenant(tenant);
        lease.setProperty(property);
        lease.setStartDate(startDate);
        lease.setEndDate(endDate);
        lease.setStatus(LeaseStatus.PENDING);
        lease.setMonthlyRent(monthlyRent);
        lease.setDeposit(deposit);

        // Lưu Lease vào cơ sở dữ liệu
        lease = leaseRepository.save(lease);

        // Tạo Transaction tương ứng
        Transaction transaction = new Transaction();
        transaction.setUser(tenant);
        transaction.setProperty(property);
        transaction.setLease(lease);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setType(TransactionType.RENTAL);
        transaction.setAmount(deposit + monthlyRent); // Tiền cọc + tiền thuê tháng đầu

        // Lưu Transaction vào cơ sở dữ liệu
        transactionRepository.save(transaction);

        // Cập nhật trạng thái bất động sản
        property.setStatus(PropertyStatus.RENTED);
        propertyRepository.save(property);

        return lease;
    }

    @Override
    public Lease updateLeaseStatus(Long id, LeaseStatus status) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hợp đồng thuê"));

        lease.setStatus(status);

        if (status == LeaseStatus.ACTIVE) {
            // Khi hợp đồng được kích hoạt, cập nhật trạng thái của Transaction thành COMPLETED
            Transaction transaction = transactionRepository.findByLease(lease)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giao dịch liên quan đến hợp đồng thuê"));
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        } else if (status == LeaseStatus.TERMINATED || status == LeaseStatus.EXPIRED) {
            // Khi hợp đồng kết thúc, cập nhật trạng thái của bất động sản thành AVAILABLE
            Property property = lease.getProperty();
            property.setStatus(PropertyStatus.AVAILABLE);
            propertyRepository.save(property);
        }

        return leaseRepository.save(lease);
    }

    @Override
    public void deleteLease(Long id) {
        leaseRepository.deleteById(id);
    }

    @Override
    public Optional<Lease> getLeaseById(Long id) {
        return leaseRepository.findById(id);
    }

    @Override
    public List<Lease> getLeasesByTenant(User tenant) {
        return leaseRepository.findByTenant(tenant);
    }

    @Override
    public List<Lease> getLeasesByProperty(Property property) {
        return leaseRepository.findByProperty(property);
    }

    @Override
    public List<Lease> getLeasesByStatus(LeaseStatus status) {
        return leaseRepository.findByStatus(status);
    }

    @Override
    public List<Lease> getActiveLeases() {
        return leaseRepository.findByStatus(LeaseStatus.ACTIVE);
    }

    @Override
    public List<Lease> getExpiredLeases() {
        return leaseRepository.findByEndDateBefore(LocalDate.now());
    }

    @Override
    public void renewLease(Long id, LocalDate newEndDate, double newMonthlyRent) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hợp đồng thuê"));

        // Kiểm tra xem hợp đồng có thể gia hạn không
        if (lease.getStatus() != LeaseStatus.ACTIVE && lease.getStatus() != LeaseStatus.EXPIRED) {
            throw new IllegalStateException("Chỉ có thể gia hạn hợp đồng đang hoạt động hoặc đã hết hạn");
        }

        // Cập nhật thông tin hợp đồng
        lease.setEndDate(newEndDate);
        lease.setMonthlyRent(newMonthlyRent);
        lease.setStatus(LeaseStatus.RENEWED);

        leaseRepository.save(lease);

        // Tạo Transaction mới cho việc gia hạn
        Transaction transaction = new Transaction();
        transaction.setUser(lease.getTenant());
        transaction.setProperty(lease.getProperty());
        transaction.setLease(lease);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setType(TransactionType.RENTAL);
        transaction.setAmount(newMonthlyRent); // Tiền thuê tháng đầu khi gia hạn
        transaction.setCompletedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
    }

    @Override
    public void terminateLease(Long id) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hợp đồng thuê"));

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể chấm dứt hợp đồng đang hoạt động");
        }

        lease.setStatus(LeaseStatus.TERMINATED);
        lease.setTerminatedAt(LocalDateTime.now());
        leaseRepository.save(lease);

        // Cập nhật trạng thái của bất động sản
        Property property = lease.getProperty();
        property.setStatus(PropertyStatus.AVAILABLE);
        propertyRepository.save(property);
    }

    @Override
    public boolean isPropertyAvailableForRent(Long propertyId, LocalDate startDate, LocalDate endDate) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bất động sản"));

        // Kiểm tra xem bất động sản có sẵn sàng để cho thuê không
        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            return false;
        }

        // Kiểm tra xem bất động sản có hợp đồng thuê đang hoạt động không
        return !hasActiveLease(property);
    }

    @Override
    public boolean hasActiveLease(Property property) {
        return leaseRepository.existsByPropertyAndStatus(property, LeaseStatus.ACTIVE);
    }
} 