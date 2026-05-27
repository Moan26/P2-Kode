package com.ecolink.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
 
@Configuration  

public class CorsConfig implements WebMvcConfigurer {
	 @Override  
	    public void addCorsMappings(CorsRegistry registry) {  
	        registry.addMapping("/api/**") // Apply CORS to all /api/** endpoints  
	                .allowedOrigins("http://localhost:3000", "http://localhost:8081", "exp://localhost:8081") // Tillade Request fra react web og React Expo og Native.
					.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Allow specific methods
	                .allowedHeaders("*") // Allow all headers  
	                .allowCredentials(false) // Enable credentials (e.g., cookies, JWT)  
	                .maxAge(3600); // Cache preflight for 1 hour  
	    }  
	}  
