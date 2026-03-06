package com.kiran.driversharing.event;

public record TripEvent(
    Long tripId,
    String driverId,
    String status
) {}