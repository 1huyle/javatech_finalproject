package com.example.demo.repository;

import com.example.demo.model.Property;
import com.example.demo.model.TransactionRequest;
import com.example.demo.model.User;
import com.example.demo.model.enums.RequestStatus;
import com.example.demo.model.enums.TransactionType;
import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.Query;import org.springframework.data.repository.query.Param;import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface TransactionRequestRepository extends JpaRepository<TransactionRequest, Long> {
        List<TransactionRequest> findByUser(User user);        @Query("SELECT tr FROM TransactionRequest tr WHERE tr.user.id = :userId")    List<TransactionRequest> findByUserId(@Param("userId") Long userId);        List<TransactionRequest> findByProperty(Property property);    List<TransactionRequest> findByStatus(RequestStatus status);    Page<TransactionRequest> findByStatus(RequestStatus status, Pageable pageable);    List<TransactionRequest> findByStatusIn(List<RequestStatus> statuses);
    List<TransactionRequest> findByType(TransactionType type);
    List<TransactionRequest> findByUserAndStatus(User user, RequestStatus status);
    List<TransactionRequest> findByUserAndType(User user, TransactionType type);
    List<TransactionRequest> findByPropertyAndStatus(Property property, RequestStatus status);
    @Query("SELECT tr FROM TransactionRequest tr WHERE tr.property.realtor = :realtor")
    List<TransactionRequest> findByPropertyRealtor(@Param("realtor") User realtor);

    @Query("SELECT tr FROM TransactionRequest tr WHERE tr.property.realtor = :realtor AND tr.status = :status")
    List<TransactionRequest> findByPropertyRealtorAndStatus(@Param("realtor") User realtor, @Param("status") RequestStatus status);

    List<TransactionRequest> findByAssignedRealtor(User assignedRealtor);
    
    List<TransactionRequest> findByAssignedRealtorAndStatus(User assignedRealtor, RequestStatus status);

    List<TransactionRequest> findByExpectedDateBetween(LocalDate start, LocalDate end);
    
    // Các bản ghi đang chờ xử lý (pending) hoặc đang thực hiện (in progress)
    @Query("SELECT tr FROM TransactionRequest tr WHERE tr.status IN :statuses AND tr.property.realtor = :realtor")
    List<TransactionRequest> findByStatusInAndPropertyRealtor(@Param("statuses") List<RequestStatus> statuses, @Param("realtor") User realtor);

    List<TransactionRequest> findByStatusInAndUser(List<RequestStatus> statuses, User user);
    
    // Đếm số yêu cầu theo trạng thái
    long countByStatus(RequestStatus status);
    @Query("SELECT COUNT(tr) FROM TransactionRequest tr WHERE tr.status = :status AND tr.property.realtor = :realtor")
    long countByStatusAndPropertyRealtor(@Param("status") RequestStatus status, @Param("realtor") User realtor);

    long countByStatusAndUser(RequestStatus status, User user);

    // Method to count completed transactions after a certain date
    long countByStatusAndCreatedAtAfter(RequestStatus status, LocalDateTime dateTime);

    // Method to find all completed transactions after a certain date, ordered by creation date descending
    List<TransactionRequest> findAllByStatusAndCreatedAtAfterOrderByCreatedAtDesc(RequestStatus status, LocalDateTime dateTime);

    @Query("SELECT new map(FUNCTION('YEAR', tr.createdAt) as year, FUNCTION('MONTH', tr.createdAt) as month, SUM(p.price) as totalRevenue) " +
           "FROM TransactionRequest tr JOIN tr.property p " +
           "WHERE tr.status = :status AND tr.createdAt >= :startDate " +
           "GROUP BY FUNCTION('YEAR', tr.createdAt), FUNCTION('MONTH', tr.createdAt) " +
           "ORDER BY year ASC, month ASC")
    List<Map<String, Object>> findMonthlyRevenueSince(@Param("status") RequestStatus status, @Param("startDate") LocalDateTime startDate);
} 