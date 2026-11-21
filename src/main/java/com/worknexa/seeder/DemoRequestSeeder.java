package com.worknexa.seeder;

import com.worknexa.approval.ApprovalTask;
import com.worknexa.approval.ApprovalTaskRepository;
import com.worknexa.approval.TaskStatus;
import com.worknexa.common.security.UserPrincipal;
import com.worknexa.request.WorkflowEngineService;
import com.worknexa.request.WorkflowRequest;
import com.worknexa.request.WorkflowRequestRepository;
import com.worknexa.tenant.Tenant;
import com.worknexa.tenant.TenantRepository;
import com.worknexa.user.User;
import com.worknexa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

/**
 * Demo-only: pushes a few sample requests through the REAL workflow engine
 * (AI analysis, rules, approvals, audit) so the dashboard is not empty.
 * Runs once, only when no requests exist. Disable with worknexa.seed.demo-requests=false.
 * Use AI_MOCK_ENABLED=true (or the "local" profile) for predictable outcomes.
 */
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class DemoRequestSeeder implements CommandLineRunner {

    private static final String ACME = "Acme Corp";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final WorkflowRequestRepository requestRepository;
    private final ApprovalTaskRepository taskRepository;
    private final WorkflowEngineService engine;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${worknexa.seed.demo-requests:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled || requestRepository.count() > 0) {
            return;
        }
        Tenant acme = tenantRepository.findByNameIgnoreCase(ACME).orElse(null);
        if (acme == null) {
            return;
        }
        log.info("Seeding demo requests for {}...", ACME);
        try {
            // title, category, amount, document, hours ago
            Long lunch = submit(acme, "Team lunch - sprint demo", "EXPENSE", "4500", true, 70);
            Long dinner = submit(acme, "Client dinner - Q3 review", "TRAVEL", "18500", true, 52);
            Long licence = submit(acme, "Design software licence renewal", "SOFTWARE", "35000", false, 40);
            Long vendor = submit(acme, "Vendor advance payment", "EXPENSE", "120000", false, 28);
            Long cloud = submit(acme, "Cloud infrastructure credits", "SOFTWARE", "72000", true, 20);
            Long storage = submit(acme, "Data centre storage upgrade", "EQUIPMENT", "65000", true, 9);
            Long conf = submit(acme, "DevSummit conference registration", "TRAVEL", "28000", true, 3);

            decide(acme, "manager@acme.com", dinner, "APPROVE", "Within budget, receipt verified.");
            decide(acme, "manager@acme.com", licence, "REQUEST_INFO", "Please attach the vendor invoice.");
            decide(acme, "manager@acme.com", vendor, "REJECT", "No receipt and vendor is not on the approved list.");
            decide(acme, "manager@acme.com", cloud, "APPROVE", "Approved from my side, forwarding to Finance.");
            log.info("Demo requests seeded (ids: {}).", java.util.List.of(lunch, dinner, licence, vendor, cloud, storage, conf));
        } catch (Exception e) {
            log.warn("Demo request seeding stopped early: {}", e.getMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Long submit(Tenant tenant, String title, String category, String amount, boolean doc, long hoursAgo) {
        actAs(tenant, "employee@acme.com");
        WorkflowRequest request = WorkflowRequest.builder()
                .title(title)
                .category(category)
                .amount(new BigDecimal(amount))
                .description("Sample request for demo purposes.")
                .documentAttached(doc)
                .documentName(doc ? "receipt.pdf" : null)
                .build();
        WorkflowRequest saved = engine.submitRequest(request);

        // spread timestamps so lists look natural (created_at is not updatable via JPA, so use SQL)
        Instant when = Instant.now().minus(Duration.ofHours(hoursAgo));
        jdbcTemplate.update("UPDATE workflow_requests SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(when), saved.getId());
        return saved.getId();
    }

    private void decide(Tenant tenant, String email, Long requestId, String decision, String comment) {
        ApprovalTask task = taskRepository
                .findAllByTenantIdAndRequestIdOrderBySequenceNoAsc(tenant.getId(), requestId).stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .min(Comparator.comparing(ApprovalTask::getSequenceNo))
                .orElse(null);
        if (task == null) {
            log.info("Request {} has no pending task (auto-decided or AI fallback), skipping '{}'.", requestId, decision);
            return;
        }
        actAs(tenant, email);
        engine.processTaskDecision(task.getId(), decision, comment);
    }

    private void actAs(Tenant tenant, String email) {
        User u = userRepository.findByTenantIdAndEmailIgnoreCase(tenant.getId(), email).orElseThrow();
        UserPrincipal principal = UserPrincipal.builder()
                .id(u.getId()).tenantId(u.getTenantId()).email(u.getEmail())
                .password(u.getPasswordHash()).name(u.getName()).role(u.getRole()).active(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}