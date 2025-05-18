document.addEventListener('DOMContentLoaded', function() {
    // Kiểm tra đăng nhập
    if (!checkAuth()) {
        return; // Không tiếp tục nếu chưa đăng nhập
    }
    
    // Khởi tạo các sự kiện
    initializeEvents();
    
    // Kiểm tra vai trò và điều chỉnh giao diện tương ứng
    checkUserRole();
    
    // Tải dữ liệu người dùng
    loadUserData();
    
    // Tải lịch sử giao dịch
    loadTransactions();
    
    // Tải danh sách yêu thích
    loadFavorites();
    
    // Tải cài đặt thông báo
    loadNotificationSettings();
    
    // Kích hoạt tab dựa trên hash URL nếu có
    activateTabFromHash();
});

// Kiểm tra đăng nhập
function checkAuth() {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    
    if (!token || !user || !user.id) {
        // Không có token hoặc dữ liệu người dùng, chuyển hướng đến trang đăng nhập
        window.location.href = '/login';
        return false;
    }
    return true;
}

// Khởi tạo các sự kiện
function initializeEvents() {
    // Đăng ký sự kiện cho các tab
    const tabs = document.querySelectorAll('.list-group-item[data-bs-toggle="list"]');
    const tabContents = document.querySelectorAll('.tab-pane');
    
    console.log('Found', tabs.length, 'tabs on the page');
    
    tabs.forEach(tab => {
        tab.addEventListener('click', function(e) {
            e.preventDefault();
            const targetTab = this.getAttribute('href');
            console.log('Tab clicked:', targetTab);
            tabs.forEach(t => t.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active', 'show'));
            this.classList.add('active');
            const content = document.querySelector(targetTab);
            if (content) {
                content.classList.add('active', 'show');
                console.log('Tab content activated:', targetTab);
            }
            history.replaceState(null, null, targetTab);
            if (targetTab === '#transactions') {
                console.log('Loading transactions data for tab');
                loadTransactions();
                const user = JSON.parse(localStorage.getItem('user') || '{}');
                if (user.role === 'REALTOR' || user.role === 'ADMIN') {
                    requestAnimationFrame(() => {
                        console.log('Tab click: Attempting to load realtor transaction requests for #transactions tab.');
                        loadRealtorTransactionRequests();
                    });
                }
            } else if (targetTab === '#properties') {
                console.log('Loading properties data for tab');
                loadRealtorProperties();
            } else if (targetTab === '#favorites') {
                console.log('Loading favorites data for tab');
                loadFavorites();
            }
        });
    });
    
    const profileForm = document.getElementById('profileForm');
    if (profileForm) {
        profileForm.addEventListener('submit', updateProfile);
    }
    
    const securityForm = document.getElementById('securityForm');
    if (securityForm) {
        securityForm.addEventListener('submit', changePassword);
    }

    const avatarInput = document.getElementById('avatarInput');
    const avatarPreview = document.getElementById('avatarPreview');
    const saveAvatarButton = document.getElementById('saveAvatar');

    if (avatarInput && avatarPreview) {
        avatarInput.addEventListener('change', function(event) {
            const file = event.target.files[0];
            if (file) {
                if (!file.type.startsWith('image/')) {
                    showNotification('Vui lòng chọn một file ảnh.', 'warning');
                    avatarInput.value = '';
                    avatarPreview.src = document.getElementById('userAvatar').src || '../assets/images/avatar-default.jpg';
                    return;
                }
                if (file.size > 2 * 1024 * 1024) { // 2MB
                    showNotification('Kích thước file không được vượt quá 2MB.', 'warning');
                    avatarInput.value = ''; 
                    avatarPreview.src = document.getElementById('userAvatar').src || '../assets/images/avatar-default.jpg';
                    return;
                }
                const reader = new FileReader();
                reader.onload = function(e) {
                    avatarPreview.src = e.target.result;
                }
                reader.readAsDataURL(file);
            }
        });
    }

    if (saveAvatarButton) {
        saveAvatarButton.addEventListener('click', uploadAvatar);
    }
    
    const changeAvatarModal = document.getElementById('changeAvatarModal');
    if (changeAvatarModal) {
        changeAvatarModal.addEventListener('show.bs.modal', function () {
            const currentUserAvatar = document.getElementById('userAvatar').src;
            if (avatarPreview) avatarPreview.src = currentUserAvatar || '../assets/images/avatar-default.jpg';
            if (avatarInput) avatarInput.value = ''; // Reset file input
            document.getElementById('avatarInput').classList.remove('is-invalid');
        });
    }

    // Xử lý nút lưu trong modal thêm BĐS
    const savePropertyButton = document.getElementById('saveProperty');
    if (savePropertyButton) {
        savePropertyButton.addEventListener('click', saveProperty);
    }

    // Sự kiện khi modal thêm/sửa BĐS được hiển thị
    const addPropertyModal = document.getElementById('addPropertyModal');
    if (addPropertyModal) {
        addPropertyModal.addEventListener('show.bs.modal', async function (event) {
            await loadPropertyTypes(); // Tải các loại BĐS
            const button = event.relatedTarget; // Nút đã kích hoạt modal
            const propertyId = button ? button.getAttribute('data-id') : null;
            const modalTitle = document.getElementById('addPropertyModalLabel');
            const propertyIdField = document.getElementById('propertyId');
            const propertyForm = document.getElementById('propertyForm');
            const existingImagesContainer = document.getElementById('existingImagesContainer');
            
            propertyForm.reset(); // Reset form mỗi khi mở
            existingImagesContainer.innerHTML = ''; // Xóa ảnh cũ hiển thị

            if (propertyId) { // Chế độ sửa
                modalTitle.textContent = 'Cập nhật bất động sản';
                propertyIdField.value = propertyId;
                await populatePropertyForm(propertyId);
            } else { // Chế độ thêm mới
                modalTitle.textContent = 'Thêm bất động sản mới';
                propertyIdField.value = '';
            }
        });
    }

    // Event delegation cho các nút sửa và xóa BĐS trong bảng
    const propertiesTableBody = document.getElementById('propertiesTableBody');
    if (propertiesTableBody) {
        propertiesTableBody.addEventListener('click', function(event) {
            const target = event.target;
            if (target.classList.contains('edit-property-btn') || target.closest('.edit-property-btn')) {
                // Modal sẽ tự mở qua data-bs-toggle, xử lý đã nằm trong 'show.bs.modal'
            } else if (target.classList.contains('delete-property-btn') || target.closest('.delete-property-btn')) {
                const button = target.closest('.delete-property-btn');
                const propertyId = button.getAttribute('data-id');
                if (propertyId) {
                    deletePropertyHandler(propertyId);
                }
            }
        });
    }
}

// Kiểm tra vai trò người dùng và điều chỉnh giao diện
function checkUserRole() {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const isRealtor = user.role === 'REALTOR';
    const isAdmin = user.role === 'ADMIN';
    
    // Hiển thị các phần tử dựa vào vai trò
    document.querySelectorAll('.user-only').forEach(element => {
        element.style.display = !isRealtor && !isAdmin ? 'block' : 'none';
    });
    
    document.querySelectorAll('.realtor-only').forEach(element => {
        // Chỉ áp dụng display:block cho các element realtor-only KHÔNG PHẢI là tab-pane
        // Các tab-pane sẽ được quản lý bởi logic active/show của Bootstrap tabs
        if (!element.classList.contains('tab-pane')) {
            element.style.display = isRealtor || isAdmin ? 'block' : 'none'; // ADMIN cũng có thể thấy mục của REALTOR
        } else {
            // Nếu là tab-pane.realtor-only (ví dụ tab Quản lý BĐS)
            // không đặt style.display ở đây. Nó sẽ được xử lý bởi trình chuyển tab.
            // Nhưng chúng ta cần đảm bảo tab link trong sidebar được hiển thị/ẩn đúng
            const tabLink = document.querySelector(`.list-group-item[href="#${element.id}"]`);
            if (tabLink) {
                tabLink.style.display = isRealtor || isAdmin ? 'block' : 'none';
            }
        }
    });
    
    document.querySelectorAll('.admin-only').forEach(element => {
        element.style.display = isAdmin ? 'block' : 'none';
    });
    
    const userNameElement = document.getElementById('userName');
    if (userNameElement && (isRealtor || isAdmin) && !document.getElementById('roleBadge')) {
        const roleName = isAdmin ? 'ADMIN' : 'REALTOR';
        const badgeClass = isAdmin ? 'bg-danger' : 'bg-primary';
        userNameElement.innerHTML += ` <span id="roleBadge" class="badge ${badgeClass}">${roleName}</span>`;
    } else if (userNameElement && !isRealtor && !isAdmin && document.getElementById('roleBadge')){
        document.getElementById('roleBadge').remove();
    }

    // Transaction Tabs Logic
    const userRequestsTabButton = document.getElementById('user-requests-tab');
    const userRequestsTabLi = userRequestsTabButton?.closest('li.nav-item');
    const userRequestsContent = document.getElementById('userRequestsContent');

    const realtorRequestsTabButton = document.getElementById('realtor-requests-tab');
    const realtorRequestsTabLi = realtorRequestsTabButton?.closest('li.nav-item'); // realtor-only class handles its display
    const realtorRequestsContent = document.getElementById('realtorRequestsContent'); // realtor-only class handles its display

    if (isRealtor || isAdmin) {
        // REALTOR/ADMIN: Hide 'User Requests' tab and content
        if (userRequestsTabLi) userRequestsTabLi.style.display = 'none';
        if (userRequestsTabButton) {
            userRequestsTabButton.classList.remove('active');
            userRequestsTabButton.setAttribute('aria-selected', 'false');
        }
        if (userRequestsContent) userRequestsContent.classList.remove('show', 'active');

        // REALTOR/ADMIN: Show and activate 'Realtor Requests' tab and content
        // The .realtor-only class on realtorRequestsTabLi and realtorRequestsContent handles their initial display: block/list-item.
        // We just need to set active state.
        if (realtorRequestsTabButton) {
            realtorRequestsTabButton.classList.add('active');
            realtorRequestsTabButton.setAttribute('aria-selected', 'true');
        }
        if (realtorRequestsContent) {
            realtorRequestsContent.classList.add('show', 'active');
        }
        // Load data relevant to realtor/admin for the transactions tab
        // This should ideally be triggered if the transactions tab itself is active
        // The tab click handler in initializeEvents already tries to loadRealtorTransactionRequests
        // console.log('Role check: Realtor/Admin - transaction tab configured.');

    } else { // REGULAR USER
        // USER: Show 'User Requests' tab and content
        if (userRequestsTabLi) userRequestsTabLi.style.display = 'list-item'; // or 'block'
        if (userRequestsTabButton) {
            userRequestsTabButton.classList.add('active');
            userRequestsTabButton.setAttribute('aria-selected', 'true');
        }
        if (userRequestsContent) userRequestsContent.classList.add('show', 'active');

        // USER: 'Realtor Requests' tab and content are already hidden by .realtor-only CSS or generic loop.
        // Ensure they are not active if somehow they were.
        if (realtorRequestsTabButton) {
            realtorRequestsTabButton.classList.remove('active');
            realtorRequestsTabButton.setAttribute('aria-selected', 'false');
        }
        if (realtorRequestsContent) realtorRequestsContent.classList.remove('show', 'active');
        // console.log('Role check: User - transaction tab configured.');
    }

    // Load initial data based on role
    if (isRealtor || isAdmin) {
        // loadRealtorProperties(); // This is often called when #properties tab is active
        // loadRealtorTransactionRequests(); // This is often called when #transactions tab is active
        // The tab click handler in initializeEvents should manage loading data for specific tabs.
    } else {
        // loadRecommendedProperties();
    }
    // Initial data loading like loadUserData(), loadTransactions() for user's own transactions, etc.,
    // are typically called globally after DOMContentLoaded or based on initially active tab.
}

// Tải dữ liệu người dùng
async function loadUserData() {
    try {
        const userFromStorage = JSON.parse(localStorage.getItem('user'));
        if (!userFromStorage || !userFromStorage.id) {
            console.error('User not found in localStorage or missing ID for API call.');
            // Optionally, redirect to login if this happens
            // window.location.href = '/login';
            return; // Stop if no user ID to fetch details for
        }

        // Fetch latest user data from server
        const response = await fetch(`/api/users/${userFromStorage.id}`);
        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                console.error('Unauthorized or Forbidden to fetch user data. Redirecting to login.');
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                window.location.href = '/login';
                return;
            }
            throw new Error(`Failed to fetch user data: ${response.status}`);
        }
        const latestUser = await response.json();
        localStorage.setItem('user', JSON.stringify(latestUser)); // Update localStorage with fresh data

        // Now use latestUser to populate fields
        const userNameElement = document.getElementById('userName');
        if (userNameElement) {
            userNameElement.textContent = latestUser.firstName + ' ' + latestUser.lastName;
            // Add REALTOR badge if applicable (logic from checkUserRole could be merged or called)
            if (latestUser.role === 'REALTOR' && !document.getElementById('realtorBadge')) {
                 userNameElement.innerHTML += ' <span id="realtorBadge" class="badge bg-primary">REALTOR</span>';
            }
        }
        
        const userEmailElement = document.getElementById('userEmail');
        if (userEmailElement) {
            userEmailElement.textContent = latestUser.email;
        }
        
        document.getElementById('firstName').value = latestUser.firstName || '';
        document.getElementById('lastName').value = latestUser.lastName || '';
        document.getElementById('email').value = latestUser.email || ''; // Email is readonly
        document.getElementById('phone').value = latestUser.phone || '';
        document.getElementById('address').value = latestUser.address || '';
        if (latestUser.birthDate) {
            document.getElementById('birthDate').value = latestUser.birthDate; // Assumes yyyy-MM-dd format
        }
        
        const avatarImg = document.getElementById('userAvatar');
        if (avatarImg && latestUser.avatarUrl) { // Assuming avatarUrl is part of UserDTO
            avatarImg.src = latestUser.avatarUrl;
        } else if (avatarImg) {
            avatarImg.src = '../assets/images/avatar-default.jpg'; // Default if no avatarUrl
        }
    } catch (error) {
        console.error('Lỗi khi tải dữ liệu người dùng:', error);
        showNotification('Không thể tải thông tin người dùng. Vui lòng thử lại.', 'error');
        // Potentially redirect to login or handle appropriately
    }
}

