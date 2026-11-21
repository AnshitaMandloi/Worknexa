package com.worknexa.auth.dto;

import jakarta.validation.constraints.Email;
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
public class RegisterOrganizationRequestDto {

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 120, message = "Organization name must be between 2 and 120 characters")
    private String organizationName;

    @NotBlank(message = "Admin name is required")
    @Size(min = 2, max = 120, message = "Admin name must be between 2 and 120 characters")
    private String adminName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be a valid email address")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String adminPassword;
}
