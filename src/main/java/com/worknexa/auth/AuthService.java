package com.worknexa.auth;

import com.worknexa.auth.dto.AuthResponseDto;
import com.worknexa.auth.dto.LoginRequestDto;
import com.worknexa.auth.dto.RegisterOrganizationRequestDto;
import com.worknexa.common.exception.BadRequestException;
import com.worknexa.common.exception.ResourceNotFoundException;
import com.worknexa.common.exception.UnauthorizedException;
import com.worknexa.common.security.JwtTokenProvider;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.common.security.UserPrincipal;
import com.worknexa.tenant.Tenant;
import com.worknexa.tenant.TenantRepository;
import com.worknexa.user.Role;
import com.worknexa.user.User;
import com.worknexa.user.UserRepository;
import com.worknexa.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponseDto registerOrganization(RegisterOrganizationRequestDto request) {
        if (tenantRepository.existsByNameIgnoreCase(request.getOrganizationName())) {
            throw new BadRequestException("Organization name '" + request.getOrganizationName() + "' is already registered");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getOrganizationName().trim())
                .build();
        tenant = tenantRepository.save(tenant);

        User adminUser = User.builder()
                .tenantId(tenant.getId())
                .name(request.getAdminName().trim())
                .email(request.getAdminEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .role(Role.ADMIN)
                .active(true)
                .build();
        adminUser = userRepository.save(adminUser);

        String token = jwtTokenProvider.generateToken(
                adminUser.getId(),
                tenant.getId(),
                adminUser.getEmail(),
                adminUser.getRole(),
                adminUser.getName()
        );

        return AuthResponseDto.builder()
                .token(token)
                .organizationName(tenant.getName())
                .user(UserResponseDto.fromEntity(adminUser))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = null;

        if (StringUtils.hasText(request.getOrganizationName())) {
            Tenant tenant = tenantRepository.findByNameIgnoreCase(request.getOrganizationName().trim())
                    .orElseThrow(() -> new BadRequestException("Organization not found: " + request.getOrganizationName()));
            user = userRepository.findByTenantIdAndEmailIgnoreCase(tenant.getId(), email)
                    .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        } else {
            List<User> matchingUsers = userRepository.findAllByEmailIgnoreCase(email);
            if (matchingUsers.isEmpty()) {
                throw new UnauthorizedException("Invalid email or password");
            }
            for (User u : matchingUsers) {
                if (passwordEncoder.matches(request.getPassword(), u.getPasswordHash())) {
                    user = u;
                    break;
                }
            }
            if (user == null) {
                throw new UnauthorizedException("Invalid email or password");
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is deactivated");
        }

        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                tenant.getId(),
                user.getEmail(),
                user.getRole(),
                user.getName()
        );

        return AuthResponseDto.builder()
                .token(token)
                .organizationName(tenant.getName())
                .user(UserResponseDto.fromEntity(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User user = userRepository.findByIdAndTenantId(principal.getId(), principal.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponseDto.fromEntity(user);
    }
}
