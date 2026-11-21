package com.worknexa.request.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespondInfoRequestDto {

    @NotBlank(message = "Comment or explanation is required")
    private String comment;

    private Boolean documentAttached;

    private String documentName;
}
