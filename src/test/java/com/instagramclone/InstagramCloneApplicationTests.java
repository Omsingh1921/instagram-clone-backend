package com.instagramclone;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InstagramCloneApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring Boot ApplicationContext loads successfully with test configuration
    }
}
