package com.worknexa.workflow;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards around the approval chain: separation of duties, role checks,
 * chain clean-up on rejection, and the request-info round trip.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApprovalGuardsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token(String email) throws Exception {
        LoginRequestDto req = new LoginRequestDto(email, "Password@123", null);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    private long submit(String token, double amount, boolean doc) throws Exception {
        String payload = String.format("""
                {"title":"Guard test request","amount":%s,"category":"EXPENSE",
                 "description":"guard test","documentAttached":%s,"documentName":"%s"}
                """, amount, doc, doc ? "proof.pdf" : "");
        MvcResult res = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private JsonNode detail(String token, long requestId) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/requests/" + requestId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
    }

    private void decide(String token, long taskId, String decision, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/tasks/" + taskId + "/decision")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"" + decision + "\",\"comment\":\"test\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("A manager cannot approve a request that they submitted themselves")
    void requesterCannotDecideOwnRequest() throws Exception {
        String mgr = token("manager@acme.com");
        long requestId = submit(mgr, 35000, true);

        long taskId = detail(mgr, requestId).path("tasks").get(0).path("id").asLong();
        decide(mgr, taskId, "APPROVE", 403);

        mockMvc.perform(get("/api/requests/" + requestId).header("Authorization", "Bearer " + mgr))
                .andExpect(jsonPath("$.data.request.status").value("PENDING_REVIEW"));
    }

    @Test
    @DisplayName("A role that does not match the task cannot decide it")
    void wrongRoleCannotDecide() throws Exception {
        String emp = token("employee@acme.com");
        String fin = token("finance@acme.com");
        long requestId = submit(emp, 35000, true);

        long managerTaskId = detail(emp, requestId).path("tasks").get(0).path("id").asLong();
        decide(fin, managerTaskId, "APPROVE", 403);
    }

    @Test
    @DisplayName("Rejecting at step 1 cancels the remaining tasks in the chain")
    void rejectCancelsRemainingTasks() throws Exception {
        String emp = token("employee@acme.com");
        String mgr = token("manager@acme.com");
        long requestId = submit(emp, 200000, true);

        JsonNode before = detail(emp, requestId);
        assertEquals(3, before.path("tasks").size());
        decide(mgr, before.path("tasks").get(0).path("id").asLong(), "REJECT", 200);

        JsonNode after = detail(emp, requestId);
        assertEquals("REJECTED", after.path("request").path("status").asText());
        assertEquals("REJECTED", after.path("tasks").get(0).path("status").asText());
        assertEquals("CANCELLED", after.path("tasks").get(1).path("status").asText());
        assertEquals("CANCELLED", after.path("tasks").get(2).path("status").asText());
    }

    @Test
    @DisplayName("Request info -> requester responds -> task is pending again")
    void requestInfoRoundTrip() throws Exception {
        String emp = token("employee@acme.com");
        String mgr = token("manager@acme.com");
        long requestId = submit(emp, 35000, true);

        long taskId = detail(emp, requestId).path("tasks").get(0).path("id").asLong();
        decide(mgr, taskId, "REQUEST_INFO", 200);

        mockMvc.perform(get("/api/requests/" + requestId).header("Authorization", "Bearer " + emp))
                .andExpect(jsonPath("$.data.request.status").value("INFO_REQUESTED"));

        mockMvc.perform(post("/api/requests/" + requestId + "/respond-info")
                        .header("Authorization", "Bearer " + emp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Attached the missing quote\",\"documentAttached\":true,\"documentName\":\"quote.pdf\"}"))
                .andExpect(status().isOk());

        JsonNode after = detail(emp, requestId);
        assertEquals("PENDING_REVIEW", after.path("request").path("status").asText());
        assertEquals("PENDING", after.path("tasks").get(0).path("status").asText());
    }

    @Test
    @DisplayName("An employee cannot open the approval inbox")
    void employeeCannotSeeApprovalInbox() throws Exception {
        String emp = token("employee@acme.com");
        mockMvc.perform(get("/api/tasks/pending").header("Authorization", "Bearer " + emp))
                .andExpect(status().isForbidden());
    }
}