package com.billora.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping({"/health", "/actuator/health", "/api/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "Billora",
                "version", "1.0.0",
                "timestamp", Instant.now().toString()
        ));
    }
}
