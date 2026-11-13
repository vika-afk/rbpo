package com.example.lb3;

import com.example.lb3.entity.Performance;
import com.example.lb3.entity.PerformanceStatus;
import com.example.lb3.entity.Play;
import com.example.lb3.entity.TheatreHall;
import com.example.lb3.entity.Ticket;
import com.example.lb3.entity.TicketStatus;
import com.example.lb3.entity.User;
import com.example.lb3.repository.PerformanceRepository;
import com.example.lb3.repository.PlayRepository;
import com.example.lb3.repository.TheatreHallRepository;
import com.example.lb3.repository.TicketRepository;
import com.example.lb3.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    private static final String PASSWORD = "Password1!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private PlayRepository playRepository;
    @Autowired private TheatreHallRepository hallRepository;
    @Autowired private PerformanceRepository performanceRepository;
    @Autowired private TicketRepository ticketRepository;

    @Test
    void publicEndpointsAreAvailableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/plays"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/theatre-halls"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/performances"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutAuthenticationReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void wrongPasswordReturnsJson401() throws Exception {
        createUser("wrong-password@example.test", "USER", false);

        mockMvc.perform(get("/api/users").with(httpBasic("wrong-password@example.test", "WrongPass1!")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void blockedUserCannotAuthenticate() throws Exception {
        createUser("blocked@example.test", "USER", true);

        mockMvc.perform(get("/api/auth/me").with(httpBasic("blocked@example.test", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void userCannotAccessAdminEndpointButAdminCan() throws Exception {
        createUser("plain-user@example.test", "USER", false);
        createUser("admin-user@example.test", "ADMIN", false);

        mockMvc.perform(get("/api/users").with(httpBasic("plain-user@example.test", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(get("/api/users").with(httpBasic("admin-user@example.test", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void stateChangingRequestWithoutCsrfIsRejected() throws Exception {
        createUser("csrf-manager@example.test", "MANAGER", false);

        mockMvc.perform(post("/api/plays")
                        .with(httpBasic("csrf-manager@example.test", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreatePlayWithCsrf() throws Exception {
        createUser("create-manager@example.test", "MANAGER", false);
        CsrfData csrf = csrf();

        mockMvc.perform(post("/api/plays")
                        .with(httpBasic("create-manager@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Security play"));
    }

    @Test
    void registrationRequiresCsrfAndCreatesOnlyRegularUser() throws Exception {
        String body = registerJson("new-user@example.test", "Password1!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        CsrfData csrf = csrf();
        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registrationIgnoresRoleField() throws Exception {
        CsrfData csrf = csrf();
        String body = "{\"name\":\"New user\",\"email\":\"role-attempt@example.test\","
                + "\"password\":\"Password1!\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registrationRejectsWeakPassword() throws Exception {
        CsrfData csrf = csrf();

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("weak@example.test", "pass123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationRejectsDuplicateEmailIgnoringCase() throws Exception {
        CsrfData csrf = csrf();
        String first = registerJson("duplicate@example.test", PASSWORD);
        String second = registerJson("DUPLICATE@example.test", PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(second))
                .andExpect(status().isConflict());
    }

    @Test
    void userCannotReadAllTicketsButCanReadOwnTickets() throws Exception {
        createUser("tickets-user@example.test", "USER", false);

        mockMvc.perform(get("/api/tickets").with(httpBasic("tickets-user@example.test", PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tickets/my").with(httpBasic("tickets-user@example.test", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotModifyTicketDirectly() throws Exception {
        User owner = createUser("direct-owner@example.test", "USER", false);
        Ticket ticket = createSoldTicket(owner);

        mockMvc.perform(patch("/api/tickets/" + ticket.getId())
                        .with(httpBasic("direct-owner@example.test", PASSWORD))
                        .with(csrfOrCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatNumber\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanRefundOwnTicketOnly() throws Exception {
        User owner = createUser("refund-owner@example.test", "USER", false);
        User stranger = createUser("refund-stranger@example.test", "USER", false);
        Ticket ticket = createSoldTicket(owner);
        CsrfData csrf = csrf();

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/refund")
                        .with(httpBasic("refund-stranger@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/refund")
                        .with(httpBasic("refund-owner@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void staffCanRefundAnyTicket() throws Exception {
        User owner = createUser("staff-refund-owner@example.test", "USER", false);
        createUser("staff-refund-admin@example.test", "ADMIN", false);
        Ticket ticket = createSoldTicket(owner);
        CsrfData csrf = csrf();

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/refund")
                        .with(httpBasic("staff-refund-admin@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void userCannotPurchaseTicketForAnotherUser() throws Exception {
        createUser("purchase-user@example.test", "USER", false);
        User other = createUser("purchase-other@example.test", "USER", false);
        Performance performance = createPerformance();
        CsrfData csrf = csrf();

        String body = "{\"performanceId\":" + performance.getId()
                + ",\"userId\":" + other.getId() + ",\"seatNumber\":1}";
        mockMvc.perform(post("/api/tickets/purchase")
                        .with(httpBasic("purchase-user@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanPurchaseTicketForSelf() throws Exception {
        User buyer = createUser("purchase-self@example.test", "USER", false);
        Performance performance = createPerformance();
        CsrfData csrf = csrf();

        String body = "{\"performanceId\":" + performance.getId()
                + ",\"userId\":" + buyer.getId() + ",\"seatNumber\":2}";
        mockMvc.perform(post("/api/tickets/purchase")
                        .with(httpBasic("purchase-self@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.userId").value(buyer.getId()));
    }

    @Test
    void onlyStaffCanArchivePlay() throws Exception {
        createUser("archive-user@example.test", "USER", false);
        createUser("archive-manager@example.test", "MANAGER", false);
        Play play = createPlay();
        CsrfData csrf = csrf();

        mockMvc.perform(post("/api/plays/" + play.getId() + "/archive")
                        .with(httpBasic("archive-user@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/plays/" + play.getId() + "/archive")
                        .with(httpBasic("archive-manager@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void adminCanCreateBlockAndUnblockUser() throws Exception {
        createUser("state-admin@example.test", "ADMIN", false);
        CsrfData csrf = csrf();

        String body = "{\"name\":\"Managed\",\"email\":\"managed@example.test\","
                + "\"password\":\"Password1!\",\"role\":\"MANAGER\"}";
        MvcResult created = mockMvc.perform(post("/api/users")
                        .with(httpBasic("state-admin@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/users/" + id)
                        .with(httpBasic("state-admin@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blocked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        mockMvc.perform(get("/api/auth/me").with(httpBasic("managed@example.test", PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/users/" + id)
                        .with(httpBasic("state-admin@example.test", PASSWORD))
                        .cookie(csrf.cookie)
                        .header("X-XSRF-TOKEN", csrf.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blocked\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));

        mockMvc.perform(get("/api/auth/me").with(httpBasic("managed@example.test", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    private User createUser(String email, String role, boolean blocked) {
        User user = new User();
        user.setName("Test " + role);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setEnabled(true);
        user.setBlocked(blocked);
        return userRepository.saveAndFlush(user);
    }

    private Play createPlay() {
        Play play = new Play();
        play.setTitle("Security play");
        play.setDurationMinutes(90);
        play.setAgeLimit(12);
        play.setArchived(false);
        return playRepository.saveAndFlush(play);
    }

    private Performance createPerformance() {
        TheatreHall hall = new TheatreHall();
        hall.setName("Hall-" + UUID.randomUUID());
        hall.setCapacity(50);
        hall = hallRepository.saveAndFlush(hall);

        Performance performance = new Performance();
        performance.setPlay(createPlay());
        performance.setHall(hall);
        performance.setStartTime(LocalDateTime.now().plusDays(10));
        performance.setBasePrice(new BigDecimal("1000.00"));
        performance.setStatus(PerformanceStatus.SCHEDULED);
        return performanceRepository.saveAndFlush(performance);
    }

    private Ticket createSoldTicket(User owner) {
        Ticket ticket = new Ticket();
        ticket.setPerformance(createPerformance());
        ticket.setUser(owner);
        ticket.setSeatNumber(1);
        ticket.setStatus(TicketStatus.SOLD);
        return ticketRepository.saveAndFlush(ticket);
    }

    private CsrfData csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        return new CsrfData(cookie, token);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor csrfOrCookie() throws Exception {
        CsrfData csrf = csrf();
        return request -> {
            request.setCookies(csrf.cookie);
            request.addHeader("X-XSRF-TOKEN", csrf.token);
            return request;
        };
    }

    private String playJson() {
        return "{\"title\":\"Security play\",\"durationMinutes\":90,\"ageLimit\":12,\"archived\":false}";
    }

    private String registerJson(String email, String password) {
        return "{\"name\":\"New user\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static class CsrfData {
        private final Cookie cookie;
        private final String token;

        private CsrfData(Cookie cookie, String token) {
            this.cookie = cookie;
            this.token = token;
        }
    }
}
