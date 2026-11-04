package com.mingle.event_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real service and mapper against PostgreSQL: the generated mapper must copy every field. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EventCrudTest {

    @Autowired MockMvc mvc;

    @Test
    void adminCreatedEventKeepsItsFieldsAndCanBeReadBack() throws Exception {
        String body = "{\"title\":\"Concert\",\"description\":\"d\",\"city\":\"Istanbul\",\"venue\":\"Hall\","
                + "\"startTime\":\"2030-01-01T10:00:00\",\"endTime\":\"2030-01-01T12:00:00\",\"imageUrl\":\"http://x/y.png\"}";

        MvcResult created = mvc.perform(post("/event").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Concert"))
                .andExpect(jsonPath("$.city").value("Istanbul"))
                .andExpect(jsonPath("$.endTime").value("2030-01-01T12:00:00"))
                .andReturn();

        String json = created.getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":(\\d+).*", "$1");
        assertThat(id).matches("\\d+");

        mvc.perform(get("/event/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Concert"))
                .andExpect(jsonPath("$.venue").value("Hall"));
    }
}
