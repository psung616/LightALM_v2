package com.lightalm.baseline.domain;

import com.lightalm.domain.Project;
import com.lightalm.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * ADR-008(Phase 16) / 03-data-model.md §3.20. 특정 시점에 대상(요구사항/이슈/테스트케이스)
 * 집합의 상태를 고정해두는 그릇. 실제 스냅샷 값은 {@link BaselineItem#getSnapshot()}에 저장된다.
 */
@Entity
@Table(name = "baselines")
@Getter
public class Baseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Baseline() {
        // JPA
    }

    @Builder
    private Baseline(Project project, String name, String description, User createdBy) {
        this.project = project;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
