package com.kiran.ridesharing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RideServiceApplication {

    public static void main(String[] args) {
        // This line tells Spring to start the web server and the JPA engine
        SpringApplication.run(RideServiceApplication.class, args);
    }
}