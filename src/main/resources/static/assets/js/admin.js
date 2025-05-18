// assets/js/admin.js

// Helper function to get JWT token from localStorage
function getToken() {
    return localStorage.getItem('token');
}

// Helper function for making authenticated fetch requests
async function fetchWithAuth(url, options = {}) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {}), // Preserve existing headers
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers: headers,
    });

    if (response.status === 401 || response.status === 403) {
        // Token might be invalid or expired, redirect to login
        console.error('Authentication error, redirecting to login.');
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
        // Throw an error to stop further processing in the calling function
        throw new Error('Authentication required');
    }

    return response; // Return the full response object
}

// Cập nhật trạng thái active cho sidebar
function updateSidebarActiveState() {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('#sidebar .nav-link');

    navLinks.forEach(link => {
        const linkPath = link.getAttribute('href');
        if (currentPath.includes(linkPath)) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

// Khởi tạo sự kiện
function initializeEvents() {
    // Xuất báo cáo
    const exportBtn = document.querySelector('.btn-outline-secondary[aria-label="Xuất báo cáo"]');
    if (exportBtn) {
        exportBtn.addEventListener('click', exportReport);
    }

    // In trang
    const printBtn = document.querySelector('.btn-outline-secondary[aria-label="In"]');
    if (printBtn) {
        printBtn.addEventListener('click', printDashboard);
    }

    // In chi tiết đơn hàng
    const printOrderBtn = document.querySelector('.btn-primary[aria-label="In chi tiết đơn hàng"]');
    if (printOrderBtn) {
        printOrderBtn.addEventListener('click', () => {
            showNotification('Đang in chi tiết đơn hàng...', 'info');
            window.print();
        });
    }

    // Cập nhật dữ liệu
    const refreshBtn = document.querySelector('.btn-outline-secondary[aria-label="Cập nhật dữ liệu"]');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', refreshData);
    }

    // Lọc thời gian qua dropdown
    document.querySelectorAll('.dropdown-menu a').forEach(item => {
        item.addEventListener('click', e => {
            e.preventDefault();
            const filterText = e.target.textContent;
            const filterValue = e.target.getAttribute('onclick')?.match(/'([^']+)'/)?.[1] || filterText.toLowerCase();
            document.querySelector('.dropdown-toggle').innerHTML = `<i class="bi bi-calendar3"></i> ${filterText}`;
            filterData(filterValue);
        });
    });

    // Lọc giao dịch theo loại
    document.querySelectorAll('.dropdown-menu a[onclick*="filterTransactions"]').forEach(item => {
        item.addEventListener('click', e => {
            e.preventDefault();
            const type = item.getAttribute('onclick').match(/'([^']+)'/)[1];
            filterTransactions(type);
        });
    });

    // Toggle sidebar trên mobile
    const sidebarToggle = document.querySelector('.btn-dark.d-md-none');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', () => {
            document.getElementById('sidebar').classList.toggle('show');
        });
    }

    // Khởi tạo Bootstrap tooltips
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    [...tooltipTriggerList].forEach(trigger => new bootstrap.Tooltip(trigger));

    // Gắn sự kiện cho các nút giao dịch
    bindTransactionButtons();
}

// Gắn sự kiện cho các nút trong bảng giao dịch
function bindTransactionButtons() {
    const viewButtons = document.querySelectorAll('.btn-info[data-bs-toggle="modal"]');
    viewButtons.forEach(button => {
        button.removeEventListener('click', handleViewTransaction);
        button.addEventListener('click', handleViewTransaction);
    });

    const editButtons = document.querySelectorAll('.btn-primary[aria-label="Chỉnh sửa giao dịch"]');
    editButtons.forEach(button => {
        button.removeEventListener('click', handleEditTransaction);
        button.addEventListener('click', handleEditTransaction);
    });

    const deleteButtons = document.querySelectorAll('.btn-danger[aria-label="Xóa giao dịch"]');
    deleteButtons.forEach(button => {
        button.removeEventListener('click', handleDeleteTransaction);
        button.addEventListener('click', handleDeleteTransaction);
    });
}

