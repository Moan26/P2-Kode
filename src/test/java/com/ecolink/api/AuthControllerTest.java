package com.ecolink.api;

import com.ecolink.api.config.JwtUtil;
import com.ecolink.api.config.SecurityConfiguration;
import com.ecolink.api.controller.AuthController;
import com.ecolink.api.model.User;
import com.ecolink.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfiguration.class)
class AuthControllerTest {

    private static final String HASHED_PASSWORD =
            new BCryptPasswordEncoder().encode("secret");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void register_withNewEmail_returns201() throws Exception {
        when(userRepository.existsByEmail("ny@test.dk")).thenReturn(false);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testuser",
                                  "email": "ny@test.dk",
                                  "password": "hemmeligt123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void register_withExistingEmail_returns400() throws Exception {
        when(userRepository.existsByEmail("findes@test.dk")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testuser",
                                  "email": "findes@test.dk",
                                  "password": "hemmeligt123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_withValidCredentials_returns200WithToken() throws Exception {
        User user = User.builder()
                .id("id1")
                .username("testuser")
                .email("test@test.dk")
                .password(HASHED_PASSWORD)
                .role("VIEWER")
                .build();

        when(userRepository.findByEmail("test@test.dk")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@test.dk",
                                  "password": "secret"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.user.email").value("test@test.dk"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        User user = User.builder()
                .id("id1")
                .username("testuser")
                .email("test@test.dk")
                .password(HASHED_PASSWORD)
                .role("VIEWER")
                .build();

        when(userRepository.findByEmail("test@test.dk")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@test.dk",
                                  "password": "forkert-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        when(userRepository.findByEmail("ukendt@test.dk")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ukendt@test.dk",
                                  "password": "ligemeget"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}