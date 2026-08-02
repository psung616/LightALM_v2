package com.lightalm.service;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.ChangePasswordRequest;
import com.lightalm.dto.CreateUserRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.UpdateUserRequest;
import com.lightalm.dto.UserResponse;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.exception.ValidationException;
import com.lightalm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(getEntity(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("이미 사용 중인 username입니다: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("이미 사용 중인 email입니다: " + request.getEmail());
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .systemRole(request.getSystemRole() != null ? request.getSystemRole() : SystemRole.USER)
                .enabled(true)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getEntity(id);
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("이미 사용 중인 email입니다: " + request.getEmail());
        }
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        if (request.getSystemRole() != null) {
            user.setSystemRole(request.getSystemRole());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void deactivate(Long id) {
        User user = getEntity(id);
        user.setEnabled(false);
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = getEntity(id);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ValidationException("현재 비밀번호가 올바르지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + id));
    }
}