function handleViewTransaction() {
    const row = this.closest('tr');
    const transactionId = row.cells[0].textContent;
    viewTransactionDetails(transactionId);
}

function handleEditTransaction() {
    const row = this.closest('tr');
    const transactionId = row.cells[0].textContent;
    editTransaction(transactionId);
}

function handleDeleteTransaction() {
    const row = this.closest('tr');
    const transactionId = row.cells[0].textContent;
    if (confirm(`Xác nhận xóa giao dịch #${transactionId}?`)) {
        showNotification(`Đã xóa giao dịch #${transactionId}`, 'success');
        row.remove();
    }
}

// Tải dữ liệu thống kê
async function loadDashboardStats(period = 'week') {
    try {
        const stats = await fetchDashboardStats(period);
        updateDashboardUI(stats);
    } catch (error) {
        console.error('Lỗi khi tải dữ liệu thống kê:', error);
        showNotification('Không thể tải dữ liệu thống kê', 'error');
    }
}

// Thay thế fetchDashboardStats
async function fetchDashboardStats() {
    // Lấy tổng số lượng user, property, sales, rentals, ...
    // Cần backend cung cấp API tổng hợp hoặc gọi nhiều API nhỏ
    try {
        const [usersRes, propertiesRes /*, transactionsRes */] = await Promise.all([
            fetchWithAuth('/api/users'),
            fetchWithAuth('/api/properties')
            // fetchWithAuth('/api/transactions?recent=5') // Example: API for recent transactions
        ]);
        if (!usersRes.ok || !propertiesRes.ok /* || !transactionsRes.ok */) throw new Error('Không thể lấy dữ liệu dashboard');
        const users = await usersRes.json();
        const properties = await propertiesRes.json();
        // const transactions = await transactionsRes.json();
        
        return {
            totalUsers: users.length, // Assuming API returns an array
            totalProperties: properties.length, // Assuming API returns an array
            recentTransactions: [], // transactions || []
            recentProperties: properties.slice(0, 5)
        };
    } catch (error) {
        if (error.message !== 'Authentication required') { // Avoid double logging if redirected
            console.error('Error fetching dashboard stats:', error);
        }
        throw error; // Re-throw to be caught by caller
    }
}

// Cập nhật giao diện với dữ liệu thống kê
function updateDashboardUI(stats) {
    const propertyStat = document.querySelector('.bg-primary h2');
    const salesStat = document.querySelector('.bg-success h2');
    const rentalsStat = document.querySelector('.bg-info h2');
    const usersStat = document.querySelector('.bg-warning h2');

    if (propertyStat) propertyStat.textContent = stats.totalProperties;
    if (salesStat) salesStat.textContent = stats.totalSales;
    if (rentalsStat) rentalsStat.textContent = stats.totalRentals;
    if (usersStat) usersStat.textContent = stats.totalUsers;

    updateTransactionsTable(stats.recentTransactions);
    updateRecentProperties(stats.recentProperties);
}

