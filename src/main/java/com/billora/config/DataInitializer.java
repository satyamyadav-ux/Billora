package com.billora.config;

import com.billora.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AuthService authService;

    @Override
    public void run(String... args) {
        authService.seedDefaultUsers();
    }
}
