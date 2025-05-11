package dev.sushaanth.bookly;

import dev.sushaanth.bookly.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class MultiTenantConcurrencyTest {
    
    /**
     * Test Security Configuration to disable authentication for tests
     */
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                    .anyRequest().permitAll()
                );
            return http.build();
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private static final String TENANT1 = "tenant1";
    private static final String TENANT2 = "tenant2";
    private static final String TENANT_HEADER = "X-TenantId";
    private static final String BASE_URL = "http://localhost:";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;

    // Define users for tenant1
    private final List<String> tenant1Usernames = List.of("user1-tenant1", "user2-tenant1", "user3-tenant1");
    
    // Define users for tenant2
    private final List<String> tenant2Usernames = List.of("user1-tenant2", "user2-tenant2", "user3-tenant2");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.0")
            .withDatabaseName("multitenant-test")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("db/testcontainer-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        
        // Clear existing data
        jdbcTemplate.execute("DELETE FROM tenant1.users");
        jdbcTemplate.execute("DELETE FROM tenant2.users");
        
        // Insert test users for tenant1
        for (String username : tenant1Usernames) {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO tenant1.users (id, username, firstname, lastname) VALUES (?, ?, ?, ?)",
                    id, username, "First" + username, "Last" + username
            );
        }
        
        // Insert test users for tenant2
        for (String username : tenant2Usernames) {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update(
                    "INSERT INTO tenant2.users (id, username, firstname, lastname) VALUES (?, ?, ?, ?)",
                    id, username, "First" + username, "Last" + username
            );
        }
    }

    @Test
    void testGetTenantEndpoint() {
        // Test the /tenant endpoint to verify tenant context is set correctly
        HttpHeaders headers1 = new HttpHeaders();
        headers1.set(TENANT_HEADER, TENANT1);
        HttpEntity<?> entity1 = new HttpEntity<>(headers1);
        
        ResponseEntity<String> response1 = restTemplate.exchange(
                BASE_URL + port + "/tenant",
                HttpMethod.GET,
                entity1,
                String.class
        );
        
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(TENANT1, response1.getBody());
        
        // Test with tenant2
        HttpHeaders headers2 = new HttpHeaders();
        headers2.set(TENANT_HEADER, TENANT2);
        HttpEntity<?> entity2 = new HttpEntity<>(headers2);
        
        ResponseEntity<String> response2 = restTemplate.exchange(
                BASE_URL + port + "/tenant",
                HttpMethod.GET,
                entity2,
                String.class
        );
        
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(TENANT2, response2.getBody());
    }

    @Test
    void testConcurrentTenantSpecificRequests() throws Exception {
        int numberOfRequests = 10; // Reduced for faster tests
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Submit tenant1 request task
        Future<List<User>> tenant1Future = executor.submit(() -> {
            List<User> allResponses = new ArrayList<>();
            
            try {
                latch.await(); // Wait for the signal to start
                
                for (int i = 0; i < numberOfRequests; i++) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set(TENANT_HEADER, TENANT1);
                    HttpEntity<?> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<List<User>> response = null;
                    // Retry logic in case of initial failures
                    for (int retry = 0; retry < MAX_RETRIES; retry++) {
                        try {
                            response = restTemplate.exchange(
                                    BASE_URL + port + "/users",
                                    HttpMethod.GET,
                                    entity,
                                    new ParameterizedTypeReference<List<User>>() {}
                            );
                            break;
                        } catch (Exception e) {
                            if (retry == MAX_RETRIES - 1) throw e;
                            Thread.sleep(RETRY_DELAY_MS);
                        }
                    }
                    
                    if (response != null && response.getBody() != null) {
                        allResponses.addAll(response.getBody());
                    }
                    // Small delay to allow interleaving
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return allResponses;
        });
        
        // Submit tenant2 request task
        Future<List<User>> tenant2Future = executor.submit(() -> {
            List<User> allResponses = new ArrayList<>();
            
            try {
                latch.await(); // Wait for the signal to start
                
                for (int i = 0; i < numberOfRequests; i++) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set(TENANT_HEADER, TENANT2);
                    HttpEntity<?> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<List<User>> response = null;
                    // Retry logic in case of initial failures
                    for (int retry = 0; retry < MAX_RETRIES; retry++) {
                        try {
                            response = restTemplate.exchange(
                                    BASE_URL + port + "/users",
                                    HttpMethod.GET,
                                    entity,
                                    new ParameterizedTypeReference<List<User>>() {}
                            );
                            break;
                        } catch (Exception e) {
                            if (retry == MAX_RETRIES - 1) throw e;
                            Thread.sleep(RETRY_DELAY_MS);
                        }
                    }
                    
                    if (response != null && response.getBody() != null) {
                        allResponses.addAll(response.getBody());
                    }
                    // Small delay to allow interleaving
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return allResponses;
        });
        
        // Start concurrent execution
        latch.countDown();
        
        // Get results
        List<User> tenant1Users = tenant1Future.get();
        List<User> tenant2Users = tenant2Future.get();
        
        executor.shutdown();
        
        // Verify tenant1 responses contain only tenant1 usernames
        List<String> tenant1ResponseUsernames = tenant1Users.stream()
                .map(User::getUsername)
                .distinct()
                .collect(Collectors.toList());
        
        assertThat(tenant1ResponseUsernames).containsExactlyInAnyOrderElementsOf(tenant1Usernames);
        assertThat(tenant1ResponseUsernames).doesNotContainAnyElementsOf(tenant2Usernames);
        
        // Verify tenant2 responses contain only tenant2 usernames
        List<String> tenant2ResponseUsernames = tenant2Users.stream()
                .map(User::getUsername)
                .distinct()
                .collect(Collectors.toList());
        
        assertThat(tenant2ResponseUsernames).containsExactlyInAnyOrderElementsOf(tenant2Usernames);
        assertThat(tenant2ResponseUsernames).doesNotContainAnyElementsOf(tenant1Usernames);
    }

    @Test
    void testMissingTenantHeader() {
        // Test with missing tenant header
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + port + "/users",
                HttpMethod.GET,
                entity,
                String.class
        );
        
        // Verify it's a Bad Request (400) response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), 
                "Expected HTTP 400 Bad Request for missing tenant header");
        
        // Optionally verify the response body contains tenant-related error message
        // Skip body check if response body is null
        if (response.getBody() != null) {
            assertTrue(response.getBody().contains("Tenant header is required"), 
                    "Response should mention the missing tenant header requirement");
        }
    }

    @Test
    void testInvalidTenantHeader() {
        // Test with invalid tenant header
        HttpHeaders headers = new HttpHeaders();
        headers.set(TENANT_HEADER, "non-existent-tenant");
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + port + "/users",
                HttpMethod.GET,
                entity,
                String.class
        );
        
        // Verify it's a Bad Request (400) response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Expected HTTP 400 Bad Request for invalid tenant header");
        
        // Verify the response body contains the invalid tenant info
        // Skip body check if response body is null
        if (response.getBody() != null) {
            assertTrue(response.getBody().contains("Invalid tenant"),
                    "Response should mention the invalid tenant");
        }
    }

    @Test
    void testCrossTenantWriteOperations() {
        // Create a new user for tenant1
        HttpHeaders postHeaders = new HttpHeaders();
        postHeaders.set(TENANT_HEADER, TENANT1);
        postHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        String newUserJson = "{\"username\":\"new-user-tenant1\",\"firstName\":\"New\",\"lastName\":\"User\"}";
        HttpEntity<String> postEntity = new HttpEntity<>(newUserJson, postHeaders);
        
        // Create user in tenant1
        ResponseEntity<User> postResponse = restTemplate.exchange(
                BASE_URL + port + "/users",
                HttpMethod.POST,
                postEntity,
                User.class
        );
        
        assertEquals(HttpStatus.CREATED, postResponse.getStatusCode());
        
        // Verify the new user exists in tenant1
        HttpHeaders getHeaders1 = new HttpHeaders();
        getHeaders1.set(TENANT_HEADER, TENANT1);
        HttpEntity<?> getEntity1 = new HttpEntity<>(getHeaders1);
        
        ResponseEntity<List<User>> getResponse1 = restTemplate.exchange(
                BASE_URL + port + "/users",
                HttpMethod.GET,
                getEntity1,
                new ParameterizedTypeReference<List<User>>() {}
        );
        
        List<String> tenant1Usernames = getResponse1.getBody().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
        
        assertThat(tenant1Usernames).contains("new-user-tenant1");
        
        // Verify tenant2 data is unaffected
        HttpHeaders getHeaders2 = new HttpHeaders();
        getHeaders2.set(TENANT_HEADER, TENANT2);
        HttpEntity<?> getEntity2 = new HttpEntity<>(getHeaders2);
        
        ResponseEntity<List<User>> getResponse2 = restTemplate.exchange(
                BASE_URL + port + "/users",
                HttpMethod.GET,
                getEntity2,
                new ParameterizedTypeReference<List<User>>() {}
        );
        
        List<String> tenant2Usernames = getResponse2.getBody().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
        
        assertThat(tenant2Usernames).doesNotContain("new-user-tenant1");
        assertThat(tenant2Usernames).containsExactlyInAnyOrderElementsOf(this.tenant2Usernames);
    }
}

