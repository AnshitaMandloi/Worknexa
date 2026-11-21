package com.worknexa.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequestDto {

    @NotBlank(message = "Workflow name is required")
    @Size(min = 2, max = 120, message = "Workflow name must be between 2 and 120 characters")
    private String name;

    @NotBlank(message = "Request type is required (e.g. EXPENSE, LEAVE)")
    @Size(min = 2, max = 50, message = "Request type must be between 2 and 50 characters")
    private String requestType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
