package com.lightalm;

import static org.assertj.core.api.Assertions.assertThat;

import com.lightalm.domain.Project;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.RequirementLevel;
import com.lightalm.domain.RequirementType;
import com.lightalm.domain.SystemRole;
import com.lightalm.domain.User;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.RequirementResponse;
import com.lightalm.repository.ProjectRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import com.lightalm.service.RequirementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * ADR-013: 요구사항 목록 조회에 추가된 선택적 쿼리 파라미터(requirementLevel, rootOnly)의
 * 동작과, 파라미터를 생략했을 때 기존과 완전히 동일하게 동작하는지(하위 호환 회귀)를 검증한다.
 * Testcontainers가 필요하므로 *IT.java로 명명되어 mvn verify(failsafe)에서만 실행된다.
 */
@Testcontainers
@SpringBootTest
class RequirementListFilterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RequirementRepository requirementRepository;
    @Autowired
    private RequirementService requirementService;

    private UserPrincipal adminPrincipal() {
        User admin = userRepository.save(User.builder()
                .username("req-filter-admin-" + System.nanoTime())
                .password("hash")
                .email("req-filter-admin-" + System.nanoTime() + "@example.com")
                .fullName("Req Filter Admin")
                .systemRole(SystemRole.ADMIN)
                .build());
        return new UserPrincipal(admin);
    }

    @Test
    void list_withoutNewParams_behavesExactlyAsBefore() {
        UserPrincipal principal = adminPrincipal();
        Project project = projectRepository.save(Project.builder()
                .projectKey("REQFLT1")
                .name("Requirement Filter Regression Project")
                .createdBy(userRepository.findById(principal.getId()).orElseThrow())
                .build());

        Requirement root = requirementRepository.save(Requirement.builder()
                .project(project).reqKey("REQFLT1-R1").title("루트 PRD").type(RequirementType.BUSINESS)
                .requirementLevel(RequirementLevel.PRD).build());
        requirementRepository.save(Requirement.builder()
                .project(project).reqKey("REQFLT1-R2").title("자식 SRS").type(RequirementType.FUNCTIONAL)
                .requirementLevel(RequirementLevel.SRS).parentRequirement(root).build());

        PageResponse<RequirementResponse> result = requirementService.list(
                project.getId(), null, null, null, null, null, null, null, null, principal, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void list_withRequirementLevelFilter_returnsOnlyMatchingLevel() {
        UserPrincipal principal = adminPrincipal();
        Project project = projectRepository.save(Project.builder()
                .projectKey("REQFLT2")
                .name("Requirement Level Filter Project")
                .createdBy(userRepository.findById(principal.getId()).orElseThrow())
                .build());

        requirementRepository.save(Requirement.builder()
                .project(project).reqKey("REQFLT2-R1").title("PRD 항목").type(RequirementType.BUSINESS)
                .requirementLevel(RequirementLevel.PRD).build());
        requirementRepository.save(Requirement.builder()
                .project(project).reqKey("REQFLT2-R2").title("SRS 항목").type(RequirementType.FUNCTIONAL)
                .requirementLevel(RequirementLevel.SRS).build());

        PageResponse<RequirementResponse> prdOnly = requirementService.list(
                project.getId(), null, null, null, null, null, null, RequirementLevel.PRD, null, principal, PageRequest.of(0, 20));

        assertThat(prdOnly.getContent()).hasSize(1);
        assertThat(prdOnly.getContent().get(0).getRequirementLevel()).isEqualTo(RequirementLevel.PRD);
    }

    @Test
    void list_withRootOnly_returnsOnlyRequirementsWithoutParent() {
        UserPrincipal principal = adminPrincipal();
        Project project = projectRepository.save(Project.builder()
                .projectKey("REQFLT3")
                .name("Requirement RootOnly Filter Project")
                .createdBy(userRepository.findById(principal.getId()).orElseThrow())
                .build());

        Requirement root = requirementRepository.save(Requirement.builder()
                .project(project).reqKey("REQFLT3-R1").title("루트").type(RequirementType.BUSINESS)
                .requirementLevel(RequirementLevel.PRD).build());
        requirementRepository.save(Requirement.builder()
                .project(project).reqKey("REQFLT3-R2").title("자식").type(RequirementType.FUNCTIONAL)
                .requirementLevel(RequirementLevel.SRS).parentRequirement(root).build());

        PageResponse<RequirementResponse> rootOnly = requirementService.list(
                project.getId(), null, null, null, null, null, null, null, true, principal, PageRequest.of(0, 20));

        assertThat(rootOnly.getContent()).hasSize(1);
        assertThat(rootOnly.getContent().get(0).getParentRequirementId()).isNull();
    }

    @Test
    void create_withoutRequirementLevel_defaultsToSrs() {
        UserPrincipal principal = adminPrincipal();
        Project project = projectRepository.save(Project.builder()
                .projectKey("REQFLT4")
                .name("Requirement Default Level Project")
                .createdBy(userRepository.findById(principal.getId()).orElseThrow())
                .build());

        com.lightalm.dto.CreateRequirementRequest request = new com.lightalm.dto.CreateRequirementRequest();
        request.setTitle("레벨 미지정 요구사항");
        request.setType(RequirementType.FUNCTIONAL);

        RequirementResponse created = requirementService.create(project.getId(), request, principal);

        assertThat(created.getRequirementLevel()).isEqualTo(RequirementLevel.SRS);
    }
}
