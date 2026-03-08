package com.kiran.ridersharing.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.ParameterizedTypeReference;

import com.kiran.ridersharing.dto.NearbyDriverDTO;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@EnableCaching
@Service
@Slf4j
public class RiderLocationService {

    private final WebClient webClient;

    public RiderLocationService(WebClient.Builder webClientBuilder) {
        // 'driver-service' is the name of the container in docker-compose
        this.webClient = webClientBuilder.baseUrl("http://driver-service:8081").build();
    }

    @CircuitBreaker(name = "driverService", fallbackMethod = "verifyDriverFallback")
    public Flux<NearbyDriverDTO> getNearbyDrivers(double lat, double lng, double radius) {
    return this.webClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/driver/nearby")
                    .queryParam("lat", lat)
                    .queryParam("lng", lng)
                    .queryParam("radius", radius)
                    .build())
            .retrieve()
            // 1. Get the whole body as a Map first
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}) 
            // 2. Turn the "content" list into a Flux (stream of items)
            .flatMapMany(response -> {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                return Flux.fromIterable(content);
            })
            // 3. Map each item to our DTO
            .map(item -> {
                NearbyDriverDTO dto = new NearbyDriverDTO();
                Map<String, Object> innerContent = (Map<String, Object>) item.get("content");
                Map<String, Object> point = (Map<String, Object>) innerContent.get("point");
                Map<String, Object> distance = (Map<String, Object>) item.get("distance");

                dto.setDriverId((String) innerContent.get("name"));
                dto.setLatitude((Double) point.get("y"));
                dto.setLongitude((Double) point.get("x"));
                dto.setDistance((Double) distance.get("value"));
                return dto;
            });
    }

    public Flux<NearbyDriverDTO> verifyDriverFallback(Double lat, Double lng, Exception e, Throwable t) {
        log.error("Resilience4j Fallback Triggered. Reason: {}", t.getMessage());
        // Return an empty list or cached results so the UI doesn't crash
        return Flux.empty();
    }
}