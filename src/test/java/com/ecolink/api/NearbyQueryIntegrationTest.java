package com.ecolink.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Disabled("Testcontainers incompatible with Docker Desktop WSL2 backend on this machine - placeholder test only")
@Testcontainers
@SpringBootTest(
    classes = EcopointsP2Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class NearbyQueryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer =
        new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getConnectionString);
    }

    @Test
    void contextLoads() {
    }
}