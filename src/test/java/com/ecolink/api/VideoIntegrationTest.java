package com.ecolink.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Disabled("Won't work with Docker Desktop WSL2 backend")
@SpringBootTest(
    classes = EcopointsP2Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class VideoIntegrationTest {
	 @Autowired
	 private TestRestTemplate restTemplate;
	
    @Container
    static MongoDBContainer mongoDBContainer =
        new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
    }

    @Test
    void getAllVideos_returnerOk() throws Exception {
    	var response = restTemplate.getForEntity("/api/videos", String.class);
        assertEquals(200, response.getStatusCode().value());
    }
}