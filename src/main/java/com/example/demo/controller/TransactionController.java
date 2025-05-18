package com.example.demo.controller;

import com.example.demo.model.Transaction;
import com.example.demo.model.User;
import com.example.demo.model.enums.TransactionStatus;
import com.example.demo.service.TransactionService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller xử lý các API cho giao dịch bất động sản.
 * Cung cấp các endpoint để:
 * - Tạo và cập nhật giao dịch
 * - Truy vấn thông tin giao dịch
 * - Quản lý trạng thái giao dịch (hoàn thành/hủy)
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    //--------------------------------------------------
    // THAO TÁC CQQL (CREATE, UPDATE, DELETE)
    //--------------------------------------------------
    
    /**
     * Tạo giao dịch mới
     * @param userId ID người dùng tham gia giao dịch
     * @param propertyId ID bất động sản liên quan
     * @param amount Số tiền giao dịch
     * @return Thông tin giao dịch đã tạo
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Transaction> createTransaction(
            @RequestParam Long userId,
            @RequestParam Long propertyId,
            @RequestParam double amount) {
        return ResponseEntity.ok(transactionService.createTransaction(userId, propertyId, amount));
    }

    /**
     * Cập nhật trạng thái giao dịch
     * @param id ID giao dịch cần cập nhật
     * @param status Trạng thái mới
     * @return Thông tin giao dịch đã cập nhật
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Transaction> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionService.updateTransactionStatus(id, status));
    }

    /**
     * Xóa giao dịch
     * @param id ID giao dịch cần xóa
     * @return Thông báo thành công
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }

    //--------------------------------------------------
    // TRUY VẤN THÔNG TIN GIAO DỊCH
    //--------------------------------------------------
    
    /**
     * Lấy thông tin giao dịch theo ID
     * @param id ID giao dịch cần lấy thông tin
     * @return Thông tin giao dịch
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @transactionService.isUserInvolved(#id, authentication.principal.id)")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy danh sách giao dịch theo người dùng
     * @param userId ID người dùng
     * @return Danh sách giao dịch của người dùng
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<Transaction>> getTransactionsByUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(transactionService.getTransactionsByUser(user));
    }

    /**
     * Lấy danh sách giao dịch theo trạng thái
     * @param status Trạng thái giao dịch cần lọc
     * @return Danh sách giao dịch theo trạng thái
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Transaction>> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        return ResponseEntity.ok(transactionService.getTransactionsByStatus(status));
    }

    /**
     * Lấy danh sách giao dịch theo người dùng và trạng thái
     * @param userId ID người dùng
     * @param status Trạng thái giao dịch
     * @return Danh sách giao dịch phù hợp với điều kiện lọc
     */
    @GetMapping("/user/{userId}/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<Transaction>> getTransactionsByUserAndStatus(
            @PathVariable Long userId,
            @PathVariable TransactionStatus status) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(transactionService.getTransactionsByUserAndStatus(user, status));
    }

    //--------------------------------------------------
    // QUẢN LÝ TRẠNG THÁI GIAO DỊCH
    //--------------------------------------------------
    
    /**
     * Hoàn thành giao dịch
     * @param id ID giao dịch cần hoàn thành
     * @return Thông báo thành công
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> completeTransaction(@PathVariable Long id) {
        transactionService.completeTransaction(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Hủy giao dịch
     * @param id ID giao dịch cần hủy
     * @return Thông báo thành công
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelTransaction(@PathVariable Long id) {
        transactionService.cancelTransaction(id);
        return ResponseEntity.ok().build();
    }
} 