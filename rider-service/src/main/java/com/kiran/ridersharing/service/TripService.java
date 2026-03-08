package com.kiran.ridersharing.service;

import com.kiran.ridersharing.entity.Trip;
import com.kiran.ridersharing.entity.TripStatus;
import com.kiran.ridersharing.event.TripEvent;
import com.kiran.ridersharing.repository.TripRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Slf4j
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final RiderLocationService riderLocationService;
    private final WebClient webClient;
    private final KafkaTemplate<String, TripEvent> kafkaTemplate;

    public TripService(TripRepository tripRepository,
                       RiderLocationService riderLocationService,
                       WebClient.Builder webClientBuilder,
                       KafkaTemplate<String, TripEvent> kafkaTemplate) {
        this.tripRepository = tripRepository;
        this.riderLocationService = riderLocationService;
        this.webClient = webClientBuilder.baseUrl("http://driver-service:8081").build();
        this.kafkaTemplate = kafkaTemplate;
    }

    public Trip requestRide(Trip tripRequest) {
        // 1. Save the trip to Postgres
        Trip savedTrip = tripRepository.save(tripRequest);
        log.info("Trip {} created for Rider {}", savedTrip.getId(), savedTrip.getRiderId());

        // 2. DISPATCH EVENT (This fills the 'UNKNOWN' gap)
    kafkaTemplate.send("trip-events", new TripEvent(savedTrip.getId(), null, "REQUESTED"));

        // 3. Find nearby drivers asynchronously using our existing logic
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
        Trip savedTrip = tripRepository.save(trip);

        // 6. ASYNC EVENT: Notify Driver-Service to make driver "BUSY"
        try {
            TripEvent event = new TripEvent(tripId, driverId, "ACCEPTED");
            // We use driverId as the Kafka partition key to ensure order for that driver
            kafkaTemplate.send("trip-events", driverId, event);
            log.info("Sent ACCEPTED event to Kafka for Driver: {}", driverId);
        } catch (Exception e) {
            // As an SDE-2, we log this but don't necessarily fail the transaction 
            // unless we want strict consistency (part of the Saga discussion later).
            log.error("Failed to send Kafka event for trip {}: {}", tripId, e.getMessage());
        }

    return savedTrip;
    }

    @Transactional
    public Trip completeTrip(Long tripId) {
        // 1. Fetch & Update DB
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        
        if (trip.getStatus() != TripStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot complete a trip that isn't in ACCEPTED state.");
        }

        trip.setStatus(TripStatus.COMPLETED);
        Trip savedTrip = tripRepository.save(trip);

        // 2. Emit Kafka Event
        try {
            TripEvent event = new TripEvent(tripId, trip.getDriverId(), "COMPLETED");
            kafkaTemplate.send("trip-events", trip.getDriverId(), event);
            log.info("Trip {} completed. Kafka event sent for Driver {}", tripId, trip.getDriverId());
        } catch (Exception e) {
            log.error("Failed to notify Kafka about trip completion: {}", e.getMessage());
        }

        return savedTrip;
    }

    @CircuitBreaker(name = "driverService", fallbackMethod = "verifyDriverFallback")
    public void verifyDriverPresence(String driverId) {
        log.info("Verifying presence for Driver: {}", driverId);
        
        // If driver-service is down or slow, this will trigger the fallback
        webClient.get()
                .uri("/api/v1/driver/verify/" + driverId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block(); 
    }

    // The Fallback method to prevent the 60-second hang
    public void verifyDriverFallback(String driverId, Throwable t) {
        log.error("CIRCUIT BREAKER OPEN: Driver service is failing. Providing graceful failure.");
        throw new RuntimeException("Driver verification service is currently unavailable. Please try again in 10 seconds.");
    }
}