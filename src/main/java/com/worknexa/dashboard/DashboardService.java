package com.worknexa.dashboard;

import com.worknexa.approval.ApprovalTaskRepository;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.request.RequestStatus;
import com.worknexa.request.WorkflowRequestRepository;
import com.worknexa.user.Role;
import com.worknexa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkflowRequestRepository requestRepository;
    private final ApprovalTaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Role userRole = SecurityUtils.getCurrentUserRole();

        long total = requestRepository.countByTenantId(tenantId);
        long autoApproved = requestRepository.countByTenantIdAndStatus(tenantId, RequestStatus.AUTO_APPROVED);
        long humanApproved = requestRepository.countByTenantIdAndStatus(tenantId, RequestStatus.APPROVED);
        long pendingReview = requestRepository.countByTenantIdAndStatus(tenantId, RequestStatus.PENDING_REVIEW);
        long infoRequested = requestRepository.countByTenantIdAndStatus(tenantId, RequestStatus.INFO_REQUESTED);
        long rejected = requestRepository.countByTenantIdAndStatus(tenantId, RequestStatus.REJECTED);

        long myPendingTasks = 0;
        if (userRole != Role.EMPLOYEE) {
            myPendingTasks = taskRepository.findActionableTasksForRole(tenantId, userRole).size();
        }

        long totalUsers = userRepository.countByTenantId(tenantId);

        long totalApproved = autoApproved + humanApproved;
        double automationRate = totalApproved > 0
                ? Math.round(((double) autoApproved / totalApproved) * 1000.0) / 10.0
                : 0.0;

        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("AUTO_APPROVED", autoApproved);
        statusCounts.put("APPROVED", humanApproved);
        statusCounts.put("PENDING_REVIEW", pendingReview);
        statusCounts.put("INFO_REQUESTED", infoRequested);
        statusCounts.put("REJECTED", rejected);

        return DashboardStatsDto.builder()
                .totalRequests(total)
                .autoApprovedCount(autoApproved)
                .humanApprovedCount(humanApproved)
                .pendingReviewCount(pendingReview)
                .infoRequestedCount(infoRequested)
                .rejectedCount(rejected)
                .pendingTasksForRole(myPendingTasks)
                .totalUsers(totalUsers)
                .automationRatePercentage(automationRate)
                .statusCounts(statusCounts)
                .build();
    }
}
