package com.kiran.driversharing.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import com.kiran.driversharing.service.DriverLocationService;

@Component
@Slf4j
public class TripEventConsumer {

    private final DriverLocationService driverLocationService;

    public TripEventConsumer(DriverLocationService driverLocationService) {
        this.driverLocationService = driverLocationService;
    }

    @KafkaListener(topics = "trip-events", groupId = "driver-availability-group")
    public void handleTripEvent(TripEvent event) {
        log.info("📩 Kafka Event Received: Trip {} for Driver {} is now {}", 
                 event.tripId(), event.driverId(), event.status());
        
        if ("ACCEPTED".equals(event.status())) {
            // Logic: Remove driver from available pool in Redis
            driverLocationService.setDriverBusy(event.driverId());
            log.info("🚫 Driver {} marked as BUSY", event.driverId());
        } else if ("COMPLETED".equals(event.status())) {
           // 2. Clear "Busy" status so the next GPS Ping is accepted
            // This ensures the driver's phone can successfully re-register as 'Available'
            driverLocationService.setDriverAvailable(event.driverId());
            log.info("✅ Driver {} marked as FREE. Ready for next location update.", event.driverId());
        }
    }
}