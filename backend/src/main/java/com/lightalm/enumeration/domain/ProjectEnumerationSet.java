package com.lightalm.enumeration.domain;

import com.lightalm.domain.Project;
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
 * ADR-012 §C.2. 프로젝트별 열거형 집합(Enumeration Set) — PRIORITY 확장 또는 커스텀 필드 전용
 * SELECT 선택지 목록의 컨테이너. 생성 후 {@code enumKey}/{@code baseEnum}은 변경하지 않는다
 * (API에 수정 엔드포인트가 없다 — §C.4).
 */
@Entity
@Table(name = "project_enumeration_sets")
@Getter
public class ProjectEnumerationSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "enum_key", nullable = false, length = 50)
    private String enumKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_enum", length = 30)
    private BaseEnumType baseEnum;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProjectEnumerationSet() {
        // JPA
    }

    @Builder
    private ProjectEnumerationSet(Project project, String enumKey, BaseEnumType baseEnum, String name) {
        this.project = project;
        this.enumKey = enumKey;
        this.baseEnum = baseEnum;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
