package com.kiran.driversharing.service;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.Metrics;

@Slf4j
@Service
public class DriverLocationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String DRIVER_GEO_KEY = "DRIVER_LOCATIONS";
    private static final String BUSY_KEY_PREFIX = "driver:busy:";

    public DriverLocationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateDriverLocation(String driverId, double latitude, double longitude) {
        // Lombok will automatically insert a null check here. But we should still manually check for empty strings.
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver ID cannot be null or empty");
        }

        // Check if the driver is currently on a ride
        if (isDriverBusy(driverId)) {
            log.info("Driver {} is busy on a trip. GPS ping ignored for availability.", driverId);
            return; // Exit early! Do not add to the Geo-ZSET.
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

    public void setDriverBusy(String driverId) {
        // We remove the driver from the Geospatial set (ZSET)
        // This makes them disappear from 'Nearby' search results immediately
        redisTemplate.opsForZSet().remove(DRIVER_GEO_KEY, driverId);
        log.info("Driver {} removed from availability pool (Busy)", driverId);

        // 2. Set a "Busy" flag with an expiration (e.g., 2 hours) 
        // The timeout is a safety net in case a "Complete" event is never sent
        redisTemplate.opsForValue().set(BUSY_KEY_PREFIX + driverId, "true", Duration.ofHours(2));
    }

    public void setDriverAvailable(String driverId) {
        // Remove the "Busy" flag
        redisTemplate.delete(BUSY_KEY_PREFIX + driverId);
        log.info("Key deleted: {}{}. Driver is now free for GPS pings.", BUSY_KEY_PREFIX, driverId);
    }

    public boolean isDriverBusy(String driverId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BUSY_KEY_PREFIX + driverId));
    }
}