package com.ecolink.api.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;

    public SecurityConfiguration(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(request -> request

                        // Auth endpoints er åbne
                        .requestMatchers("/api/auth/**").permitAll()

                        // Ecopoints er åbne
                        .requestMatchers(HttpMethod.GET,
                                "/api/ecopoints/**").permitAll()
                        // Stream-endpoints er åbne (media serveres direkte af expo-av)
                        .requestMatchers("/api/videos/stream/**").permitAll() //Gør det muligt for alle som er logget ind at kunne stream billeder og video om man er viewer eller admin.
                        .requestMatchers("/api/videos/thumbnail/**").permitAll()//
                        .requestMatchers("/api/images/stream/**").permitAll()

                        // Billeder GET kræver login
                        .requestMatchers(HttpMethod.GET,
                                "/api/images/**").authenticated()

                        // Videoer kræver login
                        .requestMatchers(HttpMethod.GET,
                                "/api/videos/**").authenticated()

                        // Upload/rediger/slet kræver EDITOR eller ADMIN
                        .requestMatchers(HttpMethod.POST,
                                "/api/videos/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/videos/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/videos/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/images/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/images/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/images/**").authenticated()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}