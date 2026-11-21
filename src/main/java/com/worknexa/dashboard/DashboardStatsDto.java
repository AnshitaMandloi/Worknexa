package com.worknexa.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalRequests;
    private long autoApprovedCount;
    private long humanApprovedCount;
    private long pendingReviewCount;
    private long infoRequestedCount;
    private long rejectedCount;
    private long pendingTasksForRole;
    private long totalUsers;
    private double automationRatePercentage;
    private Map<String, Long> statusCounts;
}
