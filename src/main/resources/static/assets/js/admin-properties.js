// Utility function to format property status 
function getStatusBadgeClass(status, adminDeactivated, realtorDeleted) {
    // Handle null or undefined values
    adminDeactivated = adminDeactivated === true;
    realtorDeleted = realtorDeleted === true;
    
    // Kiểm tra nếu property bị vô hiệu hóa và xác định nguyên nhân
    if (status === 'INACTIVE') {
        if (adminDeactivated) {
            return 'bg-danger'; // Admin vô hiệu hóa: hiển thị màu đỏ
        }
        if (realtorDeleted) {
            return 'bg-secondary'; // Realtor xóa: hiển thị màu xám
        }
        return 'bg-secondary'; // INACTIVE mà không rõ lý do
    }
    
    // Các trạng thái khác
    switch (status) {
        case 'AVAILABLE':
            return 'bg-success';
        case 'PENDING':
            return 'bg-warning text-dark';
        case 'SOLD':
            return 'bg-info';
        case 'RENTED':
            return 'bg-info';
        case 'UNAVAILABLE':
            return 'bg-secondary';
        case 'DRAFT':
            return 'bg-light text-dark';
        case 'EXPIRED':
            return 'bg-dark';
        default:
            return 'bg-light text-dark';
    }
}

// Format price to VND currency
function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', { 
        style: 'currency', 
        currency: 'VND',
        maximumFractionDigits: 0
    }).format(price);
}

