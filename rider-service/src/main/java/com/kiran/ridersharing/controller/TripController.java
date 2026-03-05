package com.kiran.ridersharing.controller;

import com.kiran.ridersharing.entity.Trip;
import com.kiran.ridersharing.service.TripService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trip")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/request")
    public Trip requestRide(@RequestBody Trip tripRequest) {
        // This will save the trip to PostgreSQL and return the saved entity with its ID
        return tripService.requestRide(tripRequest);
    }

    @PatchMapping("/{tripId}/accept")
    public Trip acceptTrip(
            @PathVariable Long tripId,
            @RequestParam String driverId) {
        return tripService.acceptTrip(tripId, driverId);
    }
}