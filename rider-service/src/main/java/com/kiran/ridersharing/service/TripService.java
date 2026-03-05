package com.kiran.ridersharing.service;

import com.kiran.ridersharing.entity.Trip;
import com.kiran.ridersharing.entity.TripStatus;
import com.kiran.ridersharing.repository.TripRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final RiderLocationService riderLocationService;
    private final WebClient webClient;

    public TripService(TripRepository tripRepository, RiderLocationService riderLocationService, WebClient.Builder webClientBuilder) {
        this.tripRepository = tripRepository;
        this.riderLocationService = riderLocationService;
        this.webClient = webClientBuilder.baseUrl("http://driver-service:8081").build();
    }

    public Trip requestRide(Trip tripRequest) {
        // 1. Save the trip to Postgres
        Trip savedTrip = tripRepository.save(tripRequest);
        log.info("Trip {} created for Rider {}", savedTrip.getId(), savedTrip.getRiderId());

        // 2. Find nearby drivers asynchronously using our existing logic
        riderLocationService.getNearbyDrivers(
            savedTrip.getPickupLat(), 
            savedTrip.getPickupLng(), 
            5.0 // 5km radius
        ).subscribe(driver -> {
            // 3. Simulate notification
            log.info("NOTIFYING: Driver {} about Trip {} at distance {}km", 
                driver.getDriverId(), savedTrip.getId(), driver.getDistance());
        });
        return savedTrip;
    }

    @Transactional
    public Trip acceptTrip(Long tripId, String driverId) {

        // 1. ASYNC VERIFICATION (The "Gatekeeper")
        // We call the Driver-Service to ensure the driver exists and is active
        try {
            log.info("Verifying presence for Driver: {}", driverId);
            
            webClient.get()
                .uri("/api/v1/driver/verify/" + driverId)
                .retrieve()
                // We only care if it's a 2xx status. If it's 404, WebClient throws an exception.
                .toBodilessEntity() 
                .block(); // Blocking because we are inside a @Transactional DB block
                
        } catch (WebClientResponseException.NotFound e) {
            log.error("Verification failed: Driver {} is offline or does not exist.", driverId);
            throw new SecurityException("Unauthorized: Driver is not active in the system.");
        } catch (Exception e) {
            log.error("Network error while verifying driver: {}", e.getMessage());
            throw new RuntimeException("Driver verification service is currently unavailable.");
        }

        // 2. Fetch the trip or throw 404
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip ID " + tripId + " not found"));

        // 3. Critical Check: Is the trip still available?
        // This prevents a driver from accepting a ride that's already ACCEPTED
        if (trip.getStatus() != TripStatus.REQUESTED) {
            throw new IllegalStateException("Trip is no longer in REQUESTED state. Current status: " + trip.getStatus());
        }

        // 4. Update the State
        trip.setDriverId(driverId);
        trip.setStatus(TripStatus.ACCEPTED);

        log.info("SUCCESS: Trip {} has been accepted by Driver {}", tripId, driverId);
        
        // 5. Persist the change to PostgreSQL
        return tripRepository.save(trip);
    }
}