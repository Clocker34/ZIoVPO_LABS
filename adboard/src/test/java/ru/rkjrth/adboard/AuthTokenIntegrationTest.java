package ru.rkjrth.adboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.rkjrth.adboard.entity.SessionStatus;
import ru.rkjrth.adboard.entity.User;
import ru.rkjrth.adboard.repository.UserRepository;
import ru.rkjrth.adboard.repository.UserSessionRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clean() {
        userSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginAccessRefreshRotateAndRejectOldRefresh() throws Exception {
        User u = new User();
        u.setUsername("jwtuser");
        u.setEmail("jwtuser@test.local");
        u.setName("JWT User");
        u.setPasswordHash(passwordEncoder.encode("Secret1!x"));
        u.setRole(User.Role.USER);
        userRepository.save(u);

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jwtuser\",\"password\":\"Secret1!x\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(login.getResponse().getContentAsString());
        String access = loginJson.get("accessToken").asText();
        String refresh = loginJson.get("refreshToken").asText();

        mockMvc.perform(get("/api/ping")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());

        assertThat(userSessionRepository.findAll()).hasSize(1);
        assertThat(userSessionRepository.findAll().get(0).getStatus()).isEqualTo(SessionStatus.ACTIVE);

        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refresh));
        MvcResult refreshCall = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshCall.getResponse().getContentAsString());
        String newRefresh = refreshJson.get("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());

        assertThat(userSessionRepository.findAll()).hasSize(2);
        long replaced = userSessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.REPLACED)
                .count();
        long active = userSessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                .count();
        assertThat(replaced).isEqualTo(1);
        assertThat(active).isEqualTo(1);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", newRefresh))))
                .andExpect(status().isOk());
    }
}
