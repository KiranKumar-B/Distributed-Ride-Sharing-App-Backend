package com.kiran.driversharing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DriverController {
    
    @GetMapping("/hi")
    public String sayHi() {
        return "Driver Service is Live and Connected to Redis...!";
    }
}
