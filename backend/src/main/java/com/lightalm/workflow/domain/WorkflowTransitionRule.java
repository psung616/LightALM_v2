package com.lightalm.workflow.domain;

import com.lightalm.domain.Project;
import com.lightalm.domain.ProjectRole;
import com.lightalm.domain.TargetType;
import com.lightalm.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * ADR-012 §D.2. 프로젝트별 상태 전이 화이트리스트 한 줄(from_status -> to_status, 최소 역할).
 *
 * <p><b>범용 워크플로우 엔진이 아니다.</b> 조건 분기, 전이 시 자동 액션, 승인자 체인은 표현하지
 * 않는다 — "이 상태에서 저 상태로 전이할 수 있는가(참/거짓) + 최소 역할"만 담는다. 대상은
 * {@code REQUIREMENT}/{@code ISSUE} 두 target_type만 지원한다(서비스 레이어가 강제, §D.1).</p>
 *
 * <p>생성/삭제만 가능하고 수정 API는 없으므로(§D.4) 도메인 상태 전이 메서드를 두지 않았다 —
 * {@code @Setter}도 두지 않는다.</p>
 */
@Entity
@Table(name = "workflow_transition_rules")
@Getter
public class WorkflowTransitionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "from_status", nullable = false, length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowed_role", length = 20)
    private ProjectRole allowedRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected WorkflowTransitionRule() {
        // JPA
    }

    @Builder
    private WorkflowTransitionRule(Project project, TargetType targetType, String fromStatus, String toStatus,
                                    ProjectRole allowedRole, User createdBy) {
        this.project = project;
        this.targetType = targetType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.allowedRole = allowedRole;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