// View property details
async function viewPropertyDetails(propertyId) {
    try {
        const response = await fetch(`/api/properties/${propertyId}`);
        if (!response.ok) throw new Error('Failed to load property details');
        
        const property = await response.json();
        console.log("Property data:", property); // Debug: log property data
        
        // Populate property details
        document.getElementById('viewPropertyName').textContent = property.name || 'N/A';
        document.getElementById('viewPropertyType').textContent = property.propertyTypeName || 'N/A';
        document.getElementById('viewPropertyPrice').textContent = formatPrice(property.price);
        document.getElementById('viewPropertyId').textContent = property.id || 'N/A';
        document.getElementById('viewTransactionType').textContent = property.transactionType || 'N/A';
        
        // Ensure adminDeactivated and realtorDeleted have default values if null
        const isAdminDeactivated = property.adminDeactivated === true;
        const isRealtorDeleted = property.realtorDeleted === true;
        
        const statusBadge = document.getElementById('viewPropertyStatus');
        statusBadge.textContent = property.status;
        statusBadge.className = `badge ${getStatusBadgeClass(property.status, isAdminDeactivated, isRealtorDeleted)}`;
        
        // Thêm badge text status với cả thông tin chi tiết về lý do
        if (property.status === 'INACTIVE') {
            if (isAdminDeactivated) {
                statusBadge.textContent = "VÔ HIỆU HÓA (ADMIN)";
            } else if (isRealtorDeleted) {
                statusBadge.textContent = "VÔ HIỆU HÓA (REALTOR)";
            }
        }
        
        document.getElementById('viewPropertyAddress').textContent = property.address || 'N/A';
        document.getElementById('viewPropertyArea').textContent = `${property.area || 0} m²`;
        document.getElementById('viewPropertyBedrooms').textContent = property.bedrooms || 'N/A';
        document.getElementById('viewPropertyBathrooms').textContent = property.bathrooms || 'N/A';
        document.getElementById('viewPropertyDescription').textContent = property.description || 'Không có mô tả';
        document.getElementById('viewPropertyCreatedAt').textContent = property.createdAt ? 
            new Date(property.createdAt).toLocaleString('vi-VN') : 'N/A';
        
        // Set property ID for action buttons
        const deactivateBtn = document.getElementById('deactivatePropertyBtn');
        const reactivateBtn = document.getElementById('reactivatePropertyBtn');
        
        // Thêm thông tin về trạng thái xóa
        const deletedInfoEl = document.getElementById('viewPropertyDeletedInfo');
        if (deletedInfoEl) {
            if (isRealtorDeleted) {
                deletedInfoEl.innerHTML = '<span class="badge bg-danger"><i class="bi bi-trash-fill"></i> Đã xóa bởi môi giới</span>';
                deletedInfoEl.style.display = 'block';
            } else if (isAdminDeactivated) {
                deletedInfoEl.innerHTML = '<span class="badge bg-warning text-dark"><i class="bi bi-slash-circle-fill"></i> Vô hiệu hóa bởi Admin</span>';
                deletedInfoEl.style.display = 'block';
            } else {
                deletedInfoEl.style.display = 'none';
            }
        }
        
        if (deactivateBtn && reactivateBtn) {
            // Set property ID for both buttons
            deactivateBtn.setAttribute('data-property-id', property.id);
            reactivateBtn.setAttribute('data-property-id', property.id);
            
            // Xóa thông báo cũ nếu có
            const existingNotice = document.getElementById('propertyStatusNotice');
            if (existingNotice) {
                existingNotice.remove();
            }
            
            // Khởi tạo mặc định: ẩn cả hai nút
            deactivateBtn.style.display = 'none';
            reactivateBtn.style.display = 'none';
            
            // Kiểm tra nếu property đã bị realtor xóa thì ẩn cả hai nút và hiển thị thông báo
            if (isRealtorDeleted) {
                // Thêm thông báo để admin biết
                const modalFooter = deactivateBtn.parentElement;
                const noticeElement = document.createElement('div');
                noticeElement.id = 'propertyStatusNotice';
                noticeElement.className = 'alert alert-secondary mt-2 mb-0';
                noticeElement.innerHTML = '<i class="bi bi-info-circle me-2"></i>Bất động sản này đã bị môi giới xóa, không thể thay đổi trạng thái.';
                modalFooter.prepend(noticeElement);
            } 
            // Hiển thị nút dựa trên trạng thái 
            else if (property.status === 'AVAILABLE') {
                // Nếu property đang available, hiển thị nút vô hiệu hóa
                deactivateBtn.style.display = 'block';
            } 
            else if (property.status === 'INACTIVE' && isAdminDeactivated) {
                // Nếu property đã bị admin vô hiệu hóa, hiển thị nút kích hoạt lại
                reactivateBtn.style.display = 'block';
            }
            else {
                // Các trường hợp còn lại (SOLD, RENTED, OTHER...)
                // Hiển thị thông báo cho admin
                const modalFooter = deactivateBtn.parentElement;
                const noticeElement = document.createElement('div');
                noticeElement.id = 'propertyStatusNotice';
                noticeElement.className = 'alert alert-info mt-2 mb-0';
                noticeElement.innerHTML = `<i class="bi bi-info-circle me-2"></i>Bất động sản với trạng thái "${property.status}" không thể vô hiệu hóa hoặc kích hoạt lại.`;
                modalFooter.prepend(noticeElement);
            }
        }
        
        // Load realtor information
        const realtorNameEl = document.getElementById('viewRealtorName');
        const realtorContactEl = document.getElementById('viewRealtorContact');
        const realtorAvatarEl = document.getElementById('viewRealtorAvatar');
        
        if (property.realtorId) {
            // Use the fetch API to get realtor details if needed
            try {
                const realtorResponse = await fetch(`/api/users/${property.realtorId}`);
                if (realtorResponse.ok) {
                    const realtorData = await realtorResponse.json();
                    realtorNameEl.textContent = `${realtorData.firstName || ''} ${realtorData.lastName || ''}`.trim() || 'N/A';
                    realtorContactEl.textContent = realtorData.email || realtorData.phone || 'N/A';
                    if (realtorData.avatarUrl) {
                        realtorAvatarEl.src = realtorData.avatarUrl;
                    }
                } else {
                    // Fallback to using the data already in the property object
                    if (property.realtor) {
                        realtorNameEl.textContent = `${property.realtor.firstName || ''} ${property.realtor.lastName || ''}`.trim() || 'N/A';
                        realtorContactEl.textContent = property.realtor.email || property.realtor.phone || 'N/A';
                        if (property.realtor.avatarUrl) {
                            realtorAvatarEl.src = property.realtor.avatarUrl;
                        }
                    } else {
                        realtorNameEl.textContent = 'Không có thông tin';
                        realtorContactEl.textContent = '';
                    }
                }
            } catch (error) {
                console.error('Error loading realtor details:', error);
                // Fallback
                if (property.realtor) {
                    realtorNameEl.textContent = `${property.realtor.firstName || ''} ${property.realtor.lastName || ''}`.trim() || 'N/A';
                    realtorContactEl.textContent = property.realtor.email || property.realtor.phone || 'N/A';
                } else {
                    realtorNameEl.textContent = 'Không có thông tin';
                    realtorContactEl.textContent = '';
                }
            }
        } else {
            realtorNameEl.textContent = 'Không có thông tin';
            realtorContactEl.textContent = '';
        }
        
        // Load property images
        const imagesContainer = document.getElementById('propertyImagesContainer');
        imagesContainer.innerHTML = '';
        
        if (property.images && property.images.length > 0) {
            property.images.forEach((image, index) => {
                const div = document.createElement('div');
                div.className = `carousel-item ${index === 0 ? 'active' : ''}`;
                
                const img = document.createElement('img');
                img.src = image.imageUrl;
                img.className = 'd-block w-100';
                img.alt = property.name;
                img.style.height = '300px';
                img.style.objectFit = 'cover';
                
                div.appendChild(img);
                imagesContainer.appendChild(div);
            });
        } else {
            const div = document.createElement('div');
            div.className = 'carousel-item active';
            
            const placeholder = document.createElement('div');
            placeholder.className = 'bg-light d-flex align-items-center justify-content-center';
            placeholder.style.height = '300px';
            
            const icon = document.createElement('i');
            icon.className = 'bi bi-house-fill text-secondary';
            icon.style.fontSize = '5rem';
            
            placeholder.appendChild(icon);
            div.appendChild(placeholder);
            imagesContainer.appendChild(div);
        }
        
        // Open modal
        const modalElement = document.getElementById('viewPropertyModal');
        const modalInstance = new bootstrap.Modal(modalElement);
        modalInstance.show();
    } catch (error) {
        console.error('Error loading property details:', error);
        showToast('Không thể tải thông tin bất động sản', 'error');
    }
}

