// Xử lý trang chi tiết bất động sản

document.addEventListener('DOMContentLoaded', function() {
    checkPurchaseSuccess();
    setupEventHandlers();
});

// Kiểm tra và hiển thị thông báo khi đăng ký mua thành công
function checkPurchaseSuccess() {
    const urlParams = new URLSearchParams(window.location.search);
    const purchaseSuccess = urlParams.get('success');
    
    if (purchaseSuccess === 'purchase_submitted') {
        showSuccessToast('Yêu cầu mua bất động sản đã được gửi thành công! Chúng tôi sẽ liên hệ với bạn trong thời gian sớm nhất.');
        
        // Xóa tham số query để tránh hiển thị lại thông báo khi refresh
        const url = new URL(window.location.href);
        url.searchParams.delete('success');
        window.history.replaceState({}, document.title, url);
    }
}

// Thiết lập các xử lý sự kiện
function setupEventHandlers() {
    // Xử lý nút "Mua ngay"
    const buyNowBtn = document.querySelector('a[href*="/purchase"]');
    if (buyNowBtn) {
        buyNowBtn.addEventListener('click', function(e) {
            // Kiểm tra xem người dùng đã đăng nhập chưa
            const token = localStorage.getItem('token');
            if (!token) {
                e.preventDefault();
                // Nếu chưa đăng nhập, lưu URL hiện tại để redirect sau khi đăng nhập
                localStorage.setItem('redirectAfterLogin', window.location.href);
                window.location.href = '/login';
            }
        });
    }
    
    // Xử lý form liên hệ
    const contactForm = document.getElementById('contactForm');
    if (contactForm) {
        contactForm.addEventListener('submit', function(e) {
            e.preventDefault();
            handleContactFormSubmit(this);
        });
    }
}

// Xử lý gửi form liên hệ
function handleContactFormSubmit(form) {
    const submitButton = document.getElementById('contactSubmit');
    const submitText = document.getElementById('contactSubmitText');
    const loadingSpinner = document.getElementById('contactLoadingSpinner');
    
    // Hiển thị trạng thái đang tải
    submitButton.disabled = true;
    submitText.classList.add('d-none');
    loadingSpinner.classList.remove('d-none');
    
    // Mô phỏng gửi form (thực tế sẽ gửi API call đến server)
    setTimeout(() => {
        // Ẩn trạng thái đang tải
        submitButton.disabled = false;
        submitText.classList.remove('d-none');
        loadingSpinner.classList.add('d-none');
        
        // Hiển thị thông báo thành công
        showSuccessToast('Tin nhắn của bạn đã được gửi thành công!');
        
        // Reset form
        form.reset();
    }, 1500);
}

// Hiển thị thông báo thành công dạng toast
function showSuccessToast(message) {
    // Kiểm tra xem đã có phần tử toast-container chưa
    let toastContainer = document.querySelector('.toast-container');
    
    if (!toastContainer) {
        // Tạo mới phần tử container nếu chưa có
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }
    
    // Tạo phần tử toast
    const toastId = 'toast-' + Date.now();
    const toastHTML = `
        <div id="${toastId}" class="toast align-items-center text-white bg-success" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="bi bi-check-circle-fill me-2"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;
    
    // Thêm toast vào container
    toastContainer.innerHTML += toastHTML;
    
    // Khởi tạo và hiển thị toast
    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
        autohide: true,
        delay: 5000
    });
    
    toast.show();
    
    // Xóa toast khỏi DOM sau khi ẩn
    toastElement.addEventListener('hidden.bs.toast', function() {
        this.remove();
    });
} 