// Tải lịch sử giao dịch
async function loadTransactions() {
    try {
        const user = JSON.parse(localStorage.getItem('user'));
        if (!user) throw new Error('Chưa đăng nhập');
        
        const transactionsTableBody = document.getElementById('transactionsTableBody');
        if (!transactionsTableBody) {
            console.log('Bảng giao dịch không tồn tại trong DOM');
            return;
        }
        
        transactionsTableBody.innerHTML = '<tr><td colspan="7" class="text-center"><div class="spinner-border spinner-border-sm text-primary" role="status"></div> Đang tải dữ liệu...</td></tr>';
        
        try {
            const res = await fetch(`/transactions/api/user-transactions`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('token')}`,
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });
            
            if (!res.ok) {
                console.error('API error:', res.status, res.statusText);
                throw new Error(`API error: ${res.status} ${res.statusText}`);
            }
            
            const transactions = await res.json();
            transactionsTableBody.innerHTML = '';
            
            if (!transactions || transactions.length === 0) {
                transactionsTableBody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Bạn chưa có giao dịch nào</td></tr>';
                return;
            }
        
            transactions.forEach(item => { // Changed variable name from transaction to item to avoid confusion with item.transaction property
                try {
                    const row = document.createElement('tr');
                    
                    const propertyName = item.property?.name || item.property?.title || 'N/A';
                    
                    let displayAmount = 'N/A';
                    if (item.amount) {
                        displayAmount = item.amount.toLocaleString('vi-VN') + ' VNĐ';
                        if (item.requestType === 'RENTAL') {
                            displayAmount += '/tháng';
                        }
                    }

                    row.innerHTML = `
                        <td>${item.id || 'N/A'}</td>
                        <td>${propertyName}</td>
                        <td>${item.requestType === 'SALE' || item.requestType === 'PURCHASE' ? 'MUA BÁN' : (item.requestType === 'RENTAL' ? 'THUÊ' : 'N/A')}</td>
                        <td>${displayAmount}</td>
                        <td><span class="badge ${getStatusClass(item.status)}">${item.statusDisplay || item.status || 'N/A'}</span></td>
                        <td>${item.requestDate ? new Date(item.requestDate).toLocaleDateString('vi-VN') : 'N/A'}</td>
                    `;
                    
                    const actionCell = document.createElement('td');
                     // Link to the transaction detail page, using the request's ID (which is item.id from this API)
                    actionCell.innerHTML = `<a href="/transactions/${item.id}" class="btn btn-sm btn-primary">Xem chi tiết</a>`;
                    row.appendChild(actionCell);
                    
                    transactionsTableBody.appendChild(row);
                } catch (rowError) {
                    console.error('Error rendering transaction row:', rowError, item);
                }
            });
        } catch (fetchError) {
            console.error('Error fetching transactions:', fetchError);
            transactionsTableBody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center text-danger">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i>
                        Không thể tải dữ liệu giao dịch: ${fetchError.message}
                    </td>
                </tr>
            `;
        }
    } catch (error) {
        console.error('Lỗi khi tải lịch sử giao dịch:', error);
    }
}

