package com.lightalm.auth.service;

import com.lightalm.auth.dto.SignupRequest;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.UserResponse;
import com.lightalm.exception.ValidationException;
import com.lightalm.license.service.LicenseEnforcementService;
import com.lightalm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-011 §1. systemRole은 항상 USER, enabled는 항상 true로 고정한다.
 * 가입 성공해도 자동 로그인하지 않는다(컨트롤러/프론트가 /login으로 안내).
 */
@Service
@RequiredArgsConstructor
public class SelfSignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LicenseEnforcementService licenseEnforcementService;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new ValidationException("password와 passwordConfirm이 일치하지 않습니다.");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ValidationException("이미 사용 중인 username입니다: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("이미 사용 중인 email입니다: " + request.email());
        }

        licenseEnforcementService.requireActiveLicense();
        licenseEnforcementService.requireSeatAvailable();

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .fullName(request.fullName())
                .systemRole(SystemRole.USER)
                .enabled(true)
                .build();
        return UserResponse.from(userRepository.save(user));
    }
}
