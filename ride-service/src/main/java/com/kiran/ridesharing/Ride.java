package com.kiran.ridesharing;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // Tells Hibernate to create a table named 'rides'
@Table(name = "rides")
public class Ride {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String passengerName;
    private String pickupLocation;
    private String destination;
    private Double fare;

    // Standard empty constructor required by JPA
    public Ride() {}

    // Constructor for easy testing
    public Ride(String passengerName, String pickupLocation, String destination, Double fare) {
        this.passengerName = passengerName;
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.fare = fare;
    }

    // Getters and Setters (use Lombok later to automate this)
    public UUID getId() { return id; }
    public String getPassengerName() { return passengerName; }
}