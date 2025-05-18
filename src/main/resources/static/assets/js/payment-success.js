document.addEventListener('DOMContentLoaded', function() {
    // Lấy thông tin giao dịch từ URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const transactionId = urlParams.get('transactionId');
    const propertyName = urlParams.get('propertyName');
    const amount = urlParams.get('amount');

    // Hiển thị thông tin giao dịch
    document.getElementById('transactionId').textContent = transactionId || 'N/A';
    document.getElementById('transactionDate').textContent = new Date().toLocaleDateString('vi-VN');
    document.getElementById('propertyName').textContent = propertyName || 'N/A';
    document.getElementById('amount').textContent = formatCurrency(amount) || 'N/A';
});

// Hàm format tiền tệ theo định dạng Việt Nam
function formatCurrency(amount) {
    if (!amount) return 'N/A';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
} 