// Cập nhật bảng giao dịch
function updateTransactionsTable(transactions) {
    const tableBody = document.querySelector('table tbody');
    if (!tableBody) return;

    tableBody.innerHTML = '';
    transactions.forEach(transaction => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${transaction.id}</td>
            <td>${transaction.property}</td>
            <td>${transaction.buyer}</td>
            <td>${transaction.type}</td>
            <td>${transaction.price}</td>
            <td><span class="badge bg-${getStatusBadgeClass(transaction.status)}">${transaction.status}</span></td>
            <td>${transaction.date}</td>
            <td>
                <div class="btn-group" role="group" aria-label="Thao tác với giao dịch">
                    <button class="btn btn-sm btn-info" data-bs-toggle="modal" data-bs-target="#orderDetailsModal" aria-label="Xem chi tiết giao dịch">
                        <i class="bi bi-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-primary" aria-label="Chỉnh sửa giao dịch">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" aria-label="Xóa giao dịch">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </td>
        `;
        tableBody.appendChild(row);
    });

    bindTransactionButtons();
}

// Cập nhật danh sách bất động sản mới
function updateRecentProperties(properties) {
    const container = document.querySelector('.row:last-child');
    if (!container) return;

    container.innerHTML = '';
    properties.forEach(property => {
        const col = document.createElement('div');
        col.className = 'col-md-4 mb-4';
        col.innerHTML = `
            <div class="card">
                <img src="${property.image}" class="card-img-top" alt="${property.name}">
                <div class="card-body">
                    <h5 class="card-title">${property.name}</h5>
                    <p class="card-text">${property.price} | ${property.area}${property.bedrooms ? ` | ${property.bedrooms} phòng ngủ` : ''}</p>
                    <div class="d-flex justify-content-between align-items-center">
                        <a href="/pages/admin/properties.html?id=${property.id}" class="btn btn-primary btn-sm">Xem chi tiết</a>
                        <span class="badge bg-${getStatusBadgeClass(property.status)}">${property.status}</span>
                    </div>
                </div>
            </div>
        `;
        container.appendChild(col);
    });
}

// Lấy class cho badge trạng thái
function getStatusBadgeClass(status) {
    switch (status) {
        case 'Hoàn thành':
        case 'Đã đăng':
            return 'success';
        case 'Đang xử lý':
        case 'Đang chờ':
        case 'Chờ duyệt':
            return 'warning';
        case 'Đã hủy':
            return 'danger';
        default:
            return 'secondary';
    }
}

// Xem chi tiết giao dịch
async function viewTransactionDetails(transactionId) {
    try {
        const transaction = await fetchTransactionDetails(transactionId);
        const modal = document.getElementById('orderDetailsModal');
        const modalTitle = modal.querySelector('.modal-title');
        const modalBody = modal.querySelector('.modal-body');

        modalTitle.textContent = `Chi tiết đơn hàng #${transactionId}`;
        modalBody.innerHTML = `
            <div class="row mb-4">
                <div class="col-md-6">
                    <h6 class="mb-3">Thông tin khách hàng</h6>
                    <p><strong>Họ và tên:</strong> ${transaction.buyer}</p>
                    <p><strong>Email:</strong> ${transaction.email || 'N/A'}</p>
                    <p><strong>Số điện thoại:</strong> ${transaction.phone || 'N/A'}</p>
                    <p><strong>Địa chỉ:</strong> ${transaction.address || 'N/A'}</p>
                </div>
                <div class="col-md-6">
                    <h6 class="mb-3">Thông tin đơn hàng</h6>
                    <p><strong>Mã đơn hàng:</strong> #${transaction.id}</p>
                    <p><strong>Ngày tạo:</strong> ${transaction.date}</p>
                    <p><strong>Trạng thái:</strong> <span class="badge bg-${getStatusBadgeClass(transaction.status)}">${transaction.status}</span></p>
                    <p><strong>Phương thức thanh toán:</strong> ${transaction.paymentMethod || 'N/A'}</p>
                </div>
            </div>
            <h6 class="mb-3">Thông tin bất động sản</h6>
            <div class="card mb-4">
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-3">
                            <img src="${transaction.propertyImage || '/assets/images/property1.jpg'}" alt="${transaction.property}" class="img-fluid rounded">
                        </div>
                        <div class="col-md-9">
                            <h5>${transaction.property}</h5>
                            <p><strong>Mã bất động sản:</strong> #${transaction.propertyId}</p>
                            <p><strong>Địa chỉ:</strong> ${transaction.propertyAddress || 'N/A'}</p>
                            <p><strong>Diện tích:</strong> ${transaction.propertyArea || 'N/A'}</p>
                            <p><strong>Giá:</strong> ${transaction.price}</p>
                        </div>
                    </div>
                </div>
            </div>
        `;
    } catch (error) {
        console.error('Lỗi khi tải chi tiết giao dịch:', error);
        showNotification('Không thể tải chi tiết giao dịch', 'error');
    }
}