// Function to handle modal closing
function handleModalClose() {
    // Clean up any resources
    document.getElementById('propertyImagesContainer').innerHTML = '';
    
    // Make sure backdrop is removed
    const modals = document.querySelectorAll('.modal-backdrop');
    modals.forEach(modal => {
        modal.remove();
    });
    
    // Remove modal-open class from body and restore scrolling
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
    
    // Đặt lại các nút và trạng thái trong modal
    const deactivateBtn = document.getElementById('deactivatePropertyBtn');
    const reactivateBtn = document.getElementById('reactivatePropertyBtn');
    if (deactivateBtn && reactivateBtn) {
        deactivateBtn.disabled = false;
        deactivateBtn.removeAttribute('data-property-id');
        reactivateBtn.removeAttribute('data-property-id');
        
        // Reset display state
        deactivateBtn.style.display = 'block';
        reactivateBtn.style.display = 'none';
    }
    
    // Đóng hết các event listener không cần thiết
    const modalElement = document.getElementById('viewPropertyModal');
    if (modalElement) {
        // Đảm bảo modal thực sự ẩn trong DOM
        modalElement.style.display = 'none';
        modalElement.classList.remove('show');
        modalElement.setAttribute('aria-hidden', 'true');
    }
}

// showToast function if not exists
function showToast(message, type = 'info') { 
    try {
        if (typeof toastr === 'undefined') { 
            // Fallback khi không có toastr
            console.log(`Thông báo (${type}): ${message}`);
            alert(message); 
            return;
        }
        
        // Đảm bảo toastr đã được khởi tạo
        if (!toastr.options) {
            toastr.options = {
                "closeButton": true,
                "positionClass": "toast-bottom-right",
                "showDuration": "300",
                "hideDuration": "1000",
                "timeOut": "5000",
                "extendedTimeOut": "1000"
            };
        }
        
        // Đảm bảo phương thức type tồn tại trong toastr
        if (toastr[type] && typeof toastr[type] === 'function') {
            toastr[type](message);
        } else {
            // Fallback nếu loại thông báo không tồn tại
            console.log(`Thông báo (${type}): ${message}`);
            if (typeof toastr.info === 'function') {
                toastr.info(message);
            } else if (typeof toastr.success === 'function') {
                toastr.success(message);
            } else {
                alert(message);
            }
        }
    } catch (error) {
        console.error("Error showing toast:", error);
        alert(message);
    }
}

