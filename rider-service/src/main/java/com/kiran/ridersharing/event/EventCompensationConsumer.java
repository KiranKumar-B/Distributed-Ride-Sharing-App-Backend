package com.kiran.ridersharing.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.kiran.ridersharing.entity.TripStatus;
import com.kiran.ridersharing.repository.TripRepository;

@Slf4j
@Service
public class EventCompensationConsumer {

    private final TripRepository tripRepository;
    
    public EventCompensationConsumer(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }
    
    @KafkaListener(topics = "trip-event-compensations", groupId = "rider-group")
    public void rollbackTrip(TripEventCompensation compensation) {
        tripRepository.findById(compensation.tripId()).ifPresent(trip -> {
            trip.setStatus(TripStatus.CANCELLED); // Or REJECTED
            tripRepository.save(trip);
            log.warn("SAGA ROLLBACK: Trip {} cancelled due to: {}", compensation.tripId(), compensation.reason());
        });
    }
}
