package com.olamireDev.threelineswallet.controller;

import com.olamireDev.threelineswallet.data.dto.CreateUserRequestDTO;
import com.olamireDev.threelineswallet.data.dto.LoginRequestDTO;
import com.olamireDev.threelineswallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void register_validRequest_returns200WithToken() throws Exception {
        var request = new CreateUserRequestDTO("jane@example.com", "P@ssw0rd!", "Jane Doe");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.name").value("Jane Doe"));
    }

    @Test
    void register_passwordIsHashedInDatabase() throws Exception {
        var request = new CreateUserRequestDTO("jane@example.com", "P@ssw0rd!", "Jane Doe");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var saved = userRepository.findByEmail("jane@example.com").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getPassword()).isNotEqualTo("P@ssw0rd!");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("P@ssw0rd!", saved.getPassword())).isTrue();
    }

    @Test
    void register_invalidEmail_expectedToReturn400() throws Exception {
        var request = new CreateUserRequestDTO("not-an-email", "P@ssw0rd!", "Jane Doe");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankPassword_expectedToReturn400() throws Exception {
        var request = new CreateUserRequestDTO("jane@example.com", "", "Jane Doe");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_doesNotReturn200() throws Exception {
        var request = new CreateUserRequestDTO("dup@example.com", "P@ssw0rd!", "First");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var secondRequest = new CreateUserRequestDTO("dup@example.com", "AnotherPass1!", "Second");

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotEqualTo(200));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        registerUser("jane@example.com", "P@ssw0rd!", "Jane Doe");

        var loginRequest = new LoginRequestDTO("jane@example.com", "P@ssw0rd!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.name").value("Jane Doe"));
    }

    @Test
    void login_unknownEmail_doesNotReturn200() throws Exception {
        var loginRequest = new LoginRequestDTO("ghost@example.com", "whatever1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotEqualTo(200));
    }

    @Test
    void login_wrongPassword_doesNotReturn200() throws Exception {
        registerUser("jane@example.com", "P@ssw0rd!", "Jane Doe");

        var loginRequest = new LoginRequestDTO("jane@example.com", "totallyWrong1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void register_withoutCsrfToken_isNotRejectedForCsrfReasons() throws Exception {
        var request = new CreateUserRequestDTO("nocsrf@example.com", "P@ssw0rd!", "No Csrf");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    private void registerUser(String email, String password, String name) throws Exception {
        var request = new CreateUserRequestDTO(email, password, name);
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

}