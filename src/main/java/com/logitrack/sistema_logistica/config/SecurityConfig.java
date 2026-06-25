package com.logitrack.sistema_logistica.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.logitrack.sistema_logistica.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Público existente
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/mock/**").permitAll()
                        .requestMatchers("/api/reportes/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/tracking/consulta").permitAll()

                        // Endpoint para obtener PDF de Carta Porte (puede ser público o restringido
                        .requestMatchers(HttpMethod.GET, "/api/envios/*/pdf-carta-porte").permitAll()

                        // Admin
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")

                        // Chofer
                        .requestMatchers("/api/chofer/**").hasRole("CHOFER")
                        .requestMatchers(HttpMethod.PATCH, "/api/envios/*/estado").hasRole("CHOFER")
                        .requestMatchers(HttpMethod.POST, "/api/envios/*/incidencias").hasRole("CHOFER")

                        // Solo Supervisor
                        .requestMatchers(HttpMethod.GET, "/api/envios/historial-completo").hasRole("SUPERVISOR")

                        // Operador y Supervisor
                        .requestMatchers(HttpMethod.POST, "/api/envios").hasAnyRole("OPERADOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/envios/sin-asignar").hasAnyRole("OPERADOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/envios/search").hasAnyRole("OPERADOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/envios/*/operativo")
                        .hasAnyRole("OPERADOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/envios/*/asignar-transporte")
                        .hasAnyRole("OPERADOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PUT, "/api/envios/*/cancelar").hasAnyRole("SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/**").hasAnyRole("OPERADOR", "SUPERVISOR")

                        // Envío por ID — todos los roles autenticados
                        .requestMatchers(HttpMethod.GET, "/api/envios/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/envios/buscar/*").authenticated()

                        .requestMatchers("/ws-logistica/**").permitAll() // Libera el WebSocket
                        // .requestMatchers("/test-ws.html").permitAll() //para testear los websockets

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean de BCrypt para hashear passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Deshabilitamos credenciales si usamos SOLO JWT
        configuration.setAllowCredentials(true);

        // Al estar en false, podemos usar el comodín global
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000",
                "https://logitrackagro.vercel.app",
                "https://logitrackagro-git-main-logi-track-s-projects.vercel.app"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}