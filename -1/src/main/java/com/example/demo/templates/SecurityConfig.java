package com.example.demo.templates;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                        "/market.js"
                    ).permitAll()
                .requestMatchers(
                        "/market.css"
                    ).permitAll()
                .requestMatchers(
                        "/market.html"
                    ).permitAll()

                .anyRequest().permitAll()
            );

        return http.build();
    }
}