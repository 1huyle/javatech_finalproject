// Khởi tạo bản đồ
function initMap(latitude, longitude, address) {
    // Tạo map với vị trí mặc định
    const map = new google.maps.Map(document.getElementById('map'), {
        center: { lat: latitude, lng: longitude },
        zoom: 15,
        styles: [
            {
                "featureType": "poi",
                "elementType": "labels",
                "stylers": [{ "visibility": "off" }]
            }
        ]
    });

    // Thêm marker
    const marker = new google.maps.Marker({
        position: { lat: latitude, lng: longitude },
        map: map,
        title: address
    });

    // Thêm info window
    const infoWindow = new google.maps.InfoWindow({
        content: `<div class="p-2"><strong>${address}</strong></div>`
    });

    // Hiển thị info window khi click vào marker
    marker.addListener('click', () => {
        infoWindow.open(map, marker);
    });

    // Hiển thị info window ngay lập tức
    infoWindow.open(map, marker);
}

// Lấy tọa độ từ địa chỉ
async function getCoordinates(address) {
    try {
        const response = await fetch(`https://maps.googleapis.com/maps/api/geocode/json?address=${encodeURIComponent(address)}&key=YOUR_API_KEY`);
        const data = await response.json();
        
        if (data.status === 'OK') {
            const location = data.results[0].geometry.location;
            return {
                lat: location.lat,
                lng: location.lng
            };
        }
        throw new Error('Không thể tìm thấy địa chỉ');
    } catch (error) {
        console.error('Lỗi khi lấy tọa độ:', error);
        return null;
    }
}

// Tìm đường đi
function getDirections(destination) {
    const origin = new google.maps.LatLng(10.762622, 106.660172); // Vị trí hiện tại (có thể lấy từ geolocation)
    
    const directionsService = new google.maps.DirectionsService();
    const directionsRenderer = new google.maps.DirectionsRenderer();
    
    const map = new google.maps.Map(document.getElementById('map'), {
        zoom: 12,
        center: origin
    });
    
    directionsRenderer.setMap(map);
    
    const request = {
        origin: origin,
        destination: destination,
        travelMode: google.maps.TravelMode.DRIVING
    };
    
    directionsService.route(request, (result, status) => {
        if (status === 'OK') {
            directionsRenderer.setDirections(result);
        } else {
            console.error('Lỗi khi tìm đường đi:', status);
        }
    });
}

// Lấy vị trí hiện tại
function getCurrentLocation() {
    return new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
            reject(new Error('Trình duyệt không hỗ trợ định vị'));
            return;
        }
        
        navigator.geolocation.getCurrentPosition(
            (position) => {
                resolve({
                    lat: position.coords.latitude,
                    lng: position.coords.longitude
                });
            },
            (error) => {
                reject(error);
            }
        );
    });
}

// Tính khoảng cách
function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // Bán kính trái đất (km)
    const dLat = deg2rad(lat2 - lat1);
    const dLon = deg2rad(lon2 - lon1);
    const a = 
        Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
        Math.sin(dLon/2) * Math.sin(dLon/2); 
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
    const distance = R * c; // Khoảng cách (km)
    return distance;
}

function deg2rad(deg) {
    return deg * (Math.PI/180);
} 