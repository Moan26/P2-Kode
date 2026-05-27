package com.ecolink.api;

import com.ecolink.api.model.User;
import com.ecolink.api.repository.UserRepository;
import com.ecolink.api.service.UserDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDetailServiceTest {

    private UserRepository userRepository;
    private UserDetailService userDetailService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailService = new UserDetailService(userRepository);
    }

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        User user = User.builder()
                .id("id1")
                .username("testuser")
                .email("test@test.dk")
                .password("hashed-password")
                .role("VIEWER")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = userDetailService.loadUserByUsername("testuser");

        assertEquals("testuser", result.getUsername());
        assertEquals("hashed-password", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER")));
    }

    @Test
    void loadUserByUsername_unknownUser_throwsException() {
        when(userRepository.findByUsername("ukendt")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailService.loadUserByUsername("ukendt"));
    }
}