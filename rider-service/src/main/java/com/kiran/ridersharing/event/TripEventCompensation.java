package com.kiran.ridersharing.event;

public record TripEventCompensation(
    Long tripId,
    String reason
) {}
