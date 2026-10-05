package com.lightalm.enumeration.domain;

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
 * ADR-012 §C.2. 열거형 집합에 속한 개별 값. {@code isSystemDefault=true}는 기존 고정 Java enum
 * 값을 그대로 미러링한 행으로 삭제(소프트) 금지 — 라벨/순서만 수정 가능하다. 삭제 거부 자체는
 * 서비스 레이어({@code EnumerationSetService})가 호출 전에 검사한다(소프트삭제 가능 여부는
 * 도메인 메서드 호출 전 책임이 서비스에 있다는 점에서 {@code CustomFieldDefinition} 패턴과 다르게,
 * 이 엔티티는 상태 변경 메서드만 제공하고 거부 판단은 서비스가 한다).
 */
@Entity
@Table(name = "project_enumeration_values")
@Getter
public class ProjectEnumerationValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enumeration_set_id", nullable = false)
    private ProjectEnumerationSet enumerationSet;

    @Column(name = "value_key", nullable = false, length = 50)
    private String valueKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_system_default", nullable = false)
    private Boolean isSystemDefault;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumerationValueStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProjectEnumerationValue() {
        // JPA
    }

    @Builder
    private ProjectEnumerationValue(ProjectEnumerationSet enumerationSet, String valueKey, String label,
                                     Integer displayOrder, Boolean isSystemDefault) {
        this.enumerationSet = enumerationSet;
        this.valueKey = valueKey;
        this.label = label;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isSystemDefault = isSystemDefault != null ? isSystemDefault : Boolean.FALSE;
        this.status = EnumerationValueStatus.ACTIVE;
    }

    /** label/displayOrder만 수정 가능하다 — valueKey는 불변. is_system_default 값에도 허용된다(§C.2). */
    public void updateLabelAndOrder(String label, Integer displayOrder) {
        this.label = label;
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }

    public void deprecate() {
        this.status = EnumerationValueStatus.DEPRECATED;
    }

    public boolean isActive() {
        return status == EnumerationValueStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
