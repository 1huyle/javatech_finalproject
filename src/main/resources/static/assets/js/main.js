// Fetch interceptor to attach JWT token to all requests
(function() {
    const originalFetch = window.fetch;
    
    window.fetch = function(url, options = {}) {
        // Get the token from localStorage
        const token = localStorage.getItem('token');
        
        // If token exists, add it to the Authorization header
        if (token) {
            options.headers = options.headers || {};
            
            // Check if headers is a Headers object, convert if needed
            if (options.headers instanceof Headers) {
                if (!options.headers.has('Authorization')) {
                    options.headers.append('Authorization', `Bearer ${token}`);
                }
            } else {
                if (!options.headers['Authorization']) {
                    options.headers['Authorization'] = `Bearer ${token}`;
                }
            }
        }
        
        // Call the original fetch with updated options
        return originalFetch(url, options);
    };
})();

// Define redirectToAccount function globally so it's available on all pages
window.redirectToAccount = function() {
    const token = localStorage.getItem('token');
    if (token) {
        // If we're already on account page, just refresh
        if (window.location.pathname === '/account') {
            return;
        }
        window.location.href = '/account';
    } else {
        window.location.href = '/login';
    }
};

// Hàm khởi tạo
document.addEventListener('DOMContentLoaded', function() {
    initializeNavigation();
    setupEventListeners();
    checkAuth().then(() => { // Ensure checkAuth completes before other user-dependent setups
        loadUserWishlistPropertyIds().then(() => {
            setupWishlistButtons(); // Setup buttons after wishlist IDs are loaded
        });
    });
    // Removed setupWishlistButtons(); from here as it depends on wishlist IDs
});

// Khởi tạo navigation
function initializeNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            if (this.getAttribute('href').startsWith('#')) {
                e.preventDefault();
                const targetId = this.getAttribute('href').substring(1);
                const targetElement = document.getElementById(targetId);
                if (targetElement) {
                    targetElement.scrollIntoView({ behavior: 'smooth' });
                }
            }
        });
    });
}

// Kiểm tra xác thực và chuyển hướng nếu cần
async function checkAuth() { // Made async to allow awaiting token validation
    // Danh sách các trang yêu cầu xác thực
    const protectedPages = [
        '/account',
        '/payment',
        '/payment-success'
    ];
    
    const currentPath = window.location.pathname;
    
    // Kiểm tra nếu đang ở trang yêu cầu đăng nhập
    if (protectedPages.some(page => currentPath.startsWith(page))) {
        const token = localStorage.getItem('token');
        const user = localStorage.getItem('user');
        
        if (!token || !user) {
            // Nếu không có token hoặc thông tin user, chuyển về trang đăng nhập
            window.location.href = '/login';
            return Promise.reject("No token or user"); // Return a rejected promise
        }
        
        // Kiểm tra token hết hạn
        try {
            const tokenData = JSON.parse(atob(token.split('.')[1]));
            if (tokenData.exp && Date.now() >= tokenData.exp * 1000) {
                // Token đã hết hạn
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                window.location.href = '/login';
                return Promise.reject("Token expired"); // Return a rejected promise
            }
        } catch (e) {
            console.error('Error validating token:', e);
            // Token không hợp lệ
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
            return Promise.reject("Invalid token"); // Return a rejected promise
        }
    }
    return Promise.resolve(); // Resolve if auth checks pass or not on a protected page
}

// Thiết lập các event listeners
function setupEventListeners() {
    // Xử lý form submit - chỉ cho các form có class 'main-form'
    const mainForms = document.querySelectorAll('form.main-form');
    mainForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            handleFormSubmit(this);
        });
    });

    // Xử lý responsive menu
    const menuToggle = document.querySelector('.menu-toggle');
    if (menuToggle) {
        menuToggle.addEventListener('click', function() {
            document.querySelector('.nav-menu').classList.toggle('active');
        });
    }
}

// Xử lý form submit
function handleFormSubmit(form) {
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    
    // Validate form data
    if (validateFormData(data)) {
        // Gửi dữ liệu lên server
        submitFormData(data)
            .then(response => {
                showNotification('Thành công!', 'success');
                form.reset();
            })
            .catch(error => {
                showNotification('Có lỗi xảy ra!', 'error');
            });
    }
}

// Validate form data
function validateFormData(data) {
    // Implement validation logic here
    return true;
}

// Submit form data to server
async function submitFormData(data) {
    // Implement API call here
    return new Promise((resolve) => {
        setTimeout(() => resolve({ success: true }), 1000);
    });
}

