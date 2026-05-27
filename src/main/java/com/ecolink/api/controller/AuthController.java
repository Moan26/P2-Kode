package com.ecolink.api.controller;

import com.ecolink.api.config.JwtUtil;
import com.ecolink.api.model.User;
import com.ecolink.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController { //Håndtere alt login og regitrerings relateret og er åbne enbdpoints da det er på startsiden og alle har adgang.

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, JwtUtil jwtUtil)
    {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder(); //BCrypt er en algoritme til at kryptere adgangskoder som er standard og sikker måde at håndtere password.
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> //Tjekker for eksiterende email og giver fejl hvis den findes så der ikke oprettes med samme mail.
                                              body) {
        String email = body.get("email");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "User already exists"));
        }

        User user = User.builder()
                .username(body.get("username"))
                .email(email)
                .password(passwordEncoder.encode(body.get("password"))) //Når registreret krypteres password og gemmes i databasen.
                .address(body.get("address"))
                .phone(body.get("phone"))
                .role(body.getOrDefault("role", "VIEWER")) //som standard får alle rollen som viewer (mest sikkert) man kan ændre i gemte konti i databasen dem til admin senere.
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body)
    {
        String email = body.get("email");
        String password = body.get("password");

        return userRepository.findByEmail(email)//tjekker om passwords for login matcher det krypterede som er gemt i databasen for samme email.
                .filter(user -> passwordEncoder.matches(password,
                        user.getPassword()))
                .map(user -> {

                    userRepository.save(user.toBuilder().lastLogin(Instant.now()).build()); //Opdatere tidspunkt for login i databasen
                    String token = jwtUtil.generateToken(user.getId(), //opretter ny token som bruges ved næste login.
                            user.getUsername(), user.getEmail());
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "token", token,
                            "user", Map.of(
                                    "id", user.getId(),
                                    "username", user.getUsername(),
                                    "email", user.getEmail()
                            )
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED) //Returnere kode 401 hvis forkert email eller password.
                        .body(Map.of("success", false, "message", "Invalid credentials")));
    }
}
