package com.worknexa.user;

import com.worknexa.common.dto.ApiResponse;
import com.worknexa.common.dto.PagedResponse;
import com.worknexa.user.dto.CreateUserRequestDto;
import com.worknexa.user.dto.UpdateUserRequestDto;
import com.worknexa.user.dto.UserResponseDto;
import com.worknexa.user.dto.UserStatusUpdateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Admin user provisioning and lifecycle APIs")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new user within current tenant")
    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(
            @Valid @RequestBody CreateUserRequestDto request) {
        UserResponseDto user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", user));
    }

    @GetMapping
    @Operation(summary = "List users for current tenant with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> getUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(userService.getUsers(pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user name or role")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        UserResponseDto updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate user account")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateDto request) {
        UserResponseDto updated = userService.updateUserStatus(id, request.getActive());
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", updated));
    }
}
