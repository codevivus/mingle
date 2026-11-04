package com.mingle.ticket_service;

import com.mingle.ticket_service.client.EventServiceClient;
import com.mingle.ticket_service.client.UserServiceClient;
import com.mingle.ticket_service.client.dto.response.EventResponse;
import com.mingle.ticket_service.client.dto.response.UserResponse;
import com.mingle.ticket_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class TicketOwnershipTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired MockMvc mvc;
    @Autowired TicketRepository ticketRepository;
    @MockBean EventServiceClient eventServiceClient;
    @MockBean UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        when(eventServiceClient.findById(anyLong())).thenReturn(ResponseEntity.ok(new EventResponse()));
        when(userServiceClient.findById(anyLong())).thenReturn(ResponseEntity.ok(new UserResponse()));
    }

    private long createTicket(long asUser, String body) throws Exception {
        MvcResult result = mvc.perform(post("/ticket/add")
                        .header(USER_HEADER, asUser)
                        .header("X-User-Verified", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return ticketRepository.findAll().stream()
                .filter(t -> t.getUserId() == asUser)
                .mapToLong(t -> t.getId()).max().orElseThrow();
    }

    @Test
    void addIgnoresUserIdInBodyAndUsesHeader() throws Exception {
        long id = createTicket(1, "{\"eventId\": 10, \"userId\": 999}");

        assertThat(ticketRepository.findById(id).orElseThrow().getUserId()).isEqualTo(1L);
        assertThat(ticketRepository.findByUserId(999L)).isEmpty();
    }

    @Test
    void missingHeaderIsUnauthorized() throws Exception {
        mvc.perform(get("/ticket")).andExpect(status().isUnauthorized());
        mvc.perform(post("/ticket/add").contentType(MediaType.APPLICATION_JSON).content("{\"eventId\": 10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listOnlyReturnsCallersTickets() throws Exception {
        createTicket(1, "{\"eventId\": 10}");
        createTicket(2, "{\"eventId\": 10}");

        mvc.perform(get("/ticket").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/ticket/event/10").header(USER_HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listingAnotherUsersTicketsIsForbidden() throws Exception {
        mvc.perform(get("/ticket/user/2").header(USER_HEADER, 1)).andExpect(status().isForbidden());
        mvc.perform(get("/ticket/user/1").header(USER_HEADER, 1)).andExpect(status().isOk());
    }

    @Test
    void readingAnotherUsersTicketIsForbidden() throws Exception {
        long id = createTicket(1, "{\"eventId\": 10}");

        mvc.perform(get("/ticket/" + id).header(USER_HEADER, 2)).andExpect(status().isForbidden());
        mvc.perform(get("/ticket/" + id).header(USER_HEADER, 1)).andExpect(status().isOk());
    }

    @Test
    void onlyOwnerCanDelete() throws Exception {
        long id = createTicket(1, "{\"eventId\": 10}");

        mvc.perform(delete("/ticket/" + id).header(USER_HEADER, 2)).andExpect(status().isForbidden());
        assertThat(ticketRepository.existsById(id)).isTrue();

        mvc.perform(delete("/ticket/" + id).header(USER_HEADER, 1)).andExpect(status().isNoContent());
        assertThat(ticketRepository.existsById(id)).isFalse();
    }

    @Test
    void internalHoldersListsDistinctUserIdsForEvent() throws Exception {
        createTicket(1, "{\"eventId\": 10}");
        createTicket(1, "{\"eventId\": 10}");
        createTicket(2, "{\"eventId\": 10}");
        createTicket(3, "{\"eventId\": 11}");

        mvc.perform(get("/internal/tickets/event/10/holders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder(1, 2)));
    }

    @Test
    void deletingMissingTicketIsNotFound() throws Exception {
        mvc.perform(delete("/ticket/123456").header(USER_HEADER, 1)).andExpect(status().isNotFound());
    }

    @Test
    void unverifiedUsersCannotAddTickets() throws Exception {
        mvc.perform(post("/ticket/add").header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"eventId\": 10}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/ticket/add").header(USER_HEADER, 1).header("X-User-Verified", "false")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"eventId\": 10}"))
                .andExpect(status().isForbidden());
        assertThat(ticketRepository.findAll()).isEmpty();
    }
}
