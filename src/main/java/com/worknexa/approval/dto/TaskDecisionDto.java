package com.worknexa.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDecisionDto {

    @NotBlank(message = "Decision is required (APPROVE, REJECT, REQUEST_INFO)")
    private String decision;

    private String comment;
}
