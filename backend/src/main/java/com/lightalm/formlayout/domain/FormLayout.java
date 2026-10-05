package com.lightalm.formlayout.domain;

import com.lightalm.domain.Project;
import com.lightalm.domain.TargetType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * ADR-012 §B.2. 프로젝트+target_type당 정확히 1개(UNIQUE(project_id,target_type)). 이 Phase는
 * "전체 치환" 저장만 지원하므로({@link com.lightalm.formlayout.service.FormLayoutService}),
 * 기존 레이아웃이 있으면 삭제 후 전체를 다시 생성하는 방식으로 쓰기를 처리한다 — 그래서
 * 부분 갱신용 메서드(섹션 추가/삭제 등)는 두지 않고, {@link #addSection(FormLayoutSection)}로
 * 생성 시점에만 트리를 조립한다.
 */
@Entity
@Table(name = "form_layouts")
@Getter
public class FormLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @OneToMany(mappedBy = "formLayout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private final List<FormLayoutSection> sections = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected FormLayout() {
        // JPA
    }

    @Builder
    private FormLayout(Project project, TargetType targetType) {
        this.project = project;
        this.targetType = targetType;
    }

    public void addSection(FormLayoutSection section) {
        section.assignFormLayout(this);
        this.sections.add(section);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
