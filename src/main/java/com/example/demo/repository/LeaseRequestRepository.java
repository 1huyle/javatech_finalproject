package com.example.demo.repository;

import com.example.demo.model.LeaseRequest;
import com.example.demo.model.Property;
import com.example.demo.model.User;
import com.example.demo.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaseRequestRepository extends JpaRepository<LeaseRequest, Long> {
    List<LeaseRequest> findByUser(User user);
    List<LeaseRequest> findByProperty(Property property);
    List<LeaseRequest> findByStatus(RequestStatus status);
    List<LeaseRequest> findByUserAndStatus(User user, RequestStatus status);
    List<LeaseRequest> findByPropertyAndStatus(Property property, RequestStatus status);
    List<LeaseRequest> findByExpectedDateBetween(LocalDate start, LocalDate end);
    
    // Tìm các yêu cầu thuê với giá trong khoảng
    List<LeaseRequest> findByMonthlyRentBetween(Double minRent, Double maxRent);
    
    // Tìm các yêu cầu thuê theo thời hạn
    List<LeaseRequest> findByStartDateAfterAndEndDateBefore(LocalDate startAfter, LocalDate endBefore);
    
    // Kiểm tra xem có yêu cầu thuê nào cho property trong khoảng thời gian
    boolean existsByPropertyAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Property property, List<RequestStatus> statuses, LocalDate endDate, LocalDate startDate);

    boolean existsByUserAndPropertyAndStatusIn(User user, Property property, List<RequestStatus> statuses);
} 