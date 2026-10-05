package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateUserRequest;
import com.lightalm.dto.UpdateUserRequest;
import com.lightalm.dto.UserResponse;
import com.lightalm.repository.UserRepository;
import com.lightalm.user.domain.LastActiveAdminRemovalException;
import com.lightalm.user.service.SystemAdminRetentionPolicy;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ADR-015. 마지막 활성 System Admin 보호 연결(§3.1)과 계정 생성 시 라이센스 게이트 제거(D2) 검증.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SystemAdminRetentionPolicy systemAdminRetentionPolicy;

    @InjectMocks
    private UserService userService;

    private static User user(long id, SystemRole role, boolean enabled) {
        return User.builder().id(id).username("u" + id).password("hash").email("u" + id + "@example.com")
                .fullName("User " + id).systemRole(role).enabled(enabled).build();
    }

    private static UpdateUserRequest updateRequest(User target, SystemRole systemRole, Boolean enabled) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail(target.getEmail());
        request.setFullName("Renamed");
        request.setSystemRole(systemRole);
        request.setEnabled(enabled);
        return request;
    }

    @Test
    void update_revokingAdmin_checksPolicyBeforeLoadingAndChangingTarget() {
        User target = user(1L, SystemRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        UserResponse response = userService.update(1L, updateRequest(target, SystemRole.USER, null));

        InOrder order = inOrder(systemAdminRetentionPolicy, userRepository);
        order.verify(systemAdminRetentionPolicy).requireAnotherActiveAdmin(1L);
        order.verify(userRepository).findById(1L);
        assertThat(response.getSystemRole()).isEqualTo(SystemRole.USER);
    }

    @Test
    void update_disabling_checksPolicy() {
        User target = user(1L, SystemRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        userService.update(1L, updateRequest(target, null, false));

        verify(systemAdminRetentionPolicy).requireAnotherActiveAdmin(1L);
        assertThat(target.getEnabled()).isFalse();
    }

    @Test
    void update_whenPolicyRejects_leavesTargetUnchanged() {
        User target = user(1L, SystemRole.ADMIN, true);
        doThrow(new LastActiveAdminRemovalException()).when(systemAdminRetentionPolicy).requireAnotherActiveAdmin(1L);

        assertThatThrownBy(() -> userService.update(1L, updateRequest(target, SystemRole.USER, false)))
                .isInstanceOf(LastActiveAdminRemovalException.class);

        verify(userRepository, never()).findById(any());
        assertThat(target.getSystemRole()).isEqualTo(SystemRole.ADMIN);
        assertThat(target.getEnabled()).isTrue();
        assertThat(target.getFullName()).isEqualTo("User 1");
    }

    @Test
    void update_onlyEmailAndFullName_skipsPolicy() {
        User target = user(1L, SystemRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        userService.update(1L, updateRequest(target, null, null));

        verify(systemAdminRetentionPolicy, never()).requireAnotherActiveAdmin(any());
        assertThat(target.getFullName()).isEqualTo("Renamed");
    }

    @Test
    void update_keepingAdminAndEnabledTrue_skipsPolicy() {
        User target = user(1L, SystemRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        userService.update(1L, updateRequest(target, SystemRole.ADMIN, true));

        verify(systemAdminRetentionPolicy, never()).requireAnotherActiveAdmin(any());
    }

    @Test
    void deactivate_alwaysChecksPolicyBeforeChange() {
        User target = user(3L, SystemRole.USER, true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));

        userService.deactivate(3L);

        verify(systemAdminRetentionPolicy).requireAnotherActiveAdmin(3L);
        assertThat(target.getEnabled()).isFalse();
    }

    @Test
    void deactivate_whenPolicyRejects_doesNotDisable() {
        User target = user(1L, SystemRole.ADMIN, true);
        doThrow(new LastActiveAdminRemovalException()).when(systemAdminRetentionPolicy).requireAnotherActiveAdmin(1L);

        assertThatThrownBy(() -> userService.deactivate(1L)).isInstanceOf(LastActiveAdminRemovalException.class);

        assertThat(target.getEnabled()).isTrue();
    }

    @Test
    void create_hasNoSeatOrLicenseGate_andDefaultsToUserRole() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newbie");
        request.setPassword("password1");
        request.setEmail("newbie@example.com");
        request.setFullName("New Bie");
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(userRepository.existsByEmail("newbie@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(request);

        assertThat(response.getUsername()).isEqualTo("newbie");
        assertThat(response.getSystemRole()).isEqualTo(SystemRole.USER);
        verify(systemAdminRetentionPolicy, never()).requireAnotherActiveAdmin(any());
    }
}
