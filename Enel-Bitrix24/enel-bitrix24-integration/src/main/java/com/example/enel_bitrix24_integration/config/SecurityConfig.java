package com.example.enel_bitrix24_integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // disabilitiamo CSRF
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // lasciamo passare tutto
                );
        return http.build();
    }
}
