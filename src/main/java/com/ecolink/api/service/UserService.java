package com.ecolink.api.service;

import com.ecolink.api.model.User;
import com.ecolink.api.repository.UserRepository;
import org.springframework.stereotype.Service;
//Overordnet så sker der det at det er muligt at hente user by email.
@Service
public class UserService { //Klassen bruges af UserController til at håndtere bruger-logikken.

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }//Bruges af eendpoint i controller til at hente user profile login fra databasen, men kaster fejl hvis den ikke kan findes med email.

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: "
                        + email));
    }
}
