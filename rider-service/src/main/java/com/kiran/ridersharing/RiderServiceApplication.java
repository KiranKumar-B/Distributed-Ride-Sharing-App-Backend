package com.kiran.ridersharing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RiderServiceApplication {

    public static void main(String[] args) {
        // This line tells Spring to start the web server and the JPA engine
        SpringApplication.run(RiderServiceApplication.class, args);
    }
}