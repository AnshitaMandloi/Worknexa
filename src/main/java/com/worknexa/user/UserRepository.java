package com.worknexa.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTenantIdAndEmailIgnoreCase(Long tenantId, String email);

    List<User> findAllByEmailIgnoreCase(String email);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    Page<User> findAllByTenantId(Long tenantId, Pageable pageable);

    boolean existsByTenantIdAndEmailIgnoreCase(Long tenantId, String email);

    List<User> findAllByTenantIdAndRoleAndActiveTrue(Long tenantId, Role role);

    long countByTenantId(Long tenantId);
}
