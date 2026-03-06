package com.kiran.ridersharing.event;

public record TripEvent(
    Long tripId,
    String driverId,
    String status // e.g., "ACCEPTED", "COMPLETED"
) {}
