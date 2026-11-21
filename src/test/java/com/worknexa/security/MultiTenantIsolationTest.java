package com.worknexa.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worknexa.auth.dto.LoginRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiTenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto(email, password, null);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).path("data").path("token").asText();
    }

    @Test
    @DisplayName("Tenant isolation: Company B employee cannot access Company A requests")
    void testTenantCannotAccessOtherTenantData() throws Exception {
        String acmeToken = loginAndGetToken("employee@acme.com", "Password@123");
        String globexToken = loginAndGetToken("employee@globex.com", "Password@123");

        // 1. Acme employee submits a request
        String createRequestJson = """
                {
                    "title": "Confidential Project Budget",
                    "amount": 7500.00,
                    "category": "EXPENSE",
                    "description": "Internal Acme materials",
                    "documentAttached": true,
                    "documentName": "receipt.pdf"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + acmeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        long acmeRequestId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Acme employee can read their request
        mockMvc.perform(get("/api/requests/" + acmeRequestId)
                        .header("Authorization", "Bearer " + acmeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request.id").value(acmeRequestId));

        // 3. Globex employee attempts to read Acme's request -> must be 404 or 403
        mockMvc.perform(get("/api/requests/" + acmeRequestId)
                        .header("Authorization", "Bearer " + globexToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant isolation: Globex Admin cannot read Acme users")
    void testAdminCannotAccessOtherTenantUsers() throws Exception {
        String globexAdminToken = loginAndGetToken("admin@globex.com", "Password@123");

        // Globex admin listing users only sees Globex users
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + globexAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.email == 'admin@acme.com')]").doesNotExist());
    }
}
