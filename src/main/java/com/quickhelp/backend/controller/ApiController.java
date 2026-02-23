package com.quickhelp.backend.controller;

import com.quickhelp.backend.model.Booking;
import com.quickhelp.backend.model.Provider;
import com.quickhelp.backend.model.Service;
import com.quickhelp.backend.repository.BookingRepository;
import com.quickhelp.backend.repository.ProviderRepository;
import com.quickhelp.backend.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow Flutter web/emulator to access
public class ApiController {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private com.quickhelp.backend.repository.ProviderRequestRepository providerRequestRepository;



    @Autowired
    private com.quickhelp.backend.repository.UserRepository userRepository;

    @GetMapping("/services")
    public List<Service> getServices() {
        List<Service> services = serviceRepository.findAll();
        for (Service s : services) {
            s.setProviderCount(providerRepository.countByServiceTypeIgnoreCase(s.getName()));
        }
        return services;
    }

    @GetMapping("/bookings")
    public List<Booking> getBookings(@RequestParam(name = "userId", required = false) Long userId) {
        if (userId != null) {
            return bookingRepository.findByUserId(userId);
        }
        return bookingRepository.findAll();
    }

    @GetMapping("/bookings/provider/{providerId}")
    public List<Booking> getProviderBookings(@PathVariable Long providerId) {
        return bookingRepository.findByProviderId(providerId);
    }

    @PostMapping("/bookings")
    public Booking createBooking(@RequestBody Booking booking) {
        if (booking.getStatus() == null) {
            booking.setStatus("PENDING");
        }
        return bookingRepository.save(booking);
    }

