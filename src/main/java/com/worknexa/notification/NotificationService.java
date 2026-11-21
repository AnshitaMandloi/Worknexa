package com.worknexa.notification;

import com.worknexa.common.exception.ResourceNotFoundException;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.user.Role;
import com.worknexa.user.User;
import com.worknexa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(Long tenantId, Long userId, String message) {
        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .userId(userId)
                .message(message)
                .read(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void notifyRole(Long tenantId, Role role, String message) {
        List<User> users = userRepository.findAllByTenantIdAndRoleAndActiveTrue(tenantId, role);
        for (User u : users) {
            createNotification(tenantId, u.getId(), message);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.findAllByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId);
    }

    @Transactional
    public Notification markAsRead(Long id) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Notification notification = notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }
}
