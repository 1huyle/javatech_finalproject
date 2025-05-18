// Khởi tạo form thanh toán
function initializePayment() {
    const paymentForm = document.getElementById('paymentForm');
    if (!paymentForm) return;

    paymentForm.addEventListener('submit', handlePayment);
    setupPaymentMethodSelection();
}

// Xử lý thanh toán
async function handlePayment(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const paymentData = {
        amount: formData.get('amount'),
        method: formData.get('paymentMethod'),
        cardNumber: formData.get('cardNumber'),
        expiryDate: formData.get('expiryDate'),
        cvv: formData.get('cvv'),
        nameOnCard: formData.get('nameOnCard')
    };

    try {
        // Validate payment data
        if (!validatePaymentData(paymentData)) {
            return;
        }

        // Process payment
        const result = await processPayment(paymentData);
        
        if (result.success) {
            showNotification('Thanh toán thành công!', 'success');
            window.location.href = '/pages/payment-success.html';
        }
    } catch (error) {
        showNotification('Thanh toán thất bại: ' + error.message, 'error');
    }
}

// Validate payment data
function validatePaymentData(data) {
    // Validate card number
    if (!/^\d{16}$/.test(data.cardNumber)) {
        showNotification('Số thẻ không hợp lệ', 'error');
        return false;
    }

    // Validate expiry date
    if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(data.expiryDate)) {
        showNotification('Ngày hết hạn không hợp lệ', 'error');
        return false;
    }

    // Validate CVV
    if (!/^\d{3,4}$/.test(data.cvv)) {
        showNotification('Mã CVV không hợp lệ', 'error');
        return false;
    }

    return true;
}

// Process payment
async function processPayment(paymentData) {
    const res = await fetch('/api/transactions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(paymentData)
    });
    if (!res.ok) throw new Error('Không thể thực hiện giao dịch');
    return await res.json();
}

// Setup payment method selection
function setupPaymentMethodSelection() {
    const methodSelect = document.getElementById('paymentMethod');
    if (!methodSelect) return;

    methodSelect.addEventListener('change', function() {
        const cardFields = document.getElementById('cardFields');
        const bankTransferFields = document.getElementById('bankTransferFields');
        
        if (this.value === 'card') {
            cardFields.style.display = 'block';
            bankTransferFields.style.display = 'none';
        } else if (this.value === 'bankTransfer') {
            cardFields.style.display = 'none';
            bankTransferFields.style.display = 'block';
        }
    });
}

// Format card number input
function formatCardNumber(input) {
    let value = input.value.replace(/\D/g, '');
    value = value.replace(/(\d{4})/g, '$1 ').trim();
    input.value = value;
}

// Format expiry date input
function formatExpiryDate(input) {
    let value = input.value.replace(/\D/g, '');
    if (value.length >= 2) {
        value = value.slice(0,2) + '/' + value.slice(2,4);
    }
    input.value = value;
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    initializePayment();

    // Format inputs
    const cardNumberInput = document.getElementById('cardNumber');
    const expiryDateInput = document.getElementById('expiryDate');

    if (cardNumberInput) {
        cardNumberInput.addEventListener('input', function() {
            formatCardNumber(this);
        });
    }

    if (expiryDateInput) {
        expiryDateInput.addEventListener('input', function() {
            formatExpiryDate(this);
        });
    }

    // Lấy thông tin đơn hàng từ URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const propertyId = urlParams.get('propertyId');
    
    if (propertyId) {
        loadPropertyDetails(propertyId);
    }
});

// Load thông tin chi tiết bất động sản
async function loadPropertyDetails(propertyId) {
    try {
        const property = await fetchPropertyDetails(propertyId);

        // Cập nhật thông tin đơn hàng
        document.getElementById('transactionId').textContent = generateTransactionId();
        document.getElementById('propertyName').textContent = property.name;
        document.getElementById('propertyPrice').textContent = property.price;
        document.getElementById('serviceFee').textContent = property.serviceFee;
        document.getElementById('totalAmount').textContent = calculateTotal(property.price, property.serviceFee);

        // Cập nhật tóm tắt đơn hàng
        document.getElementById('propertyImage').src = property.image;
        document.getElementById('propertyTitle').textContent = property.name;
        document.getElementById('propertyLocation').textContent = property.location;
        document.getElementById('summaryPrice').textContent = property.price;
        document.getElementById('summaryFee').textContent = property.serviceFee;
        document.getElementById('summaryTotal').textContent = calculateTotal(property.price, property.serviceFee);

    } catch (error) {
        console.error('Error loading property details:', error);
        showError('Không thể tải thông tin bất động sản. Vui lòng thử lại sau.');
    }
}

