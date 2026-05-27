package com.ecolink.api.service;

import com.ecolink.api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import
        org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
//UserDetailService oversætter vores User model til noget Spring security kan aflæse.
@Service
public class UserDetailService implements UserDetailsService { //Spring Security brugger klassen til at finde frem til hvordan man henter en user.

    private final UserRepository userRepository;

    public UserDetailService(UserRepository userRepository) {
        this.userRepository = userRepository;} // Spring Security kalder metoden når en bruger skal verificeres og indlæse user data

    @Override
    public UserDetails loadUserByUsername(String username) throws //Den kaster fejl når en bruger ikke kan findes i databasen ved username.
            UsernameNotFoundException {
        com.ecolink.api.model.User user =
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User was not found: " + username));

        return // Alt hvad den returnere er en pakke af bruger info som username, email crypt password og rollen (admin, viewer...). Bruges til adgangskontrol for login.
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole())
                        .build();
    }
}