// Tải danh sách yêu thích
async function loadFavorites() {
    try {
        const user = JSON.parse(localStorage.getItem('user'));
        if (!user) throw new Error('Chưa đăng nhập');
        
        // Tìm container hiển thị danh sách yêu thích - có thể khác nhau trong các template
        const favoritesContainer = document.getElementById('favorites');
        if (!favoritesContainer) {
            console.log('Container danh sách yêu thích không tồn tại trong DOM');
            return;
        }
        
        // Hiển thị thông báo đang tải
        favoritesContainer.innerHTML = '<div class="text-center"><div class="spinner-border text-primary" role="status"></div><p>Đang tải dữ liệu...</p></div>';
        
        try {
            const res = await fetch(`/api/wishlists/user/${user.id}`, {
                headers: {
                    'Authorization': 'Bearer ' + localStorage.getItem('token')
                }
            });
            
            if (res.status === 400) {
                // Có thể API chưa được triển khai hoặc có lỗi
                favoritesContainer.innerHTML = '<div class="alert alert-info">Chức năng đang được phát triển</div>';
                return;
            }
            
        if (!res.ok) throw new Error('Không thể lấy danh sách yêu thích');
            
        const favorites = await res.json();
            
            // Xóa dữ liệu cũ
            favoritesContainer.innerHTML = '';
            
            // Tạo row để chứa các card
            const row = document.createElement('div');
            row.className = 'row';
            favoritesContainer.appendChild(row);
            
            // Kiểm tra nếu không có yêu thích
            if (favorites.length === 0) {
                row.innerHTML = '<div class="col-12"><div class="alert alert-info">Bạn chưa có bất động sản yêu thích nào.</div></div>';
                return;
            }
            
            // Thêm dữ liệu mới
            favorites.forEach(property => {
                const col = document.createElement('div');
                col.className = 'col-md-4 mb-4';
                
                col.innerHTML = `
                    <div class="card h-100">
                        <img src="${property.imageUrl || '/assets/images/property-placeholder.jpg'}" class="card-img-top" alt="${property.name || 'Bất động sản'}">
                        <div class="card-body">
                            <h5 class="card-title">${property.name || 'Không có tiêu đề'}</h5>
                            <p class="card-text">${property.address || 'Không có địa chỉ'}</p>
                            <p class="card-text text-primary fw-bold">${property.price ? property.price.toLocaleString('vi-VN') + ' VNĐ' : 'Liên hệ'}</p>
                            <a href="/listing-detail/${property.id}" class="btn btn-primary btn-sm">Xem chi tiết</a>
                            <button class="btn btn-danger btn-sm remove-favorite" data-id="${property.id}">Xóa</button>
                        </div>
                    </div>
                `;
                
                row.appendChild(col);
                
                // Thêm event listener cho nút xóa
                const removeButton = col.querySelector('.remove-favorite');
                if (removeButton) {
                    removeButton.addEventListener('click', function() {
                        removeFavorite(this.getAttribute('data-id'));
                    });
                }
            });
        } catch (error) {
            // Hiển thị thông báo lỗi
            favoritesContainer.innerHTML = '<div class="alert alert-danger">Không thể tải danh sách yêu thích</div>';
            console.error('Lỗi khi tải danh sách yêu thích:', error);
        }
    } catch (error) {
        console.error('Lỗi khi tải danh sách yêu thích:', error);
    }
}

// Tải gợi ý BĐS cho USER
async function loadRecommendedProperties() {
    try {
        const user = JSON.parse(localStorage.getItem('user'));
        if (!user) throw new Error('Chưa đăng nhập');
        
        const res = await fetch(`/api/properties/recommendations/${user.id}`, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        });
        
        if (!res.ok) throw new Error('Không thể lấy gợi ý bất động sản');
        
        const properties = await res.json();
        const container = document.getElementById('recommendationsContainer');
        if (!container) return;
        
        // Xóa dữ liệu cũ
        container.innerHTML = '';
        
        // Kiểm tra nếu không có dữ liệu
        if (properties.length === 0) {
            container.innerHTML = '<div class="alert alert-info">Hiện không có gợi ý nào phù hợp với bạn.</div>';
            return;
        }
        
        // Thêm dữ liệu mới
        properties.forEach(property => {
            const col = document.createElement('div');
            col.className = 'col-md-4 mb-4';
            
            col.innerHTML = `
                <div class="card h-100">
                    <img src="${property.imageUrl || '/assets/images/property-placeholder.jpg'}" class="card-img-top" alt="${property.title}">
                    <div class="card-body">
                        <h5 class="card-title">${property.title}</h5>
                        <p class="card-text">${property.address}</p>
                        <p class="card-text text-primary fw-bold">${property.price.toLocaleString('vi-VN')} VNĐ</p>
                        <a href="/listing-detail/${property.id}" class="btn btn-primary btn-sm">Xem chi tiết</a>
                        <button class="btn btn-outline-primary btn-sm add-favorite" data-id="${property.id}">Yêu thích</button>
                    </div>
                </div>
            `;
            
            container.appendChild(col);
            
            // Thêm event listener cho nút yêu thích
            col.querySelector('.add-favorite').addEventListener('click', function() {
                addToFavorite(this.getAttribute('data-id'));
            });
        });
    } catch (error) {
        console.error('Lỗi khi tải gợi ý bất động sản:', error);
    }
}

// Tải danh sách các loại bất động sản và điền vào select
async function loadPropertyTypes() {
    const propertyTypeSelect = document.getElementById('propertyType');
    if (!propertyTypeSelect) return;

    // Giữ lại option "Chọn loại..."
    const firstOption = propertyTypeSelect.options[0];
    propertyTypeSelect.innerHTML = '';
    if (firstOption) {
        propertyTypeSelect.appendChild(firstOption);
    }

    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/property-types', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (!response.ok) {
            throw new Error('Failed to load property types');
        }
        const types = await response.json();
        types.forEach(type => {
            const option = new Option(type.name, type.id);
            propertyTypeSelect.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading property types:', error);
        showNotification('Không thể tải danh sách loại bất động sản.', 'danger');
    }
}

