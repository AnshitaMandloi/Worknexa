package com.worknexa.rule;

import com.worknexa.common.dto.ApiResponse;
import com.worknexa.rule.dto.BusinessRuleDto;
import com.worknexa.rule.dto.CreateRuleRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows/{workflowId}/rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Business Rules", description = "Business rule configuration APIs for workflows")
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    @Operation(summary = "Get all business rules for a workflow")
    public ResponseEntity<ApiResponse<List<BusinessRuleDto>>> getRules(@PathVariable Long workflowId) {
        return ResponseEntity.ok(ApiResponse.ok(ruleService.getRulesByWorkflow(workflowId)));
    }

    @PostMapping
    @Operation(summary = "Create a business rule for a workflow")
    public ResponseEntity<ApiResponse<BusinessRuleDto>> createRule(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateRuleRequestDto request) {
        BusinessRuleDto created = ruleService.createRule(workflowId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rule created successfully", created));
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "Update a business rule")
    public ResponseEntity<ApiResponse<BusinessRuleDto>> updateRule(
            @PathVariable Long workflowId,
            @PathVariable Long ruleId,
            @Valid @RequestBody CreateRuleRequestDto request) {
        BusinessRuleDto updated = ruleService.updateRule(workflowId, ruleId, request);
        return ResponseEntity.ok(ApiResponse.ok("Rule updated successfully", updated));
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Delete a business rule")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable Long workflowId,
            @PathVariable Long ruleId) {
        ruleService.deleteRule(workflowId, ruleId);
        return ResponseEntity.ok(ApiResponse.ok("Rule deleted successfully", null));
    }
}