// Hiển thị thông báo
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// --- Wishlist Enhancement ---
let userWishlistPropertyIds = []; // Stores IDs of properties in user's wishlist

async function loadUserWishlistPropertyIds() {
    const userString = localStorage.getItem('user');
    if (!userString) {
        // console.log("User not logged in, skipping wishlist load.");
        userWishlistPropertyIds = []; // Ensure it's reset if user logs out
        return;
    }
    try {
        const user = JSON.parse(userString);
        if (user && user.id) {
            const response = await fetch(`/api/wishlists/user/${user.id}/property-ids`);
            if (response.ok) {
                userWishlistPropertyIds = await response.json();
                // console.log("Loaded wishlist property IDs:", userWishlistPropertyIds);
            } else {
                console.error("Failed to load wishlist property IDs:", response.status);
                userWishlistPropertyIds = [];
            }
        } else {
            userWishlistPropertyIds = [];
        }
    } catch (error) {
        console.error('Error loading user wishlist property IDs:', error);
        userWishlistPropertyIds = [];
    }
}

// Thiết lập chức năng Yêu thích 
function setupWishlistButtons() {
    const wishlistButtons = document.querySelectorAll('.favorite-btn');
    
    wishlistButtons.forEach(button => {
        const propertyIdStr = button.getAttribute('data-id');
        if (!propertyIdStr) return;
        
        const propertyId = parseInt(propertyIdStr, 10);
        updateButtonVisualState(propertyId, button); // Set initial state
        
        button.addEventListener('click', function(e) {
            e.preventDefault();
            
            const token = localStorage.getItem('token');
            if (!token) {
                window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname + window.location.search);
                return;
            }
            // const currentPropertyId = this.getAttribute('data-id'); // Already have propertyId
            toggleWishlist(propertyId, this);
        });
    });
}

// Cập nhật giao diện nút dựa trên trạng thái yêu thích
function updateButtonVisualState(propertyId, button) {
    // Ensure propertyId is a number for comparison
    const numericPropertyId = parseInt(propertyId, 10);
    if (userWishlistPropertyIds.includes(numericPropertyId)) {
                button.classList.add('active');
        button.querySelector('i').classList.remove('bi-heart');
        button.querySelector('i').classList.add('bi-heart-fill', 'text-danger');
    } else {
        button.classList.remove('active');
        button.querySelector('i').classList.remove('bi-heart-fill', 'text-danger');
        button.querySelector('i').classList.add('bi-heart');
    }
}

// Kiểm tra trạng thái yêu thích (Sẽ hoàn thiện sau khi có endpoint backend)
// This function is now replaced by updateButtonVisualState and initial load
// async function checkWishlistStatus(propertyId, button) {
//     console.log("Checking wishlist status for property: ", propertyId);
// }

// Toggle trạng thái yêu thích của một bất động sản
async function toggleWishlist(propertyId, button) { // propertyId is already a number if passed from setupWishlistButtons
    const token = localStorage.getItem('token');
    if (!token) {
        showNotification('Vui lòng đăng nhập để sử dụng chức năng này.', 'warning');
        // Optional: redirect to login
        // window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname + window.location.search);
        return;
    }

    const isInWishlist = userWishlistPropertyIds.includes(propertyId);
    const url = isInWishlist ? '/api/wishlists/remove' : '/api/wishlists/add';
    const method = 'POST';

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ propertyId: propertyId }) // userId is handled by backend via Principal
        });

        if (response.ok) {
            // const result = await response.json(); // May not be needed if only status is important
            if (isInWishlist) {
                userWishlistPropertyIds = userWishlistPropertyIds.filter(id => id !== propertyId);
                showNotification('Đã xóa khỏi danh sách yêu thích.', 'success'); // Added notification
            } else {
                userWishlistPropertyIds.push(propertyId);
                showNotification('Đã thêm vào danh sách yêu thích!', 'success'); // Added notification
            }
            updateButtonVisualState(propertyId, button);
        } else {
            const errorData = await response.json().catch(() => ({}));
            console.error("Wishlist toggle failed:", response.status, errorData);
            showNotification(errorData.message || 'Thao tác thất bại. Vui lòng thử lại.', 'error');
        }
    } catch (error) {
        console.error('Error toggling wishlist:', error);
        showNotification('Lỗi kết nối hoặc xử lý yêu cầu.', 'error');
    }
} 