// Thay thế fetchTransactionDetails
async function fetchTransactionDetails(transactionId) {
    try {
        const res = await fetchWithAuth(`/api/transactions/${transactionId}`);
        if (!res.ok) throw new Error('Không thể lấy chi tiết giao dịch');
        return await res.json();
    } catch (error) {
        if (error.message !== 'Authentication required') {
            console.error('Error fetching transaction details:', error);
        }
        throw error;
    }
}

// Chỉnh sửa giao dịch
function editTransaction(transactionId) {
    const modal = document.getElementById('editOrderModal');
    if (!modal) {
        showNotification('Chức năng chỉnh sửa chưa được hỗ trợ', 'warning');
        return;
    }

    // Giả lập dữ liệu để điền vào form
    const transaction = {
        id: transactionId,
        propertyId: 'P001',
        buyerId: 'U001',
        price: '2500000000',
        status: 'completed',
        date: '2024-05-15',
        paymentMethod: 'bank_transfer'
    };

    document.getElementById('editPropertyId').value = transaction.propertyId;
    document.getElementById('editBuyerId').value = transaction.buyerId;
    document.getElementById('editOrderPrice').value = transaction.price;
    document.getElementById('editOrderStatus').value = transaction.status;
    document.getElementById('editOrderDate').value = transaction.date;
    document.getElementById('editPaymentMethod').value = transaction.paymentMethod;

    new bootstrap.Modal(modal).show();
}

// Lọc dữ liệu theo thời gian
async function filterData(period) {
    try {
        await loadDashboardStats(period);
        showNotification(`Đã lọc dữ liệu theo ${period}`, 'success');
    } catch (error) {
        console.error('Lỗi khi lọc dữ liệu:', error);
        showNotification('Không thể lọc dữ liệu', 'error');
    }
}

