package com.kiran.ridersharing.dto;

import lombok.Data;

@Data
public class NearbyDriverDTO {
    private String driverId;
    private double latitude;
    private double longitude;
    private double distance;
}