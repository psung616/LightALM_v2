package com.lightalm.service.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lightalm.domain.Issue;
import com.lightalm.domain.Project;
import com.lightalm.domain.Requirement;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.TestCase;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TestCaseRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolymorphicTargetValidatorTest {

    @Mock
    private RequirementRepository requirementRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private PolymorphicTargetValidator validator;

    private Project projectA;
    private Project projectB;

    @BeforeEach
    void setUp() {
        projectA = Project.builder().id(10L).projectKey("LALM").name("Light ALM").build();
        projectB = Project.builder().id(20L).projectKey("OTHER").name("Other Project").build();
    }

    // ---- REQUIREMENT ----

    @Test
    void ensureExists_requirement_existsInSameProject_doesNotThrow() {
        Requirement requirement = Requirement.builder().id(1L).project(projectA).build();
        when(requirementRepository.findById(1L)).thenReturn(Optional.of(requirement));

        assertThatCode(() -> validator.ensureExists(10L, TargetType.REQUIREMENT, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureExists_requirement_notFound_throwsResourceNotFound() {
        when(requirementRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.ensureExists(10L, TargetType.REQUIREMENT, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ensureExists_requirement_belongsToOtherProject_throwsResourceNotFound() {
        Requirement requirement = Requirement.builder().id(1L).project(projectB).build();
        when(requirementRepository.findById(1L)).thenReturn(Optional.of(requirement));

        assertThatThrownBy(() -> validator.ensureExists(10L, TargetType.REQUIREMENT, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- ISSUE ----

    @Test
    void ensureExists_issue_existsInSameProject_doesNotThrow() {
        Issue issue = Issue.builder().id(2L).project(projectA).build();
        when(issueRepository.findById(2L)).thenReturn(Optional.of(issue));

        assertThatCode(() -> validator.ensureExists(10L, TargetType.ISSUE, 2L))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureExists_issue_notFound_throwsResourceNotFound() {
        when(issueRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.ensureExists(10L, TargetType.ISSUE, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ensureExists_issue_belongsToOtherProject_throwsResourceNotFound() {
        Issue issue = Issue.builder().id(2L).project(projectB).build();
        when(issueRepository.findById(2L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> validator.ensureExists(10L, TargetType.ISSUE, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- TEST_CASE ----

    @Test
    void ensureExists_testCase_existsInSameProject_doesNotThrow() {
        TestCase testCase = TestCase.builder().id(3L).project(projectA).build();
        when(testCaseRepository.findById(3L)).thenReturn(Optional.of(testCase));

        assertThatCode(() -> validator.ensureExists(10L, TargetType.TEST_CASE, 3L))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureExists_testCase_notFound_throwsResourceNotFound() {
        when(testCaseRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.ensureExists(10L, TargetType.TEST_CASE, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ensureExists_testCase_belongsToOtherProject_throwsResourceNotFound() {
        TestCase testCase = TestCase.builder().id(3L).project(projectB).build();
        when(testCaseRepository.findById(3L)).thenReturn(Optional.of(testCase));

        assertThatThrownBy(() -> validator.ensureExists(10L, TargetType.TEST_CASE, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
