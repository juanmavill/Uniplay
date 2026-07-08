package edu.eci.uniplay.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "uniplay.gateway.rate-limit.enabled=false")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
