package com.handgelus.infrastructure.config.seguridad;

import com.handgelus.application.port.out.UsuarioRepositoryPort;
import com.handgelus.infrastructure.adapter.out.seguridad.JwtTokenAdapter;
import com.handgelus.infrastructure.config.HandgelusProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguridad del API REST: autenticación JWT sin estado y control de acceso por rol (SWR-02).
 * Las reglas por URL son una primera barrera; cada caso de uso vuelve a validar el rol y la
 * pertenencia del recurso (defensa en profundidad).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PERSONAL = {"ASISTENTE", "TATUADOR", "ADMINISTRADOR"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, JwtTokenAdapter jwt, UsuarioRepositoryPort usuarios,
                                               HandgelusProperties properties) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(c -> c.configurationSource(configuracionCors(properties)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Público
                        .requestMatchers("/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/catalogos", "/api/tarifas").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/webhook").permitAll()
                        .requestMatchers("/h2-console/**", "/error").permitAll()
                        // F-01: solo administrador
                        .requestMatchers("/api/usuarios/tatuadores").hasAnyRole(PERSONAL)
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        // F-02: propietario (tatuador) y administrador
                        .requestMatchers(HttpMethod.PUT, "/api/tarifas/**").hasAnyRole("TATUADOR", "ADMINISTRADOR")
                        // Panel interno
                        .requestMatchers("/api/tablero/**", "/api/notificaciones/**", "/api/agenda/**")
                        .hasAnyRole(PERSONAL)
                        .requestMatchers(HttpMethod.GET, "/api/pagos").hasAnyRole(PERSONAL)
                        .requestMatchers(HttpMethod.GET, "/api/clientes").hasAnyRole(PERSONAL)
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escribirError(res,
                                HttpServletResponse.SC_UNAUTHORIZED, "Su sesión no es válida o expiró. Inicie sesión nuevamente."))
                        .accessDeniedHandler((req, res, ex) -> escribirError(res,
                                HttpServletResponse.SC_FORBIDDEN, "Su rol no tiene permiso para acceder a este recurso.")))
                .addFilterBefore(new JwtFiltroAutenticacion(jwt, usuarios), UsernamePasswordAuthenticationFilter.class);

        if (properties.seguridad().requerirHttps()) {   // SWR-03: rechaza tráfico sin cifrar
            http.requiresChannel(c -> c.anyRequest().requiresSecure());
        }
        return http.build();
    }

    private CorsConfigurationSource configuracionCors(HandgelusProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().origenesPermitidos());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Firma-Pasarela"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void escribirError(HttpServletResponse res, int estado, String mensaje) throws IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"estado\":" + estado + ",\"mensaje\":\"" + mensaje + "\"}");
    }
}
