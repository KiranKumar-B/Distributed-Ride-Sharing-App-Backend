package com.kiran.ridersharing.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiran.ridersharing.dto.NearbyDriverDTO;
import com.kiran.ridersharing.service.RiderLocationService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/rider")
public class RiderLocationController {

    private final RiderLocationService riderLocationService;

    public RiderLocationController(RiderLocationService riderLocationService) {
        this.riderLocationService = riderLocationService;
    }

    @GetMapping("/nearby-cabs")
    public Flux<NearbyDriverDTO> getNearbyCabs(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius) {
        
        return riderLocationService.getNearbyDrivers(lat, lng, radius);
    }
}