package com.lightalm.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lightalm.auth.dto.SignupRequest;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.UserResponse;
import com.lightalm.exception.ValidationException;
import com.lightalm.license.service.LicenseEnforcementService;
import com.lightalm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ADR-011 §1.2: systemRole 고정, password/passwordConfirm 불일치 거부에 대한 회귀 테스트.
 */
@ExtendWith(MockitoExtension.class)
class SelfSignupServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LicenseEnforcementService licenseEnforcementService;

    @InjectMocks
    private SelfSignupService selfSignupService;

    @Test
    void signup_throwsValidationException_whenPasswordConfirmDoesNotMatch() {
        SignupRequest request = new SignupRequest("jdoe", "jdoe@example.com", "John Doe", "P@ssw0rd1", "different");

        assertThatThrownBy(() -> selfSignupService.signup(request))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(licenseEnforcementService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void signup_persistsUserWithFixedUserRoleAndEnabledTrue_regardlessOfRequestPayload() {
        SignupRequest request = new SignupRequest("jdoe", "jdoe@example.com", "John Doe", "P@ssw0rd1", "P@ssw0rd1");
        when(userRepository.existsByUsername("jdoe")).thenReturn(false);
        when(userRepository.existsByEmail("jdoe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("P@ssw0rd1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = selfSignupService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getSystemRole()).isEqualTo(SystemRole.USER);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getUsername()).isEqualTo("jdoe");
        assertThat(response.getSystemRole()).isEqualTo(SystemRole.USER);
    }

    @Test
    void signup_throwsValidationException_whenUsernameAlreadyExists() {
        SignupRequest request = new SignupRequest("jdoe", "jdoe@example.com", "John Doe", "P@ssw0rd1", "P@ssw0rd1");
        when(userRepository.existsByUsername("jdoe")).thenReturn(true);

        assertThatThrownBy(() -> selfSignupService.signup(request))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(licenseEnforcementService);
    }
}
