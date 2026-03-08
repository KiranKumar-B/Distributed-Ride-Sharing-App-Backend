package com.kiran.ridersharing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiran.ridersharing.query.TripAuditProjector;

@RestController
@RequestMapping("/api/v1/analytics")
public class TripQueryController {

    private final TripAuditProjector auditProjector;

    public TripQueryController(TripAuditProjector auditProjector) {
        this.auditProjector = auditProjector;
    }

    @GetMapping("/status/{tripId}")
    public ResponseEntity<String> getQuickStatus(@PathVariable Long tripId) {
        // This is a high-speed read that never hits the main DB
        return ResponseEntity.ok(auditProjector.getLiveAuditStatus(tripId));
    }
}