package com.mingle.event_service;

import com.mingle.event_service.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EventAdminOnlyTest {

    private static final String ROLE = "X-User-Role";
    private static final String BODY = "{\"title\":\"t\"}";

    @Autowired MockMvc mvc;
    @MockBean EventService eventService;

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b) {
        return b.contentType(MediaType.APPLICATION_JSON).content(BODY);
    }

    @Test
    void writesWithoutAdminRoleAreForbidden() throws Exception {
        mvc.perform(json(post("/event"))).andExpect(status().isForbidden());
        mvc.perform(json(put("/event/1"))).andExpect(status().isForbidden());
        mvc.perform(delete("/event/1")).andExpect(status().isForbidden());
        mvc.perform(post("/event/import").param("city", "Berlin")).andExpect(status().isForbidden());

        mvc.perform(json(post("/event")).header(ROLE, "USER")).andExpect(status().isForbidden());
        mvc.perform(delete("/event/1").header(ROLE, "USER")).andExpect(status().isForbidden());

        verify(eventService, never()).createEvent(any());
        verify(eventService, never()).deleteEvent(any());
        verify(eventService, never()).importEventsFromTicketmaster(any());
    }

    @Test
    void adminCanWrite() throws Exception {
        mvc.perform(json(post("/event")).header(ROLE, "ADMIN")).andExpect(status().isCreated());
        mvc.perform(json(put("/event/1")).header(ROLE, "ADMIN")).andExpect(status().isOk());
        mvc.perform(delete("/event/1").header(ROLE, "ADMIN")).andExpect(status().isNoContent());
        mvc.perform(post("/event/import").param("city", "Berlin").header(ROLE, "ADMIN")).andExpect(status().isOk());
    }

    @Test
    void readsStayOpenToEveryone() throws Exception {
        mvc.perform(get("/event")).andExpect(status().isOk());
        mvc.perform(get("/event/1")).andExpect(status().isOk());
    }
}