    @PutMapping("/bookings/{id}/accept")
    public org.springframework.http.ResponseEntity<?> acceptBooking(@PathVariable Long id) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus("ACCEPTED");
            bookingRepository.save(booking);
            return org.springframework.http.ResponseEntity.ok(booking);
        }).orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    @PutMapping("/bookings/{id}/decline")
    public org.springframework.http.ResponseEntity<?> declineBooking(@PathVariable Long id) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus("DECLINED");
            bookingRepository.save(booking);
            return org.springframework.http.ResponseEntity.ok(booking);
        }).orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    @PutMapping("/bookings/{id}/status")
    public org.springframework.http.ResponseEntity<?> updateBookingStatus(@PathVariable Long id, @RequestParam("status") String status) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus(status);
            bookingRepository.save(booking);
            return org.springframework.http.ResponseEntity.ok(booking);
        }).orElse(org.springframework.http.ResponseEntity.notFound().build());
    }
    
    @GetMapping("/admin/users")
    public List<com.quickhelp.backend.model.User> getAllUsers() {
        return userRepository.findAll();
    }

    // Temporary endpoint to promote user to admin (for development)
    @PostMapping("/admin/promote/{username}")
    public org.springframework.http.ResponseEntity<?> promoteToAdmin(@PathVariable String username) {
        return userRepository.findByUsername(username).map(user -> {
            user.setRole("ADMIN");
            userRepository.save(user);
            return org.springframework.http.ResponseEntity.ok("User promoted to ADMIN");
        }).orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    @GetMapping("/providers")
    public List<Provider> getProviders(
            @RequestParam(name = "serviceType", required = false) String serviceType,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "radius", defaultValue = "50") Double radius) { // Default 50km
        
        if (lat != null && lng != null) {
             List<Provider> nearby = providerRepository.findNearbyProviders(lat, lng, radius);
             // Verify this logic: if serviceType is also present, we should filter the nearby list
             if (serviceType != null && !serviceType.isEmpty()) {
                 return nearby.stream()
                         .filter(p -> p.getServiceType().equalsIgnoreCase(serviceType))
                         .collect(java.util.stream.Collectors.toList());
             }
             return nearby;
        }

        if (serviceType != null && !serviceType.isEmpty()) {
            return providerRepository.findByServiceTypeIgnoreCase(serviceType);
        }
        return providerRepository.findAll();
    }

    @PostMapping("/providers")
    public Provider createProvider(@RequestBody Provider provider) {
        return providerRepository.save(provider);
    }

    @PutMapping("/provider-profile/{id}")
    public org.springframework.http.ResponseEntity<?> updateProviderProfile(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "serviceType", required = false) String serviceType,
            @RequestParam(value = "price", required = false) String price
    ) {
        return providerRepository.findById(id).map(provider -> {
            if (name != null) provider.setName(name);
            if (phone != null) provider.setPhone(phone);
            if (email != null) provider.setEmail(email);
            if (serviceType != null) provider.setServiceType(serviceType);
            if (price != null) provider.setPrice(price);

            try {
                if (file != null && !file.isEmpty()) {
                    provider.setPhotoData(file.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            providerRepository.save(provider);
            return org.springframework.http.ResponseEntity.ok(provider);
        }).orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    @GetMapping("/providers/search-location")
    public org.springframework.http.ResponseEntity<List<Provider>> searchProvidersByLocation(
            @RequestParam(value = "lat", required = true) Double lat,
            @RequestParam(value = "lng", required = true) Double lng,
            @RequestParam(value = "radiusKm", defaultValue = "10.0") Double radiusKm,
            @RequestParam(value = "serviceType", required = false) String serviceType,
            @RequestParam(value = "gender", required = false) String gender
    ) {
        // Fetch providers. For a massive production database, we'd use PostGIS, 
        // but for QuickHelp we'll filter in-memory which is perfectly fast for a few thousand rows.
        List<Provider> allProviders = providerRepository.findAll();

        List<Provider> withinRadius = allProviders.stream()
                .filter(p -> p.getLat() != null && p.getLng() != null) // Must have coordinates
                .filter(p -> serviceType == null || serviceType.isEmpty() || p.getServiceType().equalsIgnoreCase(serviceType))
                .filter(p -> gender == null || gender.isEmpty() || "Any".equalsIgnoreCase(gender) || gender.equalsIgnoreCase(p.getGender()))
                .filter(p -> calculateHaversineDistance(lat, lng, p.getLat(), p.getLng()) <= radiusKm)
                .sorted(Comparator.comparingDouble(p -> calculateHaversineDistance(lat, lng, p.getLat(), p.getLng()))) // Sort strictly by closest first
                .collect(Collectors.toList());

        return org.springframework.http.ResponseEntity.ok(withinRadius);
    }

    // Haversine formula to calculate distance between two lat/lng coordinates in kilometers
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; 
    }

    @PostMapping("/provider-requests")
    public org.springframework.http.ResponseEntity<String> submitProviderRequest(
            @RequestParam("name") String name,
            @RequestParam("serviceType") String serviceType,
            @RequestParam("description") String description,
            @RequestParam("location") String location,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "gender", required = false) String gender,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        
        // Check for duplicates
        if (email != null && !email.isEmpty() && providerRepository.findByIdentifier(email).isPresent()) {
            return org.springframework.http.ResponseEntity.badRequest().body("Error: Email already registered");
        }
        if (providerRepository.findByIdentifier(phoneNumber).isPresent()) {
            return org.springframework.http.ResponseEntity.badRequest().body("Error: Phone number already registered");
        }

        try {
            com.quickhelp.backend.model.ProviderRequest request = new com.quickhelp.backend.model.ProviderRequest();
            request.setName(name);
            request.setServiceType(serviceType);
            request.setDescription(description);
            request.setLocation(location);
            if (lat != null) request.setLat(lat);
            if (lng != null) request.setLng(lng);
            request.setPhoneNumber(phoneNumber);
            if (email != null && !email.isEmpty()) request.setEmail(email);
            if (password != null && !password.isEmpty()) {
                request.setPassword(password);
            } else {
                request.setPassword(phoneNumber); // Fallback
            }
            if (gender != null && !gender.isEmpty()) {
                request.setGender(gender);
            }
            if (file != null && !file.isEmpty()) {
                request.setPhotoData(file.getBytes());
            }
            request.setStatus(com.quickhelp.backend.model.ProviderRequest.RequestStatus.PENDING);
            providerRequestRepository.save(request);
            return org.springframework.http.ResponseEntity.ok("Request submitted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.status(500).body("Error submitting request: " + e.getMessage());
        }
    }

    @GetMapping("/provider-requests")
    public List<com.quickhelp.backend.model.ProviderRequest> getProviderRequests() {
        return providerRequestRepository.findByStatus(com.quickhelp.backend.model.ProviderRequest.RequestStatus.PENDING);
    }

    @PostMapping("/provider-requests/{id}/approve")
    public org.springframework.http.ResponseEntity<String> approveProviderRequest(@PathVariable("id") Long id) {
        try {
            return providerRequestRepository.findById(id).map(request -> {
                // Move to Provider
                Provider provider = new Provider();
                provider.setName(request.getName());
                provider.setServiceType(request.getServiceType());
                provider.setPhone(request.getPhoneNumber());
                provider.setEmail(request.getEmail());
                provider.setGender(request.getGender() != null ? request.getGender() : "Any"); // Transfer gender
                provider.setPhotoData(request.getPhotoData());
                provider.setRating(0.0); // New provider default rating
                provider.setPrice("₹200/hr"); // Default price or ask user?
                if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                    provider.setPassword(request.getPassword());
                } else {
                    provider.setPassword(request.getPhoneNumber()); // Fallback
                }
                
                // Transfer location data
                if (request.getLat() != null && request.getLng() != null) {
                    provider.setLat(request.getLat());
                    provider.setLng(request.getLng());
                } else {
                    provider.setLat(12.9716); // Default to Bangalore center
                    provider.setLng(77.5946);
                }
                
                providerRepository.save(provider);
                
                request.setStatus(com.quickhelp.backend.model.ProviderRequest.RequestStatus.APPROVED);
                providerRequestRepository.save(request);
                
                return org.springframework.http.ResponseEntity.ok("Request approved and Provider created");
            }).orElse(org.springframework.http.ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.status(500).body("Error approving request: " + e.getMessage());
        }
    }

    @PostMapping("/provider-requests/{id}/reject")
    public org.springframework.http.ResponseEntity<String> rejectProviderRequest(@PathVariable("id") Long id) {
        return providerRequestRepository.findById(id).map(request -> {
            request.setStatus(com.quickhelp.backend.model.ProviderRequest.RequestStatus.REJECTED);
            providerRequestRepository.save(request);
            return org.springframework.http.ResponseEntity.ok("Request rejected");
        }).orElse(org.springframework.http.ResponseEntity.notFound().build());
    }
}
