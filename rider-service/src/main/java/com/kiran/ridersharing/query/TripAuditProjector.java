package com.kiran.ridersharing.query;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.kiran.ridersharing.event.TripEvent;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TripAuditProjector {

    private final Map<Long, String> tripStatusCache = new ConcurrentHashMap<>();

    // This is a pure "Query Side" listener. 
    // It doesn't care about business rules; it just projects state.
    @KafkaListener(topics = "trip-events", groupId = "audit-query-group")
    public void projectAudit(TripEvent event) {
        log.info("📈 AUDIT LOG: Trip {} transitioned to {}", event.tripId(), event.status());
        
        // We are building a "Materialized View" of the trip status
        tripStatusCache.put(event.tripId(), event.status());
    }

    // New Query Endpoint that doesn't touch the main Trip Table
    public String getLiveAuditStatus(Long tripId) {
        int retries = 5;
        while (retries > 0) {
            String status = tripStatusCache.get(tripId);
            if (status != null) {
                return status;
            }
            try {
                log.info("⏳ Status not found yet for Trip {}, retrying...", tripId);
                Thread.sleep(200); // Wait 200ms for Kafka to catch up
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            retries--;
        }
        return "UNKNOWN"; // Only return UNKNOWN after 1 second of trying
    }
}
