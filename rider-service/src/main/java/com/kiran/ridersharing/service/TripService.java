package com.kiran.ridersharing.service;

import com.kiran.ridersharing.entity.Trip;
import com.kiran.ridersharing.repository.TripRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final RiderLocationService riderLocationService;

    public TripService(TripRepository tripRepository, RiderLocationService riderLocationService) {
        this.tripRepository = tripRepository;
        this.riderLocationService = riderLocationService;
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
}