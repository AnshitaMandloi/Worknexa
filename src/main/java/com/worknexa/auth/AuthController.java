package com.worknexa.auth;

import com.worknexa.auth.dto.AuthResponseDto;
import com.worknexa.auth.dto.LoginRequestDto;
import com.worknexa.auth.dto.RegisterOrganizationRequestDto;
import com.worknexa.common.dto.ApiResponse;
import com.worknexa.user.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Public organization registration, login and profile APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-organization")
    @Operation(summary = "Register a new organization and root admin user")
    public ResponseEntity<ApiResponse<AuthResponseDto>> registerOrganization(
            @Valid @RequestBody RegisterOrganizationRequestDto request) {
        AuthResponseDto response = authService.registerOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Organization registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT token")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Logged in successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser() {
        UserResponseDto user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