// Điền dữ liệu BĐS vào form khi sửa
async function populatePropertyForm(propertyId) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/properties/${propertyId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (!response.ok) {
            if (response.status === 404) throw new Error('Property not found');
            throw new Error('Failed to fetch property details');
        }
        const property = await response.json();

        document.getElementById('propertyName').value = property.name || '';
        document.getElementById('propertyType').value = property.propertyTypeId || '';
        document.getElementById('propertyPrice').value = property.price || '';
        document.getElementById('propertyArea').value = property.area || '';
        document.getElementById('propertyBedrooms').value = property.bedrooms || '';
        document.getElementById('propertyBathrooms').value = property.bathrooms || '';
        document.getElementById('propertyYearBuilt').value = property.yearBuilt || '';
        document.getElementById('propertyAddress').value = property.address || '';
        document.getElementById('propertyDescription').value = property.description || '';
        document.getElementById('propertyStatus').value = property.status || 'AVAILABLE';
        document.getElementById('propertyTransactionType').value = property.transactionType || 'SALE';
        
        // Hiển thị ảnh hiện có
        const existingImagesContainer = document.getElementById('existingImagesContainer');
        existingImagesContainer.innerHTML = ''; // Clear old images
        if (property.images && property.images.length > 0) {
            let imageHtml = '<p><strong>Ảnh hiện tại:</strong></p><div class="row">';
            property.images.forEach(img => {
                imageHtml += `
                    <div class="col-md-3 col-sm-4 mb-2 existing-image-item">
                        <img src="${img.imageUrl}" alt="Property image" class="img-thumbnail" style="width:100px; height:auto;">
                        <button type="button" class="btn btn-danger btn-sm mt-1 remove-existing-image-btn" data-image-id="${img.id}" data-property-id="${property.id}">Xóa</button>
                    </div>
                `;
            });
            imageHtml += '</div>';
            existingImagesContainer.innerHTML = imageHtml;

            // Gắn sự kiện cho nút xóa ảnh hiện có
            document.querySelectorAll('.remove-existing-image-btn').forEach(button => {
                button.addEventListener('click', async function() {
                    const imageIdToRemove = this.dataset.imageId;
                    const propId = this.dataset.propertyId;
                    await removeExistingImage(propId, imageIdToRemove, this.closest('.existing-image-item'));
                });
            });
        }

    } catch (error) {
        console.error('Error populating property form:', error);
        showNotification(`Lỗi tải thông tin BĐS: ${error.message}`, 'danger');
        // Đóng modal nếu có lỗi nghiêm trọng
        const modalInstance = bootstrap.Modal.getInstance(document.getElementById('addPropertyModal'));
        if (modalInstance) {
            modalInstance.hide();
        }
    }
}

async function removeExistingImage(propertyId, imageId, imageItemElement) {
    if (!confirm('Bạn có chắc muốn xóa ảnh này?')) return;
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/properties/${propertyId}/images/${imageId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Failed to delete image. Status: ${response.status}`);
        }
        showNotification('Xóa ảnh thành công.', 'success');
        if (imageItemElement) {
            imageItemElement.remove(); // Xóa phần tử ảnh khỏi DOM
        }
         // Optionally, re-populate or refresh part of the form if needed
    } catch (error) {
        console.error('Error deleting existing image:', error);
        showNotification(`Lỗi xóa ảnh: ${error.message}`, 'danger');
    }
}

// Cập nhật hàm loadRealtorProperties để hiển thị các nút Sửa/Xóa
async function loadRealtorProperties() {
    const propertiesTableBody = document.getElementById('propertiesTableBody');
    if (!propertiesTableBody) return;

    propertiesTableBody.innerHTML = '<tr><td colspan="7" class="text-center">Đang tải dữ liệu...</td></tr>';

    try {
        const user = JSON.parse(localStorage.getItem('user'));
        const token = localStorage.getItem('token');
        if (!user || !user.id || !token) {
            showNotification('Vui lòng đăng nhập lại.', 'warning');
            return;
        }

        // Chỉ realtor và admin mới có thể xem mục này
        if (user.role !== 'REALTOR' && user.role !== 'ADMIN') {
            propertiesTableBody.innerHTML = '<tr><td colspan="7" class="text-center">Bạn không có quyền xem mục này.</td></tr>';
            // Ẩn luôn tab link nếu chưa ẩn
            const propertiesTabLink = document.querySelector('a[href="#properties"].list-group-item');
            if (propertiesTabLink) propertiesTabLink.style.display = 'none';
            return;
        }
        
        const response = await fetch(`/api/properties/realtor/${user.id}`, {
                headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                 showNotification('Phiên đăng nhập hết hạn hoặc không có quyền. Vui lòng đăng nhập lại.', 'warning');
                 window.location.href = '/login'; // Redirect to login
                return;
            }
            throw new Error(`Failed to load properties. Status: ${response.status}`);
        }

        const properties = await response.json();

        if (properties.length === 0) {
            propertiesTableBody.innerHTML = '<tr><td colspan="7" class="text-center">Bạn chưa có bất động sản nào.</td></tr>';
                return;
            }

        let rows = '';
        properties.forEach(prop => {
            const imageUrl = prop.imageUrl || '../assets/images/property-placeholder.jpg';
            rows += `
                <tr>
                    <td>${prop.id}</td>
                    <td><img src="${imageUrl}" alt="${prop.name}" style="width: 100px; height: auto; object-fit: cover;"></td>
                    <td>${prop.name}</td>
                    <td>${prop.propertyTypeName || 'N/A'}</td>
                    <td>${prop.price ? prop.price.toLocaleString('vi-VN') + ' VNĐ' : 'N/A'}</td>
                    <td><span class="badge ${getPropertyStatusClass(prop.status)}">${prop.status || 'N/A'}</span></td>
                    <td>
                        <button class="btn btn-sm btn-info edit-property-btn" data-id="${prop.id}" data-bs-toggle="modal" data-bs-target="#addPropertyModal" title="Sửa">
                            <i class="bi bi-pencil-square"></i>
                        </button>
                        <button class="btn btn-sm btn-danger delete-property-btn" data-id="${prop.id}" title="Xóa">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                </tr>
            `;
        });
        propertiesTableBody.innerHTML = rows;

    } catch (error) {
        console.error('Error loading realtor properties:', error);
        propertiesTableBody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Lỗi tải danh sách BĐS: ${error.message}</td></tr>`;
        showNotification(`Lỗi tải danh sách BĐS: ${error.message}`, 'danger');
    }
}

