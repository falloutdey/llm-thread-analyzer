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
            // CORREÇÃO: CORS ativado ao nível do Spring Security.
            // Sem esta linha, o filtro de segurança interceta e bloqueia os pedidos OPTIONS
            // (Preflight) que o browser envia antes de qualquer POST cross-origin, devolvendo
            // HTTP 401/403 antes de o pedido chegar ao Controller — o @CrossOrigin do Controller
            // nunca chega a ser aplicado. withDefaults() delega para a configuração de CORS
            // da camada Web (definida pelo @CrossOrigin no Controller ou por um CorsConfigurationSource).
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/files/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}