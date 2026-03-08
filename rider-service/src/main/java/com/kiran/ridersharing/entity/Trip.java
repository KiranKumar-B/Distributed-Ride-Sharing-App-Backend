package com.kiran.ridersharing.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Version;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "trips")
@Data
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String riderId;
    private String driverId; // Null until accepted

    private Double pickupLat;
    private Double pickupLng;
    private Double destinationLat;
    private Double destinationLng;

    // For future PostGis use, for geo-fencing 
    @JsonIgnore // Prevents the envelope recursion in the API response
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point pickupLocation;

    // Before saving, sync them
    @PreUpdate
    public void handleBeforeUpdate() {
        syncSpatialData();
    }

    private void syncSpatialData() {
        if (this.pickupLat != null && this.pickupLng != null) {
            GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
            // Lon comes before Lat in JTS/PostGIS coordinates!
            this.pickupLocation = factory.createPoint(new Coordinate(this.pickupLng, this.pickupLat));
        }
    }

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = TripStatus.REQUESTED;
    }

    @Version
    private Integer version; // Added for Optimistic Locking
}