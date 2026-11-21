package com.worknexa.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId);

    Page<Notification> findAllByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId, Pageable pageable);

    Optional<Notification> findByIdAndTenantId(Long id, Long tenantId);

    long countByTenantIdAndUserIdAndReadFalse(Long tenantId, Long userId);
}