// Deactivate property and send notification email to realtor
async function deactivateProperty(propertyId) {
    try {
        const response = await fetch(`/api/properties/${propertyId}/deactivate`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || data.error || 'Lỗi khi vô hiệu hóa bất động sản');
        }
        
        // Nếu là lỗi từ máy chủ (response.ok = true nhưng success = false)
        if (data.success === false) {
            throw new Error(data.message || 'Không thể vô hiệu hóa bất động sản');
        }
        
        // Đóng modal nếu đang mở trước khi hiển thị thông báo
        const modal = document.getElementById('viewPropertyModal');
        if (modal) {
            const bsModal = bootstrap.Modal.getInstance(modal);
            if (bsModal) {
                bsModal.hide();
                // Đảm bảo modal được dọn sạch
                setTimeout(() => {
                    handleModalClose();
                }, 150);
            }
        }
        
        // Cập nhật UI trực tiếp mà không cần tải lại trang
        updateTableRowStatus(propertyId, 'INACTIVE', true, false);
        
        // Hiển thị thông báo thành công
        showToast(data.message || 'Đã vô hiệu hóa bất động sản và gửi thông báo cho môi giới', 'success');
    } catch (error) {
        console.error('Error deactivating property:', error);
        showToast(error.message || 'Đã xảy ra lỗi', 'error');
    }
}

// Reactivate property and notify realtor
async function reactivateProperty(propertyId) {
    try {
        const response = await fetch(`/api/properties/${propertyId}/reactivate`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || data.error || 'Lỗi khi kích hoạt lại bất động sản');
        }
        
        // Nếu là lỗi từ máy chủ (response.ok = true nhưng success = false) 
        if (data.success === false) {
            throw new Error(data.message || 'Không thể kích hoạt lại bất động sản');
        }
        
        // Đóng modal nếu đang mở trước khi hiển thị thông báo
        const modal = document.getElementById('viewPropertyModal');
        if (modal) {
            const bsModal = bootstrap.Modal.getInstance(modal);
            if (bsModal) {
                bsModal.hide();
                // Đảm bảo modal được dọn sạch
                setTimeout(() => {
                    handleModalClose();
                }, 150);
            }
        }
        
        // Cập nhật UI trực tiếp mà không cần tải lại trang
        updateTableRowStatus(propertyId, 'AVAILABLE', false, false);
        
        // Hiển thị thông báo thành công
        showToast(data.message || 'Đã kích hoạt lại bất động sản và gửi thông báo cho môi giới', 'success');
    } catch (error) {
        console.error('Error reactivating property:', error);
        showToast(error.message || 'Đã xảy ra lỗi', 'error');
    }
}

