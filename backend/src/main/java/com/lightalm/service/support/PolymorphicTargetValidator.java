package com.lightalm.service.support;

import com.lightalm.domain.TargetType;
import com.lightalm.exception.ResourceNotFoundException;
import com.lightalm.repository.IssueRepository;
import com.lightalm.repository.RequirementRepository;
import com.lightalm.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolymorphicTargetValidator {

    private final RequirementRepository requirementRepository;
    private final IssueRepository issueRepository;
    private final TestCaseRepository testCaseRepository;

    /**
     * (targetType, targetId)로 식별되는 대상이 존재하고, projectId가 가리키는 프로젝트에
     * 속하는지 검증한다. 존재하지 않거나 다른 프로젝트 소속이면 ResourceNotFoundException(404).
     * 검증된 엔티티는 리턴하지 않는다(모든 호출부가 존재 확인 용도로만 쓰기 때문).
     */
    public void ensureExists(Long projectId, TargetType targetType, Long targetId) {
        switch (targetType) {
            case REQUIREMENT -> {
                var requirement = requirementRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId));
                if (!requirement.getProject().getId().equals(projectId)) {
                    throw new ResourceNotFoundException("요구사항을 찾을 수 없습니다: " + targetId);
                }
            }
            case ISSUE -> {
                var issue = issueRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId));
                if (!issue.getProject().getId().equals(projectId)) {
                    throw new ResourceNotFoundException("이슈를 찾을 수 없습니다: " + targetId);
                }
            }
            case TEST_CASE -> {
                var testCase = testCaseRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("테스트케이스를 찾을 수 없습니다: " + targetId));
                if (!testCase.getProject().getId().equals(projectId)) {
                    throw new ResourceNotFoundException("테스트케이스를 찾을 수 없습니다: " + targetId);
                }
            }
            default -> throw new IllegalStateException("지원하지 않는 TargetType입니다: " + targetType);
        }
    }
}
