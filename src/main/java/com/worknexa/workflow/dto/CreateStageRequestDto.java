package com.worknexa.workflow.dto;

import com.worknexa.user.Role;
import com.worknexa.workflow.StageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStageRequestDto {

    @NotBlank(message = "Stage name is required")
    @Size(min = 2, max = 120, message = "Stage name must be between 2 and 120 characters")
    private String name;

    @NotNull(message = "Stage type is required")
    private StageType stageType;

    private Role assignedRole;

    @NotNull(message = "Order index is required")
    private Integer orderIndex;
}
