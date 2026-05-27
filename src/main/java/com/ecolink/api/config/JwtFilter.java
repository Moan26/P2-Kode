package com.ecolink.api.config;

import com.ecolink.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
//Overordnet håndtere klassen at filteret køre hver gang der er forespørgsel på login og tokens, så siger den bruger logget ind og kan det her...
@Component
public class JwtFilter extends OncePerRequestFilter { //Sikre at den tjekker en enkel gang for hver forespørgsel om gyldig login token.

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization"); //Læser forespørgsel

        if (header != null && header.startsWith("Bearer ")) { //checker for token (ordet er 7 tegn med mellemrum efter ord som den tjekker efter plads 7)
            String token = header.substring(7);

            if (jwtUtil.validateToken(token)) {//validere token og hvis gyldig bruger email til at fidne bryger i databasen.
                String email = jwtUtil.extractEmail(token);

                userRepository.findByEmail(email).ifPresent(user -> { //snakker med security og sikre at man er logget ind og permissions som ADMIN til ændringer osv.
                    var auth = new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" +
                                    user.getRole()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        }

        filterChain.doFilter(request, response); //giver response på forespørgsel om token om den findes eller ej så man kan komme videre i processen.
    }
}
