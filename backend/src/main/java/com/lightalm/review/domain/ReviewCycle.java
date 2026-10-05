package com.lightalm.review.domain;

import com.lightalm.domain.Project;
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
 * ADR-008(Phase 16) / 03-data-model.md §3.18. 여러 검토자가 각자 승인/반려/의견을
 * 기록하는 "의견 수집" 사이클. 기존 승인 워크플로우({@code ApprovalRequest}, Phase 15)와는
 * 완전히 별개 기능이며, 이 사이클의 종료(close)는 대상(요구사항/이슈)의 status를 바꾸지 않는다
 * (03-data-model.md §3.19 원칙, 01-scope.md §1.3).
 */
@Entity
@Table(name = "review_cycles")
@Getter
public class ReviewCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewCycleStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    protected ReviewCycle() {
        // JPA
    }

    @Builder
    private ReviewCycle(Project project, TargetType targetType, Long targetId, String name,
                         ReviewCycleStatus status, User createdBy) {
        this.project = project;
        this.targetType = targetType;
        this.targetId = targetId;
        this.name = name;
        this.status = status != null ? status : ReviewCycleStatus.OPEN;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** 사이클을 닫는다. 대상(요구사항/이슈)의 status는 전혀 건드리지 않는다(§3.19 원칙). */
    public void close() {
        this.status = ReviewCycleStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return this.status == ReviewCycleStatus.OPEN;
    }
}
