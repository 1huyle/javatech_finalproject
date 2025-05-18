// auth.js
// Quản lý đăng nhập và đăng ký cho ứng dụng bất động sản

document.addEventListener('DOMContentLoaded', function() {
    // Tìm form đăng nhập và đăng ký
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const loginError = document.getElementById('loginError');
    const errorAlert = document.getElementById('errorAlert');

    // Kiểm tra xem có tham số redirect trong URL không
    const urlParams = new URLSearchParams(window.location.search);
    const redirectUrl = urlParams.get('redirect');

    // Gắn sự kiện submit cho form đăng nhập
    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            loginError.classList.add('d-none');

            const formData = {
                email: document.getElementById('email').value,
                password: document.getElementById('password').value
            };

            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });

                if (response.ok) {
                    const data = await response.json();
                    // Lưu token và user vào localStorage
                    localStorage.setItem('token', data.token);
                    localStorage.setItem('user', JSON.stringify(data));

                    // Xác định URL chuyển hướng dựa trên tham số redirect hoặc vai trò
                    let targetUrl = '/index'; // Mặc định chuyển đến trang chủ thay vì account
                    
                    // Kiểm tra xem có URL đã lưu trong localStorage không (từ trang chi tiết bất động sản)
                    const savedRedirect = localStorage.getItem('redirectAfterLogin');
                    if (savedRedirect) {
                        targetUrl = savedRedirect;
                        localStorage.removeItem('redirectAfterLogin'); // Xóa sau khi sử dụng
                    } else if (redirectUrl) {
                        // Kiểm tra xem redirectUrl có phải là URL login không
                        if (redirectUrl.includes('/login')) {
                            console.log('Phát hiện redirect đến trang login, chuyển về trang chủ để tránh lặp vô tận');
                            targetUrl = '/index';
                        } else {
                            // Nếu có tham số redirect, ưu tiên chuyển hướng đến đó
                            targetUrl = redirectUrl;
                        }
                    } else if (data.role === 'ADMIN') {
                        targetUrl = '/admin/dashboard'; // ADMIN chuyển đến dashboard
                    } else if (data.role === 'REALTOR') { 
                        targetUrl = '/account'; // REALTOR chuyển đến trang account
                    } else if (data.role === 'USER') {
                        targetUrl = '/index'; // USER chuyển đến trang chủ
                    }
                    
                    console.log('Redirecting to:', targetUrl);
                    
                    // Chuyển hướng đến trang đích sau khi đăng nhập thành công
                    window.location.href = targetUrl;
                } else {
                    const error = await response.json();
                    loginError.textContent = error.message || 'Đăng nhập thất bại';
                    loginError.classList.remove('d-none');
                }
            } catch (error) {
                loginError.textContent = 'Có lỗi xảy ra, vui lòng thử lại';
                loginError.classList.remove('d-none');
            }
        });
    } else {
        console.warn('Login form not found');
    }

    // Gắn sự kiện submit cho form đăng ký
    if (registerForm) {
        registerForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            errorAlert.classList.add('d-none');

            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            if (password !== confirmPassword) {
                errorAlert.textContent = 'Mật khẩu xác nhận không khớp';
                errorAlert.classList.remove('d-none');
                return;
            }

            const formData = {
                email: document.getElementById('email').value,
                password: password,
                firstName: document.getElementById('firstName').value,
                lastName: document.getElementById('lastName').value,
                phone: document.getElementById('phone').value,
                address: document.getElementById('address').value,
                role: document.getElementById('isRealtor').checked ? 'REALTOR' : 'USER'
            };

            try {
                const response = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(formData)
                });

                if (response.ok) {
                    alert('Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.');
                    window.location.href = '/login';
                } else {
                    const errorData = await response.json();
                    // Hiển thị thông báo lỗi chính xác hơn
                    if (errorData.message) {
                        // Nếu có thông báo lỗi cụ thể từ server
                        if (errorData.message.includes("Email already exists")) {
                            errorAlert.textContent = 'Email này đã được sử dụng, vui lòng dùng email khác.';
                        } else if (errorData.message.includes("Phone number already exists")) {
                            errorAlert.textContent = 'Số điện thoại này đã được sử dụng, vui lòng dùng số khác.';
                        } else {
                            errorAlert.textContent = errorData.message;
                        }
                    } else {
                        errorAlert.textContent = 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.';
                    }
                    errorAlert.classList.remove('d-none');
                }
            } catch (error) {
                console.error('Lỗi đăng ký:', error);
                errorAlert.textContent = 'Có lỗi xảy ra, vui lòng thử lại sau';
                errorAlert.classList.remove('d-none');
            }
        });
    } else {
        console.warn('Register form not found');
    }

    // Đăng xuất
    async function logout() {
        try {
            await fetch('/api/auth/logout', {
                method: 'POST'
            });
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
        } catch (error) {
            console.error('Logout error:', error);
        }
    }

    // Kiểm tra trạng thái đăng nhập (nếu cần)
    function checkAuthStatus() {
        // Có thể thêm logic kiểm tra token hoặc session
    }
});