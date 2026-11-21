package com.worknexa.rule;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "business_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long workflowId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String field; // amount, documentAttached, riskScore

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleOperator operator; // LTE, LT, GT, GTE, EQ, NEQ, BETWEEN

    @Column(name = "rule_value", nullable = false, length = 100)
    private String value;

    @Column(name = "rule_value_max", length = 100)
    private String valueMax; // for BETWEEN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RuleAction action; // AUTO_APPROVE, MANAGER_REVIEW, FINANCE_REVIEW, etc.

    @Column(nullable = false)
    private Integer priority; // 1 is highest priority

    @Column(nullable = false)
    @Builder.Default
    private boolean mandatory = false; // true = overrides AI

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
