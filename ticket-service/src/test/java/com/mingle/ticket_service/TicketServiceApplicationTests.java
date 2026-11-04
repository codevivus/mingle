package com.mingle.ticket_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class TicketServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
