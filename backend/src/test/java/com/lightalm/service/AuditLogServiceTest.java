package com.lightalm.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.lightalm.domain.AuditLog;
import com.lightalm.domain.AuditTargetType;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.repository.AuditLogRepository;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private AuditLogService auditLogService;

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).username("member1").password("hash").email("member1@example.com")
                .fullName("Member One").systemRole(SystemRole.USER).enabled(true).build();
        principal = new UserPrincipal(user);
    }

    @Test
    void recordIfChanged_skipsWhenValuesAreEqual() {
        auditLogService.recordIfChanged(10L, AuditTargetType.REQUIREMENT, 5L, 1L, "title", "same", "same");

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void recordIfChanged_recordsWhenValuesDiffer() {
        auditLogService.recordIfChanged(10L, AuditTargetType.REQUIREMENT, 5L, 1L, "title", "old", "new");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void listForTarget_requiresViewerRole() {
        org.mockito.Mockito.when(auditLogRepository.findByProjectIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(10L, AuditTargetType.REQUIREMENT, 5L))
                .thenReturn(java.util.List.of());

        auditLogService.listForTarget(10L, AuditTargetType.REQUIREMENT, 5L, principal);

        verify(projectMemberService).requireRole(10L, principal, ProjectRole.VIEWER);
    }
}