// Hàm để cập nhật trạng thái của dòng trong bảng
function updateTableRowStatus(propertyId, newStatus, isAdminDeactivated, isRealtorDeleted) {
    // Tìm dòng bằng propertyId
    const rows = document.querySelectorAll('table tbody tr');
    rows.forEach(row => {
        const idCell = row.cells[0];
        if (idCell && idCell.textContent == propertyId) {
            // Tìm ô trạng thái (cột thứ 6)
            const statusCell = row.cells[6];
            if (statusCell) {
                const statusBadge = statusCell.querySelector('.badge');
                if (statusBadge) {
                    // Cập nhật text và class của badge
                    let statusText = newStatus;
                    let badgeClass = 'badge ';
                    
                    // Xác định class và text dựa trên trạng thái
                    if (newStatus === 'INACTIVE') {
                        if (isAdminDeactivated) {
                            badgeClass += 'bg-danger';
                            statusText = 'VÔ HIỆU HÓA (ADMIN)';
                        } else if (isRealtorDeleted) {
                            badgeClass += 'bg-secondary';
                            statusText = 'VÔ HIỆU HÓA (REALTOR)';
                        } else {
                            badgeClass += 'bg-secondary';
                        }
                    } else if (newStatus === 'AVAILABLE') {
                        badgeClass += 'bg-success';
                    } else if (newStatus === 'PENDING') {
                        badgeClass += 'bg-warning text-dark';
                    } else if (newStatus === 'SOLD') {
                        badgeClass += 'bg-info';
                    } else {
                        badgeClass += 'bg-light text-dark';
                    }
                    
                    // Cập nhật class và text
                    statusBadge.className = badgeClass;
                    statusBadge.textContent = statusText;
                }
            }
            
            // Update action buttons (cột thứ 8)
            const actionCell = row.cells[8];
            if (actionCell) {
                const actionBtns = actionCell.querySelector('.btn-group');
                if (actionBtns) {
                    // Clear existing buttons except view button
                    const viewBtn = actionBtns.querySelector('button[data-bs-toggle="modal"]');
                    actionBtns.innerHTML = '';
                    if (viewBtn) {
                        actionBtns.appendChild(viewBtn);
                    }
                    
                    // Add appropriate action buttons based on new status
                    if (newStatus === 'AVAILABLE' && !isRealtorDeleted) {
                        // Add deactivate button for AVAILABLE properties
                        const deactivateBtn = document.createElement('button');
                        deactivateBtn.className = 'btn btn-sm btn-outline-warning';
                        deactivateBtn.setAttribute('data-property-id', propertyId);
                        deactivateBtn.setAttribute('aria-label', 'Vô hiệu hóa bất động sản');
                        deactivateBtn.innerHTML = '<i class="bi bi-ban"></i>';
                        deactivateBtn.onclick = function() { confirmDeactivateProperty(propertyId); };
                        actionBtns.appendChild(deactivateBtn);
                    } else if (newStatus === 'INACTIVE' && isAdminDeactivated && !isRealtorDeleted) {
                        // Add reactivate button for admin-deactivated properties
                        const reactivateBtn = document.createElement('button');
                        reactivateBtn.className = 'btn btn-sm btn-outline-success';
                        reactivateBtn.setAttribute('data-property-id', propertyId);
                        reactivateBtn.setAttribute('aria-label', 'Kích hoạt lại bất động sản');
                        reactivateBtn.innerHTML = '<i class="bi bi-check-circle"></i>';
                        reactivateBtn.onclick = function() { confirmReactivateProperty(propertyId); };
                        actionBtns.appendChild(reactivateBtn);
                    }
                }
            }
        }
    });
}

