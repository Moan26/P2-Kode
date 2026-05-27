package com.ecolink.api.controller;

import com.ecolink.api.model.User;
import com.ecolink.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
//Bruges til at man kan se sin egen profil og at man er logget ind som en logget ind bruger.
@RestController
@RequestMapping("/api/users")
public class UserController { //Man skal være logget ind, men håndtere endpoints relateret til den bruger som er logget ind.

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) { //Returnere egen profil for logget ind "Authentication er hvem som er logget ind som rolle osv., egentlig bare det som er sat at JWTFilter.
        User user = userService.getByEmail(authentication.getName()); //Gir username ffra token, men bruges i forhold til email og finde user i databasen.
        return ResponseEntity.ok(Map.of( //Returnere user info. ikke password.
                "id",           user.getId(),
                "username",     user.getUsername(),
                "email",        user.getEmail(),
                "role",         user.getRole(),
                "wasteSaved",   user.getWasteSaved(),
                "carbonCredit", user.getCarbonCredit(),
                "totalPickups", user.getTotalPickups()
        ));
    }
}