// Thay thế fetchPropertyDetails
async function fetchPropertyDetails(propertyId) {
    const res = await fetch(`/api/properties/${propertyId}`);
    if (!res.ok) throw new Error('Không thể lấy thông tin bất động sản');
    return await res.json();
}

// Xử lý thay đổi phương thức thanh toán
document.getElementById('paymentMethod').addEventListener('change', function(e) {
    const cardFields = document.getElementById('cardFields');
    const bankTransferFields = document.getElementById('bankTransferFields');

    if (e.target.value === 'card') {
        cardFields.style.display = 'block';
        bankTransferFields.style.display = 'none';
    } else if (e.target.value === 'bankTransfer') {
        cardFields.style.display = 'none';
        bankTransferFields.style.display = 'block';
    } else {
        cardFields.style.display = 'none';
        bankTransferFields.style.display = 'none';
    }
});

// Xử lý submit form thanh toán
document.getElementById('paymentForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    if (!validateForm()) {
        return;
    }

    const paymentMethod = document.getElementById('paymentMethod').value;
    const formData = new FormData(e.target);

    try {
        // Hiển thị loading
        showLoading();

        // TODO: Thay thế bằng API call thực tế
        await processPayment({
            method: paymentMethod,
            data: Object.fromEntries(formData)
        });

        // Chuyển hướng đến trang xác nhận
        window.location.href = '/pages/payment-success.html?transactionId=' + document.getElementById('transactionId').textContent;

    } catch (error) {
        console.error('Error processing payment:', error);
        showError('Có lỗi xảy ra khi xử lý thanh toán. Vui lòng thử lại sau.');
    } finally {
        hideLoading();
    }
});

// Validate form
function validateForm() {
    const requiredFields = document.querySelectorAll('[required]');
    let isValid = true;

    requiredFields.forEach(field => {
        if (!field.value.trim()) {
            isValid = false;
            field.classList.add('is-invalid');
        } else {
            field.classList.remove('is-invalid');
        }
    });

    // Validate thẻ tín dụng nếu được chọn
    const paymentMethod = document.getElementById('paymentMethod').value;
    if (paymentMethod === 'card') {
        const cardNumber = document.getElementById('cardNumber').value;
        const expiryDate = document.getElementById('expiryDate').value;
        const cvv = document.getElementById('cvv').value;

        if (!validateCardNumber(cardNumber)) {
            document.getElementById('cardNumber').classList.add('is-invalid');
            isValid = false;
        }
        if (!validateExpiryDate(expiryDate)) {
            document.getElementById('expiryDate').classList.add('is-invalid');
            isValid = false;
        }
        if (!validateCVV(cvv)) {
            document.getElementById('cvv').classList.add('is-invalid');
            isValid = false;
        }
    }

    return isValid;
}

// Validate số thẻ
function validateCardNumber(cardNumber) {
    const regex = /^[0-9]{16}$/;
    return regex.test(cardNumber.replace(/\s/g, ''));
}

// Validate ngày hết hạn
function validateExpiryDate(expiryDate) {
    const regex = /^(0[1-9]|1[0-2])\/([0-9]{2})$/;
    if (!regex.test(expiryDate)) {
        return false;
    }

    const [month, year] = expiryDate.split('/');
    const currentDate = new Date();
    const currentYear = currentDate.getFullYear() % 100;
    const currentMonth = currentDate.getMonth() + 1;

    if (parseInt(year) < currentYear || 
        (parseInt(year) === currentYear && parseInt(month) < currentMonth)) {
        return false;
    }

    return true;
}

// Validate CVV
function validateCVV(cvv) {
    const regex = /^[0-9]{3,4}$/;
    return regex.test(cvv);
}

// Tính tổng tiền
function calculateTotal(price, serviceFee) {
    const priceValue = parseInt(price.replace(/[^0-9]/g, ''));
    const feeValue = parseInt(serviceFee.replace(/[^0-9]/g, ''));
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(priceValue + feeValue);
}

// Tạo mã giao dịch
function generateTransactionId() {
    return 'GD' + Date.now().toString().slice(-8);
}

// Hiển thị loading
function showLoading() {
    // TODO: Implement loading UI
}

// Ẩn loading
function hideLoading() {
    // TODO: Implement loading UI
}

// Hiển thị lỗi
function showError(message) {
    // TODO: Implement error UI
    alert(message);
} 