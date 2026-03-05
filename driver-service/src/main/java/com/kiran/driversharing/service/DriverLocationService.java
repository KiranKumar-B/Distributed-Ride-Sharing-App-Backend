package com.kiran.driversharing.service;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.Metrics;

@Service
public class DriverLocationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String DRIVER_GEO_KEY = "DRIVER_LOCATIONS";

    public DriverLocationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateDriverLocation(String driverId, double latitude, double longitude) {
        // Lombok will automatically insert a null check here. But we should still manually check for empty strings.
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver ID cannot be null or empty");
        }

        // Redis Geo expects (longitude, latitude) - order matters!
        Point point = new Point(longitude, latitude);
        redisTemplate.opsForGeo().add(DRIVER_GEO_KEY, point, driverId);
    }

    public GeoResults<RedisGeoCommands.GeoLocation<Object>> getNearbyDrivers(double lat, double lng, double radiusInKm) {
        // 1. Define the search area (Point + Radius)
        Point center = new Point(lng, lat);
        Distance radius = new Distance(radiusInKm, Metrics.KILOMETERS);
        Circle area = new Circle(center, radius);

        // 2. Set search arguments (e.g., sort by distance, include coordinates)
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance() // Tells us how far they are
                .includeCoordinates() // Gives us their exact lat/lng
                .sortAscending(); // Nearest drivers first

        // 3. Execute GEORADIUS/GEOSEARCH
        return redisTemplate.opsForGeo().radius(DRIVER_GEO_KEY, area, args);
    }

    public Double checkDriverPresence(String driverId) {
        // We check if the driver has a score in the Geospatial Sorted Set
        // If they exist, score is returned; if not, it's empty/null
        return redisTemplate.opsForZSet().score(DRIVER_GEO_KEY, driverId);
    }
}