// Filter functions - completely rewritten to be more robust
function filterProperties() {
    const searchInput = document.getElementById('searchInput').value.toLowerCase().trim();
    const typeFilter = document.getElementById('typeFilter').value.toLowerCase().trim();
    const statusFilter = document.getElementById('statusFilter').value.toLowerCase().trim();
    const priceFilter = document.getElementById('priceFilter').value.trim();
    
    // First remove any existing "no results" row
    const existingNoResultsRow = document.querySelector('.no-results-row');
    if (existingNoResultsRow) {
        existingNoResultsRow.remove();
    }
    
    const rows = document.querySelectorAll('table tbody tr');
    let hasVisibleRows = false;
    
    rows.forEach(row => {
        // Skip empty message row
        if (row.cells.length <= 1) return;
        
        let isVisible = true;
        
        // Get cell values safely
        const name = (row.cells[2]?.textContent || '').toLowerCase();
        const address = (row.cells[3]?.textContent || '').toLowerCase();
        const priceText = row.cells[4]?.textContent || '0';
        const typeName = (row.cells[5]?.textContent || '').toLowerCase();
        const statusCell = row.cells[6]?.querySelector('.badge');
        const status = statusCell ? statusCell.textContent.toLowerCase() : '';
        
        // Extract numeric price value (remove non-numeric characters)
        const price = parseFloat(priceText.replace(/[^\d.-]/g, '')) || 0;
        
        // Apply search filter
        if (searchInput && !name.includes(searchInput) && !address.includes(searchInput)) {
            isVisible = false;
        }
        
        // Apply type filter - match by property type name
        if (typeFilter && typeName) {
            // Check if the type matches directly
            if (!typeName.includes(typeFilter)) {
                isVisible = false;
            }
        }
        
        // Apply status filter with updated status values
        if (statusFilter && status) {
            // Map dropdown values to status values that might appear in the table
            const statusMap = {
                'available': ['available', 'đang bán', 'cho thuê'],
                'sold': ['sold', 'đã bán'],
                'rented': ['rented', 'đã thuê'],
                'inactive': ['inactive', 'vô hiệu hóa', 'vô hiệu hóa (admin)', 'vô hiệu hóa (realtor)']
            };
            
            // Check if status matches using the mapping
            const matchingStatuses = statusMap[statusFilter] || [];
            const matchesStatus = matchingStatuses.some(s => status.includes(s));
            
            if (!matchesStatus) {
                isVisible = false;
            }
        }
        
        // Apply price filter - price in billions (tỷ)
        if (priceFilter) {
            // Convert to billions for comparison (assuming original price is in VND)
            const priceInBillions = price / 1000000000;
            
            switch (priceFilter) {
                case '0-1':
                    if (priceInBillions >= 1) {
                        isVisible = false;
                    }
                    break;
                case '1-2':
                    if (priceInBillions < 1 || priceInBillions > 2) {
                        isVisible = false;
                    }
                    break;
                case '2-5':
                    if (priceInBillions < 2 || priceInBillions > 5) {
                        isVisible = false;
                    }
                    break;
                case '5-10':
                    if (priceInBillions < 5 || priceInBillions > 10) {
                        isVisible = false;
                    }
                    break;
                case '10+':
                    if (priceInBillions < 10) {
                        isVisible = false;
                    }
                    break;
            }
        }
        
        // Apply visibility
        row.style.display = isVisible ? '' : 'none';
        if (isVisible) {
            hasVisibleRows = true;
        }
    });
    
    // Add "no results" message if all rows are filtered out
    if (!hasVisibleRows) {
        const tbody = document.querySelector('table tbody');
        if (tbody) {
            const newRow = document.createElement('tr');
            newRow.className = 'no-results-row';
            newRow.innerHTML = `<td colspan="9" class="text-center">Không tìm thấy bất động sản phù hợp với bộ lọc.</td>`;
            tbody.appendChild(newRow);
        }
    }
}

