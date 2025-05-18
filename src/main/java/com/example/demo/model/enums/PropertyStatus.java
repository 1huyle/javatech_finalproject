package com.example.demo.model.enums;

public enum PropertyStatus {
    AVAILABLE,      // Sẵn có để bán hoặc cho thuê
    PENDING,        // Đang chờ xử lý giao dịch (ví dụ: chờ thanh toán, chờ xác nhận)
    SOLD,           // Đã bán
    RENTED,         // Đã cho thuê
    UNAVAILABLE,    // Không sẵn có (ví dụ: đang sửa chữa, chủ nhà không muốn giao dịch nữa tạm thời)
    DRAFT,          // Bản nháp, chưa công khai
    EXPIRED,        // Hết hạn đăng tin
    INACTIVE         // Realtor tự đánh dấu là đã xóa 

    // EM có tạo các enum dư nhưng chưa dùng tới nhằm mở rộng dự án sau này
} 