// ... existing code ...
// async function handleAddProperty() { // Đổi tên thành saveProperty và cập nhật
async function saveProperty() {
    const propertyForm = document.getElementById('propertyForm');
    const propertyId = document.getElementById('propertyId').value;
    const token = localStorage.getItem('token');

    // Validate form (basic example, can be more extensive)
    if (!propertyForm.checkValidity()) {
        propertyForm.classList.add('was-validated');
        showNotification('Vui lòng điền đầy đủ các trường bắt buộc.', 'warning');
        return;
    }
    propertyForm.classList.remove('was-validated');


    let formData = new FormData(); // Sửa thành let để có thể gán lại trong trường hợp update
    formData.append('name', document.getElementById('propertyName').value);
    formData.append('propertyTypeId', document.getElementById('propertyType').value);
    formData.append('price', document.getElementById('propertyPrice').value);
    formData.append('area', document.getElementById('propertyArea').value);
    formData.append('address', document.getElementById('propertyAddress').value);
    formData.append('description', document.getElementById('propertyDescription').value);
    formData.append('status', document.getElementById('propertyStatus').value);
    formData.append('transactionType', document.getElementById('propertyTransactionType').value);
    
    const bedrooms = document.getElementById('propertyBedrooms').value;
    if (bedrooms) formData.append('bedrooms', bedrooms);
    
    const bathrooms = document.getElementById('propertyBathrooms').value;
    if (bathrooms) formData.append('bathrooms', bathrooms);

    const yearBuilt = document.getElementById('propertyYearBuilt').value;
    if (yearBuilt) formData.append('yearBuilt', yearBuilt);

    const imageFiles = document.getElementById('propertyImages').files;
    for (let i = 0; i < imageFiles.length; i++) {
        formData.append('images', imageFiles[i]);
    }
    
    // Đối với cập nhật, API sử dụng @RequestPart("propertyData") cho DTO và @RequestPart("images") cho files
    // Đối với thêm mới, API sử dụng các @RequestParam cho từng trường và List<MultipartFile> cho images
    // Chúng ta cần nhất quán hoặc điều chỉnh backend. Hiện tại, PropertyController.addProperty dùng @RequestParam,
    // PropertyController.updateProperty dùng @RequestPart("propertyData") là DTO và @RequestPart("images").
    // Để đơn giản hóa phía frontend, ta sẽ gửi các trường riêng lẻ cho cả add và update.
    // Backend cho update cần được điều chỉnh để nhận các trường riêng lẻ nếu nó mong muốn DTO.
    // Giả sử backend updateProperty cũng có thể nhận FormData với các trường riêng lẻ như add.
    // Nếu không, chúng ta cần gửi JSON cho propertyData và MultipartFile cho images riêng khi cập nhật.

    let url, method;
    const headers = {
        'Authorization': `Bearer ${token}`
        // 'Content-Type' không cần thiết khi dùng FormData, browser sẽ tự đặt
    };

    if (propertyId) { // Update
        // Nếu backend `updateProperty` yêu cầu một DTO JSON trong một part 'propertyData'
        // và images trong một part 'images', thì cách gửi FormData như hiện tại sẽ không hoạt động cho update.
        // Nó cần một cấu trúc như sau:
        // formData.append('propertyData', new Blob([JSON.stringify(propertyDataObject)], { type: 'application/json' }));
        // formData.append('images', imageFile);
        // Tuy nhiên, PropertyController.updateProperty dùng @ModelAttribute nên nó có thể xử lý FormData trực tiếp.
        // Nhưng endpoint /api/properties/update/{id} trong controller dùng @RequestPart PropertyUpdateRequestDTO
        // và @RequestPart(name = "images", required = false) List<MultipartFile>.
        // Điều này có nghĩa là chúng ta phải gửi một phần 'propertyData' là JSON và phần 'images' là files.

        // Giải pháp tạm thời: gửi như 'add' và hy vọng backend linh hoạt, hoặc backend cần được xem lại.
        // Để đơn giản, tôi sẽ giả định update có thể nhận các trường giống như add. Nếu không, cần thay đổi lớn ở đây hoặc backend.
        // Thực tế, PropertyController.updateProperty(Long id, PropertyDTO, MultipartFile) (dòng 56) nhận PropertyDTO, không phải PropertyUpdateRequestDTO.
        // Và nó là @ModelAttribute, có thể bind từ FormData.
        // Còn endpoint /api/properties/update/{id} (dòng 346) sử dụng @RequestPart PropertyUpdateRequestDTO. Đây là mâu thuẫn.
        
        // TÔI SẼ DÙNG ENDPOINT PUT /api/properties/{id} (dòng 56) vì nó dùng @ModelAttribute PropertyDTO
        // phù hợp hơn với việc gửi FormData đơn giản.
        url = `/api/properties/${propertyId}`;
        method = 'PUT';
        
        // PropertyDTO không có trường images, images được xử lý riêng bởi @RequestParam MultipartFile imageFile (chỉ 1 file?)
        // hoặc cần endpoint riêng để quản lý nhiều ảnh. Endpoint POST /{id}/images có vẻ phù hợp hơn cho việc thêm nhiều ảnh sau khi tạo/cập nhật property chính.

        // Vì `PropertyController.updateProperty` (dòng 56) với `@ModelAttribute PropertyDTO`
        // và `@RequestParam(required = false) MultipartFile imageFile` chỉ xử lý 1 file chính.
        // Còn `@PutMapping("/update/{id}")` (dòng 346) với `@RequestPart(name = "propertyData") @Valid PropertyUpdateRequestDTO propertyUpdateDTO, @RequestPart(name = "images", required = false) List<MultipartFile> images`
        // có vẻ phù hợp hơn cho kịch bản nhiều ảnh.
        // Tôi sẽ nhắm vào endpoint `/api/properties/update/{id}` (dòng 346).
        // Điều này yêu cầu gửi một phần 'propertyData' là JSON và phần 'images' là files.

        const propertyData = {
            name: formData.get('name'),
            propertyTypeId: parseInt(formData.get('propertyTypeId')),
            price: parseFloat(formData.get('price')),
            area: parseFloat(formData.get('area')),
            address: formData.get('address'),
            description: formData.get('description'),
            status: formData.get('status'),
            transactionType: formData.get('transactionType'),
            bedrooms: formData.get('bedrooms') ? parseInt(formData.get('bedrooms')) : null,
            bathrooms: formData.get('bathrooms') ? parseInt(formData.get('bathrooms')) : null,
            yearBuilt: formData.get('yearBuilt') ? parseInt(formData.get('yearBuilt')) : null,
            // Các trường khác từ PropertyUpdateRequestDTO nếu có
        };
        
        const updateFormData = new FormData();
        updateFormData.append('propertyData', new Blob([JSON.stringify(propertyData)], { type: 'application/json' }));
        
        const imageFilesForUpdate = document.getElementById('propertyImages').files;
        for (let i = 0; i < imageFilesForUpdate.length; i++) {
            updateFormData.append('images', imageFilesForUpdate[i]);
        }
        
        url = `/api/properties/update/${propertyId}`;
        method = 'PUT';
        // formData sẽ được thay bằng updateFormData
        formData = updateFormData; // Gán lại formData để dùng chung cho fetch

    } else { // Add new
        // Endpoint POST /api/properties/add (dòng 284) sử dụng @RequestParam cho từng field
        // và List<MultipartFile> cho images. FormData gửi từng field riêng lẻ là phù hợp.
        url = '/api/properties/add';
        method = 'POST';
        // headers['Content-Type'] = 'multipart/form-data'; // FormData tự xử lý
    }

    const saveButton = document.getElementById('saveProperty');
    const originalButtonText = saveButton.innerHTML;
    saveButton.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Đang lưu...`;
    saveButton.disabled = true;

    try {
        const response = await fetch(url, {
            method: method,
            headers: headers, // Headers không nên chứa Content-Type khi gửi FormData
            body: formData
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Lỗi không xác định. Status: ${response.status}` }));
            throw new Error(errorData.message || `Thao tác thất bại. Status: ${response.status}`);
        }

        const result = await response.json();
        showNotification(propertyId ? 'Cập nhật bất động sản thành công!' : 'Thêm bất động sản thành công!', 'success');
        
            const modalInstance = bootstrap.Modal.getInstance(document.getElementById('addPropertyModal'));
            if (modalInstance) {
                modalInstance.hide();
            }
        loadRealtorProperties(); // Tải lại danh sách

    } catch (error) {
        console.error('Error saving property:', error);
        showNotification(`Lỗi: ${error.message}`, 'danger');
    } finally {
        saveButton.innerHTML = originalButtonText;
        saveButton.disabled = false;
    }
}