// Lọc giao dịch theo loại
function filterTransactions(type) {
    const rows = document.querySelectorAll('table tbody tr');
    rows.forEach(row => {
        const typeCell = row.querySelector('td:nth-child(4)').textContent;
        if (type === 'all' || typeCell.includes(type === 'buy' ? 'Mua bán' : 'Cho thuê')) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
    showNotification(`Đã lọc giao dịch: ${type}`, 'success');
}

// Xuất báo cáo
function exportReport() {
    showNotification('Đang xuất báo cáo...', 'info');
    // TODO: Tích hợp API xuất CSV/Excel
}

// In trang
function printDashboard() {
    window.print();
}

// Cập nhật dữ liệu
async function refreshData() {
    try {
        await loadDashboardStats();
        showNotification('Dữ liệu đã được cập nhật', 'success');
    } catch (error) {
        console.error('Lỗi khi cập nhật dữ liệu:', error);
        showNotification('Không thể cập nhật dữ liệu', 'error');
    }
}

// Keep track of charts initialized by this generic function to allow for re-drawing by this function
let genericRevenueChartInstance = null;
let genericStatusChartInstance = null;

// Vẽ biểu đồ
function drawCharts() {
    // Skip chart initialization if we're on a page that already has charts initialized in its own script
    // Dashboard has its own chart initialization logic
    if (window.location.pathname.includes('/admin/dashboard')) {
        return;
    }
    
    const revenueChartCanvas = document.getElementById('revenueChart');
    const statusChartCanvas = document.getElementById('statusChart');

    if (revenueChartCanvas) {
        // Check if a chart is already registered on this canvas ID
        // If not, then this generic function can draw its version.
        if (Chart.getChart(revenueChartCanvas) === undefined) {
            if (genericRevenueChartInstance) {
                genericRevenueChartInstance.destroy(); // Destroy previous instance from *this* function
            }
            genericRevenueChartInstance = new Chart(revenueChartCanvas, {
                type: 'line',
                data: {
                    labels: ['T1', 'T2', 'T3', 'T4', 'T5'],
                    datasets: [{
                        label: 'Doanh thu (tỷ VNĐ)',
                        data: [10, 15, 12, 20, 18],
                        borderColor: '#0d6efd',
                        fill: false
                    }]
                },
                options: {
                    responsive: true,
                    scales: { y: { beginAtZero: true } }
                }
            });
        }
    }

    if (statusChartCanvas) {
        // Check if a chart is already registered on this canvas ID
        if (Chart.getChart(statusChartCanvas) === undefined) {
            if (genericStatusChartInstance) {
                genericStatusChartInstance.destroy(); // Destroy previous instance from *this* function
            }
            genericStatusChartInstance = new Chart(statusChartCanvas, {
                type: 'pie',
                data: {
                    labels: ['Hoàn thành', 'Đang xử lý', 'Đã hủy'],
                    datasets: [{
                        data: [50, 30, 20],
                        backgroundColor: ['#28a745', '#ffc107', '#dc3545']
                    }]
                },
                options: { responsive: true }
            });
        }
    }
}

// Hiển thị thông báo bằng Bootstrap Toast
function showNotification(message, type = 'info') {
    const toastContainer = document.createElement('div');
    toastContainer.className = 'position-fixed bottom-0 end-0 p-3';
    toastContainer.style.zIndex = '11';
    toastContainer.innerHTML = `
        <div class="toast bg-${type}" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="toast-header">
                <strong class="me-auto">Thông báo</strong>
                <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">${message}</div>
        </div>
    `;
    document.body.appendChild(toastContainer);

    const toast = toastContainer.querySelector('.toast');
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    toast.addEventListener('hidden.bs.toast', () => toastContainer.remove());
}

// Xử lý form thêm giao dịch
function validateAddTransactionForm() {
    const form = document.getElementById('addOrderForm');
    if (!form || !form.checkValidity()) {
        form?.classList.add('was-validated');
        return false;
    }

    const price = document.getElementById('orderPrice')?.value;
    if (price < 0) {
        showNotification('Giá phải lớn hơn hoặc bằng 0!', 'error');
        return false;
    }

    showNotification('Thêm giao dịch thành công!', 'success');
    form.reset();
    form.classList.remove('was-validated');
    bootstrap.Modal.getInstance(document.getElementById('addOrderModal'))?.hide();
}

// Xử lý form chỉnh sửa giao dịch
function validateEditTransactionForm() {
    const form = document.getElementById('editOrderForm');
    if (!form || !form.checkValidity()) {
        form?.classList.add('was-validated');
        return false;
    }

    const price = document.getElementById('editOrderPrice')?.value;
    if (price < 0) {
        showNotification('Giá phải lớn hơn hoặc bằng 0!', 'error');
        return false;
    }

    showNotification('Cập nhật giao dịch thành công!', 'success');
    form.classList.remove('was-validated');
    bootstrap.Modal.getInstance(document.getElementById('editOrderModal'))?.hide();
}

// Thay thế fetchPropertyTypes
async function fetchPropertyTypes() {
    try {
        const res = await fetchWithAuth('/api/property-types');
        if (!res.ok) throw new Error('Không thể lấy loại bất động sản');
        return await res.json();
    } catch (error) {
        if (error.message !== 'Authentication required') {
            console.error('Error fetching property types:', error);
        }
        throw error;
    }
}

// Thay thế fetchProperties
async function fetchProperties() {
    try {
        const res = await fetchWithAuth('/api/properties');
        if (!res.ok) throw new Error('Không thể lấy danh sách bất động sản');
        return await res.json();
    } catch (error) {
        if (error.message !== 'Authentication required') {
            console.error('Error fetching properties:', error);
        }
        throw error;
    }
}

// Khởi chạy khi trang tải xong
document.addEventListener('DOMContentLoaded', () => {
    updateSidebarActiveState();
    initializeEvents();
    
    // Chỉ gọi drawCharts() nếu không phải trang dashboard
    // Trang dashboard có script riêng để khởi tạo biểu đồ
    if (!window.location.pathname.includes('/admin/dashboard')) {
        drawCharts();
    }
    
    if (document.querySelector('.bg-primary h2')) {
        loadDashboardStats();
    }
});