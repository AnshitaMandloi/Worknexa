package com.worknexa.request.dto;

import com.worknexa.ai.dto.AiAnalysisDto;
import com.worknexa.approval.dto.ApprovalTaskDto;
import com.worknexa.audit.dto.AuditLogDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDetailDto {
    private RequestResponseDto request;
    private AiAnalysisDto aiAnalysis;
    @Builder.Default
    private List<ApprovalTaskDto> tasks = new ArrayList<>();
    @Builder.Default
    private List<AuditLogDto> timeline = new ArrayList<>();
}