// ... existing code ...
// async function deleteProperty(propertyId) { // Đảm bảo chỉ có 1 hàm deleteProperty, hoặc đổi tên thành deletePropertyHandler
async function deletePropertyHandler(propertyId) {
    if (!confirm('Bạn có chắc chắn muốn xóa bất động sản này? Hành động này không thể hoàn tác.')) {
        return;
    }
    const token = localStorage.getItem('token');
    try {
        // API endpoint /api/properties/delete/{id} (dòng 416 PropertyController)
        const response = await fetch(`/api/properties/delete/${propertyId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
             const errorData = await response.json().catch(() => ({ message: `Lỗi không xác định. Status: ${response.status}` }));
            throw new Error(errorData.message || `Xóa thất bại. Status: ${response.status}`);
        }
        
        // Không có content trả về từ API delete này ( ResponseEntity<?> softDeleteProperty)
        // Backend trả về ResponseEntity.ok().build() là không đúng cho DELETE (nên là noContent())
        // nhưng frontend vẫn có thể xử lý nếu response.ok là true.
        // Nếu API trả về JSON (ví dụ: { message: "Success" }), thì có thể dùng:
        // const result = await response.json();
        // showNotification(result.message || 'Xóa bất động sản thành công!', 'success');
        
        showNotification('Xóa bất động sản thành công!', 'success');
        loadRealtorProperties(); // Tải lại danh sách
    } catch (error) {
        console.error('Error deleting property:', error);
        showNotification(`Lỗi xóa bất động sản: ${error.message}`, 'danger');
    }
}

// ... existing code ...
// function getPropertyStatusClass(status) { // Đảm bảo hàm này tồn tại và đúng
// ... existing code ...
// function editProperty(id) { // Xóa hoặc hợp nhất hàm này vì đã có xử lý modal
// ... existing code ...
// async function editProperty(propertyId) { // Xóa hoặc hợp nhất hàm này
// ... existing code ...
// async function deleteProperty(propertyId) { // Xóa hàm trùng lặp, đã có deletePropertyHandler
// ... existing code ...

// Cần đảm bảo hàm getPropertyStatusClass tồn tại ở đâu đó trong file
// Nếu chưa có, thêm vào:
function getPropertyStatusClass(status) {
    if (!status) return 'bg-secondary';
    switch (status.toUpperCase()) {
        case 'AVAILABLE': return 'bg-success';
        case 'PENDING': return 'bg-warning text-dark';
        case 'SOLD': return 'bg-danger';
        case 'RENTED': return 'bg-info text-dark';
        case 'UNAVAILABLE': return 'bg-secondary';
        default: return 'bg-light text-dark';
    }
}

// Tải cài đặt thông báo (ví dụ)
function loadNotificationSettings() {
    // Lấy cài đặt từ localStorage hoặc API
    const emailNotifications = localStorage.getItem('emailNotifications') !== 'false'; // default true
    const smsNotifications = localStorage.getItem('smsNotifications') === 'true'; // default false
    const priceAlerts = localStorage.getItem('priceAlerts') !== 'false'; // default true
    const newListings = localStorage.getItem('newListings') !== 'false'; // default true

    const emailToggle = document.getElementById('emailNotifications');
    const smsToggle = document.getElementById('smsNotifications');
    const priceAlertsToggle = document.getElementById('priceAlerts');
    const newListingsToggle = document.getElementById('newListings');

    if (emailToggle) emailToggle.checked = emailNotifications;
    if (smsToggle) smsToggle.checked = smsNotifications;
    if (priceAlertsToggle) priceAlertsToggle.checked = priceAlerts;
    if (newListingsToggle) newListingsToggle.checked = newListings;

    const notificationsForm = document.getElementById('notificationsForm');
    if (notificationsForm) {
        notificationsForm.addEventListener('submit', saveNotificationSettings);
    }
}

// Lưu cài đặt thông báo (ví dụ)
async function saveNotificationSettings(event) {
    event.preventDefault();
    const emailNotifications = document.getElementById('emailNotifications').checked;
    const smsNotifications = document.getElementById('smsNotifications').checked;
    const priceAlerts = document.getElementById('priceAlerts').checked;
    const newListings = document.getElementById('newListings').checked;

    // Lưu vào localStorage (hoặc gửi lên API)
    localStorage.setItem('emailNotifications', emailNotifications);
    localStorage.setItem('smsNotifications', smsNotifications);
    localStorage.setItem('priceAlerts', priceAlerts);
    localStorage.setItem('newListings', newListings);

    // Ví dụ gọi API (nếu có)
    // try {
    //     const token = localStorage.getItem('token');
    //     const response = await fetch('/api/users/notification-settings', {
    //         method: 'POST',
    //         headers: {
    //             'Content-Type': 'application/json',
    //             'Authorization': `Bearer ${token}`
    //         },
    //         body: JSON.stringify({ emailNotifications, smsNotifications, priceAlerts, newListings })
    //     });
    //     if (!response.ok) throw new Error('Failed to save settings');
    //     showNotification('Cài đặt thông báo đã được lưu.', 'success');
    // } catch (error) {
    //     showNotification('Lỗi lưu cài đặt thông báo.', 'danger');
    //     console.error('Error saving notification settings:', error);
    // }
    showNotification('Cài đặt thông báo đã được lưu (local).', 'success');
}

// Hàm validate form profile
function validateProfileForm(formData) {
    let isValid = true;
    // Basic validation examples (can be enhanced)
    if (!formData.firstName || formData.firstName.trim() === '') {
        showNotification('Họ không được để trống.', 'warning');
        document.getElementById('firstName').classList.add('is-invalid');
        isValid = false;
    } else {
        document.getElementById('firstName').classList.remove('is-invalid');
    }

    if (!formData.lastName || formData.lastName.trim() === '') {
        showNotification('Tên không được để trống.', 'warning');
        document.getElementById('lastName').classList.add('is-invalid');
        isValid = false;
    } else {
        document.getElementById('lastName').classList.remove('is-invalid');
    }

    if (!formData.phone || formData.phone.trim() === '') {
        showNotification('Số điện thoại không được để trống.', 'warning');
        document.getElementById('phone').classList.add('is-invalid');
        isValid = false;
    } else {
        document.getElementById('phone').classList.remove('is-invalid'); 
    }
    return isValid;
}

// Cập nhật thông tin cá nhân
async function updateProfile(e) {
    e.preventDefault();
    const profileForm = e.target;
    const submitButton = profileForm.querySelector('button[type="submit"]');
    const originalButtonText = submitButton.innerHTML;
    submitButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Đang cập nhật...';
    submitButton.disabled = true;

    profileForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));

        const formData = {
            firstName: document.getElementById('firstName').value.trim(),
            lastName: document.getElementById('lastName').value.trim(),
            phone: document.getElementById('phone').value.trim(),
            address: document.getElementById('address').value.trim(),
        birthDate: document.getElementById('birthDate').value || null,
        };

    if (!validateProfileForm(formData)) {
        submitButton.innerHTML = originalButtonText;
        submitButton.disabled = false;
            return;
        }

    try {
        const user = JSON.parse(localStorage.getItem('user'));
        if (!user || !user.id) {
            showNotification('Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.', 'danger');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/users/${user.id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify(formData)
        });

        if (response.ok) {
            const updatedUser = await response.json();
            localStorage.setItem('user', JSON.stringify(updatedUser));
            
            const userNameElement = document.getElementById('userName');
            if (userNameElement) {
                let newName = updatedUser.firstName + ' ' + updatedUser.lastName;
                const roleBadge = userNameElement.querySelector('#roleBadge');
                userNameElement.textContent = newName; // Set text content first
                if (roleBadge) { // Then re-append badge
                    userNameElement.appendChild(document.createTextNode(' ')); // Add space before badge
                    userNameElement.appendChild(roleBadge);
                }
            }
            showNotification('Thông tin cá nhân đã được cập nhật thành công!', 'success');
            } else {
            const errorData = await response.json().catch(() => ({ message: 'Cập nhật thất bại. Vui lòng thử lại.' }));
            let errorMessage = errorData.message || `Lỗi ${response.status}.`;
            if (errorData.errors) {
                errorMessage = errorData.errors.map(err => `${err.field}: ${err.message}`).join('\n');
            } else if (response.status === 400 && errorData.message && errorData.message.toLowerCase().includes("phone number already exists")){
                errorMessage = "Số điện thoại này đã được sử dụng. Vui lòng chọn số khác.";
                document.getElementById('phone').classList.add('is-invalid');
            }
            showNotification(`Lỗi cập nhật: ${errorMessage}`, 'danger');
        }
    } catch (error) {
        console.error('Lỗi khi cập nhật thông tin cá nhân:', error);
        showNotification('Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.', 'danger');
    } finally {
        submitButton.innerHTML = originalButtonText;
        submitButton.disabled = false;
    }
}

// Thay đổi mật khẩu
async function changePassword(e) {
    e.preventDefault();
    const securityForm = document.getElementById('securityForm');
    const submitButton = document.getElementById('securitySubmit');
    const submitButtonText = document.getElementById('securitySubmitText');
    const loadingSpinner = document.getElementById('securityLoadingSpinner');

    const originalButtonHTML = submitButtonText.innerHTML;
    submitButtonText.classList.add('d-none');
    loadingSpinner.classList.remove('d-none');
    submitButton.disabled = true;

    document.getElementById('currentPassword').classList.remove('is-invalid');
    document.getElementById('newPassword').classList.remove('is-invalid');
    document.getElementById('confirmPassword').classList.remove('is-invalid');
    
    try {
        const user = JSON.parse(localStorage.getItem('user'));
         if (!user || !user.id) { // Check user and user.id
            showNotification('Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.', 'danger');
            throw new Error('User not found for password change');
        }
        
        const currentPassword = document.getElementById('currentPassword').value;
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        
        let validationPassed = true;
        if (!currentPassword) {
            document.getElementById('currentPassword').classList.add('is-invalid');
            showNotification('Vui lòng nhập mật khẩu hiện tại.', 'warning');
            validationPassed = false;
        }
        if (!newPassword) { // Basic check, pattern is in HTML
            document.getElementById('newPassword').classList.add('is-invalid');
             document.getElementById('newPasswordFeedback').textContent = 'Vui lòng nhập mật khẩu mới.';
            showNotification('Vui lòng nhập mật khẩu mới.', 'warning');
            validationPassed = false;
        } else if (!newPassword.match(/^(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$/)) {
             document.getElementById('newPassword').classList.add('is-invalid');
             document.getElementById('newPasswordFeedback').textContent = 'Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số.';
             showNotification('Mật khẩu mới không đúng định dạng.', 'warning');
            validationPassed = false;
        }


        if (newPassword !== confirmPassword) {
            document.getElementById('confirmPassword').classList.add('is-invalid');
            document.getElementById('confirmPasswordFeedback').textContent = 'Mật khẩu xác nhận không khớp.';
            showNotification('Mật khẩu xác nhận không khớp.', 'warning');
            validationPassed = false;
        }

        if (!validationPassed) {
            throw new Error('Validation failed for password change');
    }
        
        const res = await fetch('/api/auth/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                 'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify({ 
                oldPassword: currentPassword, 
                newPassword: newPassword 
            })
        });
        
        const responseBodyText = await res.text();

        if (res.ok) {
            if (securityForm) securityForm.reset();
            showNotification(responseBodyText || 'Thay đổi mật khẩu thành công!', 'success');
        } else {
            let errorMessage = responseBodyText;
            try {
                const errorJson = JSON.parse(responseBodyText); 
                errorMessage = errorJson.message || responseBodyText; 
            } catch (parseError) {
                // responseBodyText is not JSON, use it as is or default message
                if (!errorMessage) errorMessage = `Lỗi ${res.status}. Không thể thay đổi mật khẩu.`;
            }
            
             if (res.status === 400) {
                if (errorMessage.toLowerCase().includes('invalid old password') || errorMessage.toLowerCase().includes('mật khẩu cũ không đúng')) {
                    document.getElementById('currentPassword').classList.add('is-invalid');
                    showNotification('Mật khẩu hiện tại không đúng.', 'danger');
                } else {
                    showNotification(`Lỗi: ${errorMessage}`, 'danger');
                }
            } else if (res.status === 401) {
                 showNotification('Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.', 'danger');
            } else {
                showNotification(errorMessage, 'danger');
            }
        }
    } catch (error) {
        // Avoid showing generic message if validation failed, as specific messages were already shown
        if (error.message && error.message !== 'Validation failed for password change' && error.message !== 'User not found for password change') {
            showNotification(error.message || 'Không thể xử lý yêu cầu đổi mật khẩu.', 'danger');
        }
    } finally {
        if (submitButtonText && loadingSpinner && submitButton) {
            submitButtonText.classList.remove('d-none');
            loadingSpinner.classList.add('d-none');
            submitButton.disabled = false;
        }
    }
}

// Tải lên avatar
async function uploadAvatar() {
    const avatarInput = document.getElementById('avatarInput');
    const file = avatarInput.files[0];
    
    const saveAvatarButton = document.getElementById('saveAvatar');
    const avatarSubmitText = document.getElementById('avatarSubmitText');
    const avatarLoadingSpinner = document.getElementById('avatarLoadingSpinner');
    const originalButtonHTML = avatarSubmitText.innerHTML; // Correctly get the HTML content

    if (!file) {
        showNotification('Vui lòng chọn một file ảnh.', 'warning');
        document.getElementById('avatarInput').classList.add('is-invalid');
        return;
    }
     // Validate again before upload
    if (!file.type.startsWith('image/')) {
        showNotification('File đã chọn không phải là ảnh.', 'danger');
        document.getElementById('avatarInput').classList.add('is-invalid');
        return;
    }
    if (file.size > 2 * 1024 * 1024) { // 2MB
        showNotification('Kích thước file ảnh quá lớn (tối đa 2MB).', 'danger');
        document.getElementById('avatarInput').classList.add('is-invalid');
        return;
    }

    avatarSubmitText.classList.add('d-none');
    avatarLoadingSpinner.classList.remove('d-none');
    saveAvatarButton.disabled = true;

    const formData = new FormData();
    formData.append('avatarFile', file);

    try {
        const response = await fetch('/api/users/avatar', { // Endpoint này cần user ID từ Principal ở backend
            method: 'POST',
            headers: {
                 'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: formData
        });

        if (response.ok) {
            const updatedUser = await response.json(); 
            // Cập nhật user trong localStorage với avatarUrl mới
            const currentUserData = JSON.parse(localStorage.getItem('user'));
            currentUserData.avatarUrl = updatedUser.avatarUrl; // UserDTO trả về nên có avatarUrl
            localStorage.setItem('user', JSON.stringify(currentUserData));

            document.getElementById('userAvatar').src = updatedUser.avatarUrl;
            // avatarPreview trong modal cũng nên được cập nhật, nhưng modal sẽ đóng

            showNotification('Ảnh đại diện đã được cập nhật thành công!', 'success');
            
            const modalInstance = bootstrap.Modal.getInstance(document.getElementById('changeAvatarModal'));
            if (modalInstance) {
                modalInstance.hide();
            }
        } else {
            const errorData = await response.json().catch(() => ({ message: 'Không thể tải lên ảnh đại diện.' }));
            showNotification(`Lỗi: ${errorData.message || response.statusText}`, 'danger');
        }
    } catch (error) {
        console.error('Lỗi khi tải lên ảnh đại diện:', error);
        showNotification('Đã xảy ra lỗi không mong muốn khi tải lên ảnh.', 'danger');
    } finally {
        avatarSubmitText.innerHTML = originalButtonHTML; // Restore original text/HTML
        avatarSubmitText.classList.remove('d-none');
        avatarLoadingSpinner.classList.add('d-none');
        saveAvatarButton.disabled = false;
    }
}

// Function to load transaction requests for REALTOR
async function loadRealtorTransactionRequests() {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (user.role !== 'REALTOR' && user.role !== 'ADMIN') {
        const container = document.getElementById('realtorTransactionRequestsTableContainer');
        if (container) container.style.display = 'none';
        return;
    }

    const tableBody = document.getElementById('realtorTransactionRequestsTableBody');
    const container = document.getElementById('realtorTransactionRequestsTableContainer');
    const noRequestsMessage = document.getElementById('noRealtorRequests');

    if (!tableBody || !container || !noRequestsMessage) {
        console.error("Realtor transaction requests table elements not found in DOM.");
        return;
    }

    tableBody.innerHTML = '<tr><td colspan="7" class="text-center"><div class="spinner-border spinner-border-sm text-primary" role="status"></div> Đang tải...</td></tr>';
    container.style.display = 'block'; // Hiển thị container trước khi tải
    noRequestsMessage.style.display = 'none';


    try {
        // Endpoint này cần được kiểm tra lại, giả sử là /api/transactions/realtor 
        // hoặc một endpoint tương tự trả về TransactionRequestSummaryDTO
        const response = await fetch('/transactions/api/realtor-requests', { 
             headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });
        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                showNotification('Bạn không có quyền truy cập mục này.', 'warning');
                 if (container) container.style.display = 'none';
                if (noRequestsMessage) {
                noRequestsMessage.style.display = 'block';
                    noRequestsMessage.textContent = 'Không có quyền truy cập hoặc không có yêu cầu.';
                }
                return;
            }
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Error fetching realtor transaction requests: ${response.status}`);
        }
        const requests = await response.json();
        tableBody.innerHTML = ''; 

        if (requests && requests.length > 0) {
            if (container) container.style.display = 'block';
            if (noRequestsMessage) noRequestsMessage.style.display = 'none';
            
            requests.forEach(req => {
                const row = tableBody.insertRow();
                row.insertCell().textContent = req.id; // Assuming TransactionRequestSummaryDTO has id
                row.insertCell().textContent = req.propertyName || 'N/A'; // propertyName
                row.insertCell().textContent = req.requestTypeDisplay || req.requestType || 'N/A'; // requestTypeDisplay (e.g., "Mua", "Thuê")
                row.insertCell().textContent = req.customerName || 'N/A'; // customerName
                row.insertCell().textContent = req.requestDate ? new Date(req.requestDate).toLocaleDateString('vi-VN') : 'N/A'; // requestDate
                
                const statusCell = row.insertCell();
                const statusBadge = document.createElement('span');
                // getStatusClass cho RequestStatus (PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED)
                statusBadge.className = `badge ${getStatusClass(req.status)}`; 
                statusBadge.textContent = req.statusDisplay || req.status; // statusDisplay (e.g. "Chờ xử lý")
                statusCell.appendChild(statusBadge);

                const actionsCell = row.insertCell();
                 // Link đến trang chi tiết yêu cầu, ví dụ /transactions/request/{id}
                actionsCell.innerHTML = `<a href="/transactions/TR${req.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i> Xem</a>`; 
            });
        } else {
            if (container) container.style.display = 'block'; // Vẫn hiển thị bảng nhưng trống
            if (noRequestsMessage) {
            noRequestsMessage.style.display = 'block';
                 noRequestsMessage.textContent = 'Không có yêu cầu giao dịch nào.';
            }
            tableBody.innerHTML = '<tr><td colspan="7" class="text-center">Không có yêu cầu giao dịch nào.</td></tr>';
        }
    } catch (error) {
        console.error('Error loading realtor transaction requests:', error);
        showNotification(`Không thể tải danh sách yêu cầu giao dịch: ${error.message}`, 'danger');
        if (container) container.style.display = 'block';
        if (noRequestsMessage) {
        noRequestsMessage.style.display = 'block';
        noRequestsMessage.textContent = 'Lỗi khi tải yêu cầu.';
        }
         if (tableBody) tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Lỗi khi tải yêu cầu: ${error.message}</td></tr>`;
    }
}

// Hiển thị thông báo (Toast)
function showNotification(message, type = 'info') { // success, info, warning, danger
    const alertContainer = document.getElementById('alert-container');
    if (!alertContainer) {
        console.warn('Alert container not found. Cannot show notification:', message);
        alert(message); // Fallback to simple alert
        return;
    }

    const wrapper = document.createElement('div');
    wrapper.innerHTML = [
        `<div class="toast align-items-center text-white bg-${type} border-0" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="5000">`,
        '  <div class="d-flex">',
        `    <div class="toast-body">${message}</div>`,
        '    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>',
        '  </div>',
        '</div>'
    ].join('');
    const toastEl = wrapper.firstChild;
    alertContainer.appendChild(toastEl);
    
    const toast = new bootstrap.Toast(toastEl);
    toast.show();

    toastEl.addEventListener('hidden.bs.toast', function () {
        toastEl.remove();
    });
}

// Lấy class cho trạng thái GIAO DỊCH (TransactionStatus) hoặc YÊU CẦU (RequestStatus)
function getStatusClass(status) {
    if (!status) return 'bg-secondary';
    switch (status.toUpperCase()) {
        // TransactionStatus & RequestStatus
        case 'COMPLETED':
        case 'APPROVED': // For requests
            return 'bg-success';
        case 'PENDING':
            return 'bg-warning text-dark';
        case 'CANCELLED':
        case 'REJECTED': // For requests
            return 'bg-danger';
        // Property specific statuses are handled by getPropertyStatusClass
        default:
            return 'bg-secondary';
    }
}

// Kích hoạt tab dựa trên hash trong URL
function activateTabFromHash() {
    const hash = window.location.hash;
    if (hash) {
        const tabTrigger = document.querySelector(`.list-group-item[href="${hash}"]`);
        if (tabTrigger) {
            // Programmatically click the tab. Bootstrap's tab JS will handle the rest.
            // This ensures that the 'shown.bs.tab' event fires if other code relies on it.
            // It also correctly sets the 'active' class on tab and pane.
            tabTrigger.click(); 
            
            // Scroll to top after tab change, if needed, but usually handled by browser
            // setTimeout(() => {
            //     window.scrollTo(0, 0); 
            // }, 100);
        }
        } else {
        // If no hash, ensure the default active tab (e.g., #profile) is indeed shown
        // This is typically handled by Bootstrap if 'active' class is set in HTML
        const defaultActiveTab = document.querySelector('.list-group-item.active[data-bs-toggle="list"]');
        if (defaultActiveTab && !document.querySelector(defaultActiveTab.getAttribute('href')).classList.contains('show')) {
             defaultActiveTab.click(); // Activate if somehow not shown
        }
    }
}

// Xóa khỏi danh sách yêu thích
async function removeFavorite(propertyId) {
    if (!confirm('Bạn có chắc muốn xóa bất động sản này khỏi danh sách yêu thích?')) return;
    try {
        const token = localStorage.getItem('token');
        // Changed to POST and added a JSON body as expected by WishlistController @PostMapping("/remove")
        const response = await fetch(`/api/wishlists/remove`, { 
            method: 'POST', // Changed from DELETE to POST
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json' // Added Content-Type for JSON body
            },
            body: JSON.stringify({ propertyId: parseInt(propertyId) }) // Send propertyId in the body
        });
        if (!response.ok) {
            // Try to parse error message from backend, otherwise use a generic one
            let errorMessage = 'Không thể xóa khỏi danh sách yêu thích';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorMessage;
            } catch (e) {
                // Failed to parse JSON, use status text or generic message
                errorMessage = response.statusText || errorMessage;
            }
            throw new Error(errorMessage);
        }
        showNotification('Đã xóa khỏi danh sách yêu thích.', 'success');
        if (document.querySelector('.list-group-item[href="#favorites"].active')) {
             loadFavorites(); // Tải lại nếu đang ở tab yêu thích
        }
    } catch (error) {
        console.error('Lỗi khi xóa khỏi danh sách yêu thích:', error);
        showNotification(error.message || 'Không thể xóa khỏi danh sách yêu thích.', 'danger');
    }
}

// Thêm vào danh sách yêu thích (ví dụ, nếu có nút "Yêu thích" ở đâu đó trên trang account)
async function addToFavorite(propertyId) {
    try {
        const token = localStorage.getItem('token');
        // API endpoint for adding might be POST /api/wishlists (with body { propertyId })
        // or POST /api/wishlists/add/{propertyId}
        const response = await fetch(`/api/wishlists/add/${propertyId}`, { 
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                // 'Content-Type': 'application/json' // if sending body
            },
            // body: JSON.stringify({ propertyId: propertyId }) // if API expects body
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Không thể thêm vào danh sách yêu thích');
        }
        showNotification('Đã thêm vào danh sách yêu thích!', 'success');
        if (document.querySelector('.list-group-item[href="#favorites"].active')) {
            loadFavorites(); // Tải lại nếu đang ở tab yêu thích
        }
    } catch (error) {
        console.error('Lỗi khi thêm vào danh sách yêu thích:', error);
        showNotification(error.message || 'Không thể thêm vào danh sách yêu thích.', 'danger');
    }
}

// ... (các hàm quản lý BĐS đã thêm ở bước trước: loadPropertyTypes, populatePropertyForm, removeExistingImage, loadRealtorProperties, saveProperty, deletePropertyHandler, getPropertyStatusClass)
// Chúng nên nằm ở đây hoặc được tổ chức một cách hợp lý.

// Ví dụ, hàm getPropertyStatusClass (đã có ở edit trước)
// function getPropertyStatusClass(status) {
//     if (!status) return 'bg-secondary';
//     switch (status.toUpperCase()) {
//         case 'AVAILABLE': return 'bg-success';
//         case 'PENDING': return 'bg-warning text-dark';
//         case 'SOLD': return 'bg-danger';
//         case 'RENTED': return 'bg-info text-dark';
//         case 'UNAVAILABLE': return 'bg-secondary';
//         default: return 'bg-light text-dark';
//     }
// }

// Đảm bảo các hàm còn lại của file (nếu có) được giữ nguyên bên dưới
// ... existing code ...
