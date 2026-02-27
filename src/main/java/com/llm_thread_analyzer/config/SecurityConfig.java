package com.llm_thread_analyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Necessário desativar o CSRF para permitir pedidos POST do Postman/PowerShell
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/files/**").permitAll() // Liberta a tua rota do analisador
                .anyRequest().authenticated() // Mantém as outras bloqueadas
            );
            
        return http.build();
    }
}