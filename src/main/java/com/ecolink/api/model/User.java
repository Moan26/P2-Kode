package com.ecolink.api.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
//Generere gettere og settere og constructers. @Document gør at det gemmes i dtabasen.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Document(collection = "users")
public class User {

    @Id
    private String id;
    private String username;
    private String email;
    private String password;
    private String address;
    private String phone;
    private String role; //role er om man er viewer, admin eller editor.

    //Miljø-tracking (Ecolink bruger det)
    @Builder.Default
    private double wasteSaved = 0;
    @Builder.Default
    private double carbonCredit = 0;
    @Builder.Default
    private int totalPickups = 0;

    private Instant createdAt;//når man registrere sig
    private Instant lastLogin;//opdatere hver gang man logger in
}

