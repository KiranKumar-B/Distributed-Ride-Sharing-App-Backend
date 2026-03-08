package com.kiran.driversharing.event;

public record TripEventCompensation(
    Long tripId,
    String reason
) {}
