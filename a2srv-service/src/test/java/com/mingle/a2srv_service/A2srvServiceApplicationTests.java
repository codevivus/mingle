package com.mingle.a2srv_service;

import com.mingle.a2srv_service.entity.Role;
import com.mingle.a2srv_service.entity.User;
import com.mingle.a2srv_service.repository.UserRepository;
import com.mingle.a2srv_service.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class A2srvServiceApplicationTests {

	@Autowired MockMvc mvc;
	@Autowired UserRepository users;
	@Autowired JwtTokenProvider tokens;

	@Test
	void apiDocsArePublic() throws Exception {
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths").exists());
	}

	@Test
	void contextLoads() {
	}

	private User save(String name, Role role) {
		User user = User.builder().username(name).email(name + "@example.com").passwordHash("x").build();
		if (role != null) {
			user.setRole(role);
		}
		return users.save(user);
	}

	private void assertValidateRole(User user, String expectedRole) throws Exception {
		String token = tokens.generateToken(String.valueOf(user.getId()));
		mvc.perform(post("/auth/validate").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.userId").value(String.valueOf(user.getId())))
				.andExpect(jsonPath("$.role").value(expectedRole));
	}

	@Test
	void validateReturnsTheUsersRole() throws Exception {
		assertValidateRole(save("plain-user", null), "USER");
		assertValidateRole(save("an-admin", Role.ADMIN), "ADMIN");
	}

	@Test
	void validateTreatsAMissingRoleAsUser() throws Exception {
		User legacy = save("legacy-user", null);
		legacy.setRole(null); // rows created before the role column existed
		users.save(legacy);

		assertValidateRole(legacy, "USER");
	}
}
