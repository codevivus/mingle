package com.mingle.event_service;

import com.mingle.event_service.entity.Event;
import com.mingle.event_service.repository.EventRepository;
import com.mingle.event_service.scheduler.ExpiredEventCleanupScheduler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real import against a local stand-in for the Ticketmaster Discovery API. The fixture follows
 * the documented response shape: venues live under each event's own _embedded object.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class TicketmasterImportTest {

    private static final String EVENTS_JSON = """
            {
              "_embedded": {
                "events": [
                  {
                    "name": "Concert A", "id": "tm-1", "type": "event", "url": "https://tm/1",
                    "images": [ { "ratio": "16_9", "url": "https://img/1.jpg" } ],
                    "dates": { "start": { "localDate": "2030-05-01", "localTime": "20:00:00" }, "timezone": "Europe/Istanbul" },
                    "_embedded": { "venues": [ { "name": "Hall A", "city": { "name": "Istanbul" }, "country": { "countryCode": "TR" } } ] }
                  },
                  {
                    "name": "Festival B", "id": "tm-2",
                    "dates": { "start": { "localDate": "2030-06-01", "timeTBA": true } },
                    "_embedded": { "venues": [ { "name": "Park", "city": { "name": "Ankara" } } ] }
                  }
                ]
              },
              "page": { "size": 50, "totalElements": 2, "totalPages": 1, "number": 0 }
            }
            """;

    private static final AtomicReference<String> BODY = new AtomicReference<>(EVENTS_JSON);
    private static final AtomicInteger STATUS = new AtomicInteger(200);
    private static final HttpServer SERVER = startStub();

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                byte[] bytes = BODY.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(STATUS.get(), bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void ticketmaster(DynamicPropertyRegistry registry) {
        registry.add("ticketmaster.api.base-url", () -> "http://localhost:" + SERVER.getAddress().getPort());
        registry.add("ticketmaster.api.retryMaxAttempts", () -> "0");
    }

    @AfterAll
    static void stopStub() {
        SERVER.stop(0);
    }

    @Autowired MockMvc mvc;
    @Autowired EventRepository events;
    @Autowired ExpiredEventCleanupScheduler cleanup;

    @BeforeEach
    void reset() {
        events.deleteAll();
        BODY.set(EVENTS_JSON);
        STATUS.set(200);
    }

    private void importCity() throws Exception {
        mvc.perform(post("/event/import").param("city", "Istanbul").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void importKeepsTitleVenueCityDateAndImage() throws Exception {
        importCity();

        Event concert = events.findAllByExternalIdIn(List.of("tm-1")).get(0);
        assertThat(concert.getTitle()).isEqualTo("Concert A");
        assertThat(concert.getVenue()).isEqualTo("Hall A");
        assertThat(concert.getCity()).isEqualTo("Istanbul");
        assertThat(concert.getStartTime()).isEqualTo(LocalDateTime.of(2030, 5, 1, 20, 0));
        assertThat(concert.getImageUrl()).isEqualTo("https://img/1.jpg");
        assertThat(concert.isImported()).isTrue();
    }

    @Test
    void anEventWithoutATimeIsStillImported() throws Exception {
        importCity();

        Event festival = events.findAllByExternalIdIn(List.of("tm-2")).get(0);
        assertThat(festival.getTitle()).isEqualTo("Festival B");
        assertThat(festival.getCity()).isEqualTo("Ankara");
        assertThat(festival.getStartTime()).isNull();
    }

    @Test
    void importingTwiceDoesNotDuplicate() throws Exception {
        importCity();
        importCity();

        assertThat(events.findAll()).hasSize(2);
    }

    @Test
    void anEmptyAnswerImportsNothing() throws Exception {
        BODY.set("{\"page\":{\"size\":50,\"totalElements\":0,\"totalPages\":0,\"number\":0}}");

        importCity();

        assertThat(events.findAll()).isEmpty();
    }

    @Test
    void aTicketmasterFailureIsAnUpstreamError() throws Exception {
        STATUS.set(500);
        BODY.set("{}");

        mvc.perform(post("/event/import").param("city", "Istanbul").header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadGateway());
        assertThat(events.findAll()).isEmpty();
    }

    @Test
    void cleanupRemovesOnlyExpiredImportedEvents() {
        events.save(imported("old-no-end", null, LocalDateTime.now().minusDays(30), true));
        events.save(imported("old-with-end", LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(21), true));
        events.save(imported("future", null, LocalDateTime.now().plusDays(10), true));
        events.save(imported("manual-old", LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(31), false));

        cleanup.deleteExpiredImportedEvents();

        assertThat(events.findAll()).extracting(Event::getExternalId).containsExactlyInAnyOrder("future", "manual-old");
    }

    private Event imported(String externalId, LocalDateTime end, LocalDateTime start, boolean imported) {
        Event e = new Event();
        e.setExternalId(externalId);
        e.setTitle(externalId);
        e.setCity("Istanbul");
        e.setStartTime(start);
        e.setEndTime(end);
        e.setImported(imported);
        return e;
    }
}
