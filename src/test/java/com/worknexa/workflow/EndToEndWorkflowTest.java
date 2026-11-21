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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String getToken(String email, String password) throws Exception {
        LoginRequestDto req = new LoginRequestDto(email, password, null);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("token").asText();
    }

    @Test
    @DisplayName("Case 1: Expense Rs 8,000 with invoice -> Automatically APPROVED")
    void testCase1_LowExpenseAutoApproval() throws Exception {
        String empToken = getToken("employee@acme.com", "Password@123");

        String payload = """
                {
                    "title": "Office Supplies & Paper",
                    "amount": 8000.00,
                    "category": "EXPENSE",
                    "description": "Stationery replenishment",
                    "documentAttached": true,
                    "documentName": "invoice-stationery.pdf"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("AUTO_APPROVED"))
                .andReturn();

        long requestId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Verify audit timeline contains AI, Rule, Decision, and AUTO_APPROVED events
        MvcResult detailRes = mockMvc.perform(get("/api/requests/" + requestId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode detailJson = objectMapper.readTree(detailRes.getResponse().getContentAsString()).path("data");
        assertEquals("LOW", detailJson.path("aiAnalysis").path("riskLevel").asText());
        assertTrue(detailJson.path("timeline").size() >= 4);
    }

    @Test
    @DisplayName("Case 2: Expense Rs 35,000 -> Manager Review -> Approve -> APPROVED")
    void testCase2_MidExpenseManagerReview() throws Exception {
        String empToken = getToken("employee@acme.com", "Password@123");
        String mgrToken = getToken("manager@acme.com", "Password@123");

        String payload = """
                {
                    "title": "Regional Team Offsite Workshop",
                    "amount": 35000.00,
                    "category": "EXPENSE",
                    "description": "Quarterly planning session venue",
                    "documentAttached": true,
                    "documentName": "venue-booking.pdf"
                }
                """;

        MvcResult createRes = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn();

        long requestId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Manager checks pending tasks
        MvcResult pendingRes = mockMvc.perform(get("/api/tasks/pending")
                        .header("Authorization", "Bearer " + mgrToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tasksArray = objectMapper.readTree(pendingRes.getResponse().getContentAsString()).path("data");
        long taskId = -1;
        for (JsonNode t : tasksArray) {
            if (t.path("requestId").asLong() == requestId) {
                taskId = t.path("taskId").asLong();
                break;
            }
        }
        assertTrue(taskId > 0, "Manager should have pending task for this request");

        // Manager approves task
        String decisionPayload = """
                {
                    "decision": "APPROVE",
                    "comment": "Workshop budget verified and within department limits."
                }
                """;

        mockMvc.perform(post("/api/tasks/" + taskId + "/decision")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Verify request is now APPROVED
        mockMvc.perform(get("/api/requests/" + requestId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Case 3: Expense Rs 2,00,000 -> Multi-step Chain (Manager -> Finance) -> Final APPROVED")
    void testCase3_HighExpenseMultiStepChain() throws Exception {
        String empToken = getToken("employee@acme.com", "Password@123");
        String mgrToken = getToken("manager@acme.com", "Password@123");
        String finToken = getToken("finance@acme.com", "Password@123");
        String compToken = getToken("compliance@acme.com", "Password@123");

        String payload = """
                {
                    "title": "Enterprise Cloud Server Infrastructure",
                    "amount": 200000.00,
                    "category": "EXPENSE",
                    "description": "Annual server cluster renewal",
                    "documentAttached": true,
                    "documentName": "cloud-contract.pdf"
                }
                """;

        MvcResult createRes = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn();

        long requestId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Check request details: should show 3 tasks in sequence (Manager, Finance, Compliance)
        MvcResult detailRes = mockMvc.perform(get("/api/requests/" + requestId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode detailJson = objectMapper.readTree(detailRes.getResponse().getContentAsString()).path("data");
        assertEquals("HIGH", detailJson.path("aiAnalysis").path("riskLevel").asText());
        assertEquals(3, detailJson.path("tasks").size());

        long mgrTaskId = detailJson.path("tasks").get(0).path("id").asLong();
        long finTaskId = detailJson.path("tasks").get(1).path("id").asLong();
        long compTaskId = detailJson.path("tasks").get(2).path("id").asLong();

        // 1. Manager approves Step 1
        mockMvc.perform(post("/api/tasks/" + mgrTaskId + "/decision")
                        .header("Authorization", "Bearer " + mgrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"APPROVE\", \"comment\": \"Manager endorsement provided.\"}"))
                .andExpect(status().isOk());

        // Request should still be PENDING_REVIEW because Finance & Compliance have not decided
        mockMvc.perform(get("/api/requests/" + requestId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request.status").value("PENDING_REVIEW"));

        // 2. Finance approves Step 2
        mockMvc.perform(post("/api/tasks/" + finTaskId + "/decision")
                        .header("Authorization", "Bearer " + finToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"APPROVE\", \"comment\": \"Finance capital outlay validated.\"}"))
                .andExpect(status().isOk());

        // Request still PENDING_REVIEW because Compliance is step 3
        mockMvc.perform(get("/api/requests/" + requestId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request.status").value("PENDING_REVIEW"));

        // 3. Compliance approves Step 3
        mockMvc.perform(post("/api/tasks/" + compTaskId + "/decision")
                        .header("Authorization", "Bearer " + compToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"APPROVE\", \"comment\": \"Compliance audit verified.\"}"))
                .andExpect(status().isOk());

        // Final status is now APPROVED
        mockMvc.perform(get("/api/requests/" + requestId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Rule Override: Amount > 50,000 requires Finance review even if categorized as routine")
    void testMandatoryRuleOverridesRoutine() throws Exception {
        String empToken = getToken("employee@acme.com", "Password@123");

        String payload = """
                {
                    "title": "High Value Software Licenses",
                    "amount": 75000.00,
                    "category": "EXPENSE",
                    "description": "Design suite enterprise renewal",
                    "documentAttached": true,
                    "documentName": "license-quote.pdf"
                }
                """;

        MvcResult createRes = mockMvc.perform(post("/api/requests")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn();

        long requestId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Verify Finance review is included in tasks
        MvcResult detailRes = mockMvc.perform(get("/api/requests/" + requestId)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode detailJson = objectMapper.readTree(detailRes.getResponse().getContentAsString()).path("data");
        boolean hasFinanceTask = false;
        for (JsonNode task : detailJson.path("tasks")) {
            if ("FINANCE".equals(task.path("assignedRole").asText())) {
                hasFinanceTask = true;
                break;
            }
        }
        assertTrue(hasFinanceTask, "Finance review task must be assigned due to mandatory business rule");
    }
}
