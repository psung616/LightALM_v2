package com.lightalm.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import com.lightalm.domain.LinkType;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateTraceabilityLinkRequest;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TraceabilityLinkRepository;
import com.lightalm.repository.TraceabilityTreeRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.support.PolymorphicTargetValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-010: TraceabilityService.createLink()가 다른 프로젝트 소속 대상을 다룰 때
 * 기존 ValidationException(400) 대신 ResourceNotFoundException(404)을 반환하도록
 * PolymorphicTargetValidator에 위임하는 변경의 회귀 테스트.
 */
@ExtendWith(MockitoExtension.class)
class TraceabilityServiceTest {

    @Mock
    private TraceabilityLinkRepository traceabilityLinkRepository;
    @Mock
    private TraceabilityTreeRepository traceabilityTreeRepository;
    @Mock
    private RequirementRepository requirementRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private PolymorphicTargetValidator polymorphicTargetValidator;

    @InjectMocks
    private TraceabilityService traceabilityService;

    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .username("member1")
                .password("hash")
                .email("member1@example.com")
                .fullName("Member One")
                .systemRole(SystemRole.USER)
                .enabled(true)
                .build();
        principal = new UserPrincipal(user);
    }

    @Test
    void createLink_whenSourceBelongsToOtherProject_propagatesResourceNotFound() {
        CreateTraceabilityLinkRequest request = new CreateTraceabilityLinkRequest();
        request.setSourceType(TargetType.REQUIREMENT);
        request.setSourceId(1L);
        request.setTargetType(TargetType.ISSUE);
        request.setTargetId(2L);
        request.setLinkType(LinkType.IMPLEMENTS);

        doThrow(new ResourceNotFoundException("요구사항을 찾을 수 없습니다: 1"))
                .when(polymorphicTargetValidator).ensureExists(10L, TargetType.REQUIREMENT, 1L);

        assertThatThrownBy(() -> traceabilityService.createLink(10L, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
