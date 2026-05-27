package com.ecolink.api.repository;

import com.ecolink.api.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
//Giver os mulighed for at bruge operations som er standard for database
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);//Bruges i JWTFilter og og login for at finde user by email. returnere optional hvis email ikke findes.

    Optional<User> findByUsername(String username);//præcis det samme som forrige, bruges bare i USerDetailService til at finde user by username.

    boolean existsByEmail(String email);//boolean til at tjekke om email findes ved registrering.
}
