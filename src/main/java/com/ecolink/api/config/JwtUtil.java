package com.ecolink.api.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
//klassen overordnet håndtere at producere tokens for login når authController har godkendt brugeren.
@Component //@Component bruges så spring ved at klassen bruges i hele applikationen.
public class JwtUtil {

    @Value("${jwt.secret}") //Henter en kode og gør den unik så den ikke kan forfalskes eller kopires
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    } //Gør string, altså koden til et format så så den kan aflæses af token-library og bruges internt i klassen.

    public String generateToken(String id, String username, String email)  //generere login token med user info, og expires efter 1 dag.
    {
        return Jwts.builder()
                .claim("id", id)
                .claim("username", username)
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() +
                        86400000)) // 1 dag
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) { //Hiver email info frem så den ved hvem som er logget ind.
        return Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload().get("email",
                        String.class);
    }

    public boolean validateToken(String token) { //Validere om det er gyldig token og returnere false hvis det er ugyldigt eller udløbet.
        try {

            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
