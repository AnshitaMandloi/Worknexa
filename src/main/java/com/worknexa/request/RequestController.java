package com.worknexa.request;

import com.worknexa.common.dto.ApiResponse;
import com.worknexa.common.dto.PagedResponse;
import com.worknexa.request.dto.CreateRequestDto;
import com.worknexa.request.dto.RequestDetailDto;
import com.worknexa.request.dto.RequestResponseDto;
import com.worknexa.request.dto.RespondInfoRequestDto;
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
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Workflow Requests", description = "Request submission, lifecycle tracking and info response APIs")
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    @Operation(summary = "Submit a new workflow request (triggers AI risk assessment and rule engine)")
    public ResponseEntity<ApiResponse<RequestResponseDto>> submitRequest(
            @Valid @RequestBody CreateRequestDto request) {
        RequestResponseDto created = requestService.submitRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Request submitted successfully", created));
    }

    @GetMapping("/my")
    @Operation(summary = "Get requests submitted by current user with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<RequestResponseDto>>> getMyRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(requestService.getMyRequests(pageable))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'COMPLIANCE')")
    @Operation(summary = "Get all requests in current tenant (Privileged roles only)")
    public ResponseEntity<ApiResponse<PagedResponse<RequestResponseDto>>> getAllRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(requestService.getAllRequests(pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full request details including AI analysis, approval tasks and audit timeline")
    public ResponseEntity<ApiResponse<RequestDetailDto>> getRequestDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(requestService.getRequestDetail(id)));
    }

    @PostMapping("/{id}/respond-info")
    @Operation(summary = "Respond to an information request from a reviewer to resume the workflow")
    public ResponseEntity<ApiResponse<RequestResponseDto>> respondToInfo(
            @PathVariable Long id,
            @Valid @RequestBody RespondInfoRequestDto request) {
        RequestResponseDto updated = requestService.respondToInfo(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Information submitted and workflow resumed", updated));
    }
}
