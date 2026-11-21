package com.worknexa.user;

import com.worknexa.common.exception.BadRequestException;
import com.worknexa.common.exception.ResourceNotFoundException;
import com.worknexa.common.security.SecurityUtils;
import com.worknexa.user.dto.CreateUserRequestDto;
import com.worknexa.user.dto.UpdateUserRequestDto;
import com.worknexa.user.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto createUser(CreateUserRequestDto request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByTenantIdAndEmailIgnoreCase(tenantId, email)) {
            throw new BadRequestException("User with email '" + email + "' already exists in this organization");
        }

        User user = User.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        user = userRepository.save(user);
        return UserResponseDto.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsers(Pageable pageable) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return userRepository.findAllByTenantId(tenantId, pageable)
                .map(UserResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponseDto.fromEntity(user);
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UpdateUserRequestDto request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setName(request.getName().trim());
        user.setRole(request.getRole());
        user = userRepository.save(user);

        return UserResponseDto.fromEntity(user);
    }

    @Transactional
    public UserResponseDto updateUserStatus(Long id, boolean active) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setActive(active);
        user = userRepository.save(user);

        return UserResponseDto.fromEntity(user);
    }
}
