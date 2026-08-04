package com.lightalm.service;

import com.lightalm.domain.Priority;
import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.TestCase;
import com.lightalm.domain.TestCaseStatus;
import com.lightalm.domain.User;
import com.lightalm.dto.CreateTestCaseRequest;
import com.lightalm.dto.PageResponse;
import com.lightalm.dto.TestCaseResponse;
import com.lightalm.dto.UpdateTestCaseRequest;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TestCaseRepository;
import com.lightalm.repository.UserRepository;
import com.lightalm.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final RequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @Transactional(readOnly = true)
    public PageResponse<TestCaseResponse> list(Long projectId, Long requirementId, TestCaseStatus status, Priority priority,
                                                 String keyword, UserPrincipal principal, Pageable pageable) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Specification<TestCase> spec = (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
        if (requirementId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("requirement").get("id"), requirementId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.like(cb.lower(root.get("title")), like));
                predicates.add(cb.like(cb.lower(root.get("tcKey")), like));
                return cb.or(predicates.toArray(new Predicate[0]));
            });
        }
        return PageResponse.from(testCaseRepository.findAll(spec, pageable).map(TestCaseResponse::from));
    }

    @Transactional(readOnly = true)
    public TestCaseResponse get(Long projectId, Long tcId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        return TestCaseResponse.from(getEntity(projectId, tcId));
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> listByRequirement(Long projectId, Long reqId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.VIEWER);
        Requirement requirement = requirementRepository.findById(reqId)
                .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId));
        if (!requirement.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + reqId);
        }
        return testCaseRepository.findByRequirementId(reqId).stream()
                .filter(tc -> tc.getProject().getId().equals(projectId))
                .map(TestCaseResponse::from)
                .toList();
    }

    @Transactional
    public TestCaseResponse create(Long projectId, CreateTestCaseRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        Project project = projectService.getEntity(projectId);
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + principal.getId()));
        Requirement requirement = resolveRequirement(projectId, request.getRequirementId());

        String tcKey = projectService.nextTestCaseKey(projectId);
        TestCase testCase = TestCase.builder()
                .project(project)
                .requirement(requirement)
                .tcKey(tcKey)
                .title(request.getTitle())
                .description(request.getDescription())
                .preconditions(request.getPreconditions())
                .steps(request.getSteps())
                .expectedResult(request.getExpectedResult())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .createdBy(creator)
                .build();
        return TestCaseResponse.from(testCaseRepository.save(testCase));
    }

    @Transactional
    public TestCaseResponse update(Long projectId, Long tcId, UpdateTestCaseRequest request, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.MEMBER);
        TestCase testCase = getEntity(projectId, tcId);
        Requirement requirement = resolveRequirement(projectId, request.getRequirementId());

        testCase.setTitle(request.getTitle());
        testCase.setDescription(request.getDescription());
        testCase.setPreconditions(request.getPreconditions());
        testCase.setSteps(request.getSteps());
        testCase.setExpectedResult(request.getExpectedResult());
        testCase.setPriority(request.getPriority() != null ? request.getPriority() : testCase.getPriority());
        testCase.setRequirement(requirement);
        if (request.getStatus() != null) {
            testCase.setStatus(request.getStatus());
        }
        return TestCaseResponse.from(testCase);
    }

    @Transactional
    public void delete(Long projectId, Long tcId, UserPrincipal principal) {
        projectMemberService.requireRole(projectId, principal, ProjectRole.PROJECT_ADMIN);
        TestCase testCase = getEntity(projectId, tcId);
        testCaseRepository.delete(testCase);
    }

    private Requirement resolveRequirement(Long projectId, Long requirementId) {
        if (requirementId == null) {
            return null;
        }
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + requirementId));
        if (!requirement.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + requirementId);
        }
        return requirement;
    }

    TestCase getEntity(Long projectId, Long tcId) {
        TestCase testCase = testCaseRepository.findById(tcId)
                .orElseThrow(() -> new ResourceNotFoundException("테스트케이스를 찾을 수 없습니다: " + tcId));
        if (!testCase.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("테스트케이스를 찾을 수 없습니다: " + tcId);
        }
        return testCase;
    }
}
