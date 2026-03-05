package com.kiran.driversharing.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;

import com.kiran.driversharing.service.DriverLocationService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverLocationController {

    private final DriverLocationService locationService;

    public DriverLocationController(DriverLocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/{driverId}/location")
    public ResponseEntity<String> updateLocation(
            @PathVariable String driverId,
            @RequestParam double lat,
            @RequestParam double lng) {
        
        locationService.updateDriverLocation(driverId, lat, lng);
        return ResponseEntity.ok("Location updated for driver: " + driverId);
    }

    @GetMapping("/nearby")
    public ResponseEntity<GeoResults<RedisGeoCommands.GeoLocation<Object>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius) {
        
        return ResponseEntity.ok(locationService.getNearbyDrivers(lat, lng, radius));
    }

    @GetMapping("/verify/{driverId}")
        public ResponseEntity<Void> verifyDriver(@PathVariable String driverId) {
            Double score = locationService.checkDriverPresence(driverId);
        
        if (score != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}