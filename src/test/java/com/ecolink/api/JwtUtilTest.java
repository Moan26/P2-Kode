package com.ecolink.api;

import com.ecolink.api.config.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-hemmelig-noegle-mindst-32-tegn-lang");
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken("id1", "testuser",
                "test@test.dk");
        assertNotNull(token);
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken("id1", "testuser",
                "test@test.dk");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("dette-er-ikke-et-token"));
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("id1", "testuser",
                "test@test.dk");
        assertEquals("test@test.dk", jwtUtil.extractEmail(token));
    }
}