// Pagination
function changePageSize() {
    const size = document.getElementById('rowsPerPage').value;
    
    // Giữ lại các tham số lọc khác
    const searchInput = document.getElementById('searchInput').value;
    const typeFilter = document.getElementById('typeFilter').value;
    const statusFilter = document.getElementById('statusFilter').value;
    const priceFilter = document.getElementById('priceFilter').value;
    
    // Xây dựng URL với tất cả các tham số
    let url = `/admin/properties?page=0&size=${size}`;
    
    if (searchInput) url += `&search=${encodeURIComponent(searchInput)}`;
    if (typeFilter) url += `&type=${encodeURIComponent(typeFilter)}`;
    if (statusFilter) url += `&status=${encodeURIComponent(statusFilter)}`;
    if (priceFilter) url += `&price=${encodeURIComponent(priceFilter)}`;
    
    window.location.href = url;
}

// Cập nhật link phân trang để giữ lại tham số lọc
function updatePaginationLinks() {
    // Lấy tất cả các tham số URL hiện tại
    const urlParams = new URLSearchParams(window.location.search);
    
    // Lấy các tham số lọc
    const search = urlParams.get('search') || '';
    const type = urlParams.get('type') || '';
    const status = urlParams.get('status') || '';
    const price = urlParams.get('price') || '';
    const size = urlParams.get('size') || '10';
    
    // Cập nhật tất cả các link phân trang
    const paginationLinks = document.querySelectorAll('.pagination .page-link');
    paginationLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href) {
            // Tạo URLSearchParams từ href hiện tại
            const linkParams = new URLSearchParams(href.split('?')[1]);
            
            // Lấy giá trị page từ link
            const page = linkParams.get('page');
            
            // Tạo URL mới với tất cả tham số
            let newUrl = `/admin/properties?page=${page}&size=${size}`;
            
            if (search) newUrl += `&search=${encodeURIComponent(search)}`;
            if (type) newUrl += `&type=${encodeURIComponent(type)}`;
            if (status) newUrl += `&status=${encodeURIComponent(status)}`;
            if (price) newUrl += `&price=${encodeURIComponent(price)}`;
            
            // Cập nhật href
            link.setAttribute('href', newUrl);
        }
    });
}

// Event listeners
document.addEventListener('DOMContentLoaded', () => {
    // Add event listeners for view property buttons
    const viewButtons = document.querySelectorAll('[data-bs-target="#viewPropertyModal"]');
    viewButtons.forEach(button => {
        button.addEventListener('click', () => {
            const propertyId = button.getAttribute('data-property-id');
            viewPropertyDetails(propertyId);
        });
    });
    
    // Handle modal hidden event to ensure proper cleanup
    const modalElement = document.getElementById('viewPropertyModal');
    if (modalElement) {
        modalElement.addEventListener('hidden.bs.modal', () => {
            handleModalClose();
        });
    }
    
    // Add event listener for rows per page change
    const rowsPerPage = document.getElementById('rowsPerPage');
    if (rowsPerPage) {
        rowsPerPage.addEventListener('change', changePageSize);
    }
    
    // Cập nhật links phân trang
    updatePaginationLinks();
    
    // Setup filter form - sử dụng cả client-side và server-side filtering
    const filterFormElement = document.getElementById('filterForm');
    if (filterFormElement) {
        // Server-side filtering: form submit sẽ gửi GET request
        filterFormElement.addEventListener('submit', (e) => {
            // Form đã có method GET và action nên không cần ngăn chặn
            // Chỉ đảm bảo đặt page về 0
            document.getElementById('pageInput').value = '0';
        });
        
        // Client-side filtering: bổ sung để filter ngay trên UI mà không reload
        const filterInputs = filterFormElement.querySelectorAll('input, select');
        filterInputs.forEach(input => {
            input.addEventListener('input', () => {
                // Không dùng ở đây vì chúng ta đã chuyển sang server-side filtering
                // filterProperties();
            });
        });
        
        // Reset filters button
        const resetButton = filterFormElement.querySelector('a[aria-label="Xóa bộ lọc"]');
        if (resetButton) {
            resetButton.addEventListener('click', (e) => {
                // Không cần làm gì thêm vì link đã trỏ về /admin/properties không có params
            });
        }
    }
}); 