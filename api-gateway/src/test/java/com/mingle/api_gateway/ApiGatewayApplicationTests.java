package com.mingle.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

	@DynamicPropertySource
	static void redis(DynamicPropertyRegistry registry) {
		RedisTestContainer.register(registry);
	}

	@Test
	void contextLoads() {
	}

}
