package com.worknexa.workflow;

import com.worknexa.user.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_stages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workflowId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StageType stageType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Role assignedRole; // Nullable for SUBMISSION, AI_ANALYSIS, COMPLETION

    @Column(nullable = false)
    private Integer orderIndex;
}
