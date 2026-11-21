package com.worknexa.common.security;

import com.worknexa.common.exception.UnauthorizedException;
import com.worknexa.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        throw new UnauthorizedException("User is not authenticated");
    }

    public static Long getCurrentTenantId() {
        return getCurrentUser().getTenantId();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static Role getCurrentUserRole() {
        return getCurrentUser().getRole();
    }
}
