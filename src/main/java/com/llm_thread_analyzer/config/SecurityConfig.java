package com.llm_thread_analyzer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.auth.FiltroLoginSumawma;
import com.llm_thread_analyzer.auth.VerificadorTokenSumawma;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Origens do frontend autorizadas a chamar a API (separadas por vírgula).
    // Padrão: frontend do Sumawma rodando localmente (3000 ou 3001).
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, VerificadorTokenSumawma verificador,
                                                   ObjectMapper json) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            // Sem sessão: cada requisição do módulo Java traz o token de login do Sumawma.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new FiltroLoginSumawma(verificador, "/api/java/"), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint(FiltroLoginSumawma.respostaSemLogin(verificador, json)))
            .authorizeHttpRequests(auth -> auth
                // Projetos e arquivos Java: só com login do Sumawma (token do backend Node).
                .requestMatchers("/api/java/**").authenticated()
                .requestMatchers("/api/files/**").permitAll()
                .anyRequest().permitAll() 
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split("\\s*,\\s*")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    
        source.registerCorsConfiguration("/**", configuration); 
        return source;
    }
}