package com.worknexa.seeder;

import com.worknexa.rule.BusinessRule;
import com.worknexa.rule.BusinessRuleRepository;
import com.worknexa.rule.RuleAction;
import com.worknexa.rule.RuleOperator;
import com.worknexa.tenant.Tenant;
import com.worknexa.tenant.TenantRepository;
import com.worknexa.user.Role;
import com.worknexa.user.User;
import com.worknexa.user.UserRepository;
import com.worknexa.workflow.StageType;
import com.worknexa.workflow.WorkflowDefinition;
import com.worknexa.workflow.WorkflowDefinitionRepository;
import com.worknexa.workflow.WorkflowStage;
import com.worknexa.workflow.WorkflowStageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final WorkflowDefinitionRepository workflowRepository;
    private final WorkflowStageRepository stageRepository;
    private final BusinessRuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (tenantRepository.count() > 0) {
            log.info("Database already seeded. Skipping DataSeeder.");
            return;
        }

        log.info("Seeding demo tenants and default workflows...");
        seedAcmeCorp();
        seedGlobex();
        log.info("Data seeding completed successfully!");
    }

    private void seedAcmeCorp() {
        Tenant acme = Tenant.builder()
                .name("Acme Corp")
                .build();
        acme = tenantRepository.save(acme);

        String encodedPassword = passwordEncoder.encode("Password@123");

        // Seed users for Acme Corp
        userRepository.save(User.builder().tenantId(acme.getId()).name("Acme Admin").email("admin@acme.com").passwordHash(encodedPassword).role(Role.ADMIN).active(true).build());
        userRepository.save(User.builder().tenantId(acme.getId()).name("Acme Employee").email("employee@acme.com").passwordHash(encodedPassword).role(Role.EMPLOYEE).active(true).build());
        userRepository.save(User.builder().tenantId(acme.getId()).name("Acme Manager").email("manager@acme.com").passwordHash(encodedPassword).role(Role.MANAGER).active(true).build());
        userRepository.save(User.builder().tenantId(acme.getId()).name("Acme Finance").email("finance@acme.com").passwordHash(encodedPassword).role(Role.FINANCE).active(true).build());
        userRepository.save(User.builder().tenantId(acme.getId()).name("Acme Compliance").email("compliance@acme.com").passwordHash(encodedPassword).role(Role.COMPLIANCE).active(true).build());

        // Seed Expense Approval Workflow for Acme Corp
        WorkflowDefinition expenseWorkflow = WorkflowDefinition.builder()
                .tenantId(acme.getId())
                .name("Expense Approval")
                .requestType("EXPENSE")
                .description("Automated and multi-stage governed corporate expense approval workflow.")
                .active(true)
                .build();
        expenseWorkflow = workflowRepository.save(expenseWorkflow);

        // Stages: SUBMISSION (1), AI_ANALYSIS (2), HUMAN_REVIEW (3), COMPLETION (4)
        stageRepository.save(WorkflowStage.builder().workflowId(expenseWorkflow.getId()).name("Request Submission").stageType(StageType.SUBMISSION).orderIndex(1).build());
        stageRepository.save(WorkflowStage.builder().workflowId(expenseWorkflow.getId()).name("AI Risk Analysis").stageType(StageType.AI_ANALYSIS).orderIndex(2).build());
        stageRepository.save(WorkflowStage.builder().workflowId(expenseWorkflow.getId()).name("Managerial Review").stageType(StageType.HUMAN_REVIEW).assignedRole(Role.MANAGER).orderIndex(3).build());
        stageRepository.save(WorkflowStage.builder().workflowId(expenseWorkflow.getId()).name("Process Completion").stageType(StageType.COMPLETION).orderIndex(4).build());

        // Seed Default Business Rules (from Section 5B)
        // 1. amount <= 10000 -> AUTO_APPROVE
        ruleRepository.save(BusinessRule.builder()
                .tenantId(acme.getId())
                .workflowId(expenseWorkflow.getId())
                .name("Low Value Auto-Approval")
                .field("amount")
                .operator(RuleOperator.LTE)
                .value("10000")
                .action(RuleAction.AUTO_APPROVE)
                .priority(10)
                .mandatory(false)
                .active(true)
                .build());

        // 2. 10000 < amount <= 50000 -> MANAGER_REVIEW
        ruleRepository.save(BusinessRule.builder()
                .tenantId(acme.getId())
                .workflowId(expenseWorkflow.getId())
                .name("Mid Value Manager Review")
                .field("amount")
                .operator(RuleOperator.BETWEEN)
                .value("10000")
                .valueMax("50000")
                .action(RuleAction.MANAGER_REVIEW)
                .priority(20)
                .mandatory(false)
                .active(true)
                .build());

        // 3. amount > 50000 -> FINANCE_REVIEW (mandatory)
        ruleRepository.save(BusinessRule.builder()
                .tenantId(acme.getId())
                .workflowId(expenseWorkflow.getId())
                .name("High Value Finance Oversight")
                .field("amount")
                .operator(RuleOperator.GT)
                .value("50000")
                .action(RuleAction.FINANCE_REVIEW)
                .priority(30)
                .mandatory(true)
                .active(true)
                .build());

        // 4. documentAttached == false -> MANUAL_REVIEW (mandatory)
        ruleRepository.save(BusinessRule.builder()
                .tenantId(acme.getId())
                .workflowId(expenseWorkflow.getId())
                .name("Missing Receipt Manual Audit")
                .field("documentAttached")
                .operator(RuleOperator.EQ)
                .value("false")
                .action(RuleAction.MANUAL_REVIEW)
                .priority(5)
                .mandatory(true)
                .active(true)
                .build());

        // 5. AI risk HIGH -> COMPLIANCE_REVIEW (mandatory)
        ruleRepository.save(BusinessRule.builder()
                .tenantId(acme.getId())
                .workflowId(expenseWorkflow.getId())
                .name("High AI Risk Compliance Review")
                .field("riskLevel")
                .operator(RuleOperator.EQ)
                .value("HIGH")
                .action(RuleAction.COMPLIANCE_REVIEW)
                .priority(15)
                .mandatory(true)
                .active(true)
                .build());

        log.info("Seeded Acme Corp tenant (ID={}) with 5 users and Expense workflow.", acme.getId());
    }

    private void seedGlobex() {
        Tenant globex = Tenant.builder()
                .name("Globex")
                .build();
        globex = tenantRepository.save(globex);

        String encodedPassword = passwordEncoder.encode("Password@123");

        // Seed Globex users
        userRepository.save(User.builder().tenantId(globex.getId()).name("Globex Admin").email("admin@globex.com").passwordHash(encodedPassword).role(Role.ADMIN).active(true).build());
        userRepository.save(User.builder().tenantId(globex.getId()).name("Globex Employee").email("employee@globex.com").passwordHash(encodedPassword).role(Role.EMPLOYEE).active(true).build());

        // Seed Expense Approval Workflow for Globex
        WorkflowDefinition globexWorkflow = WorkflowDefinition.builder()
                .tenantId(globex.getId())
                .name("Standard Expense")
                .requestType("EXPENSE")
                .description("Globex internal expenses.")
                .active(true)
                .build();
        globexWorkflow = workflowRepository.save(globexWorkflow);

        stageRepository.save(WorkflowStage.builder().workflowId(globexWorkflow.getId()).name("Submission").stageType(StageType.SUBMISSION).orderIndex(1).build());
        stageRepository.save(WorkflowStage.builder().workflowId(globexWorkflow.getId()).name("AI Analysis").stageType(StageType.AI_ANALYSIS).orderIndex(2).build());
        stageRepository.save(WorkflowStage.builder().workflowId(globexWorkflow.getId()).name("Completion").stageType(StageType.COMPLETION).orderIndex(3).build());

        log.info("Seeded Globex tenant (ID={}) with 2 users for tenant isolation testing.", globex.getId());
    }
}