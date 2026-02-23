package com.kiran.ridesharing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // Tells Spring this class handles HTTP requests
public class RideController {

    @GetMapping("/hello") // Maps GET requests for /hello to this method
    public String sayHello() {
        return "Ride Service is Live and Connected to Postgres!";
    }
}