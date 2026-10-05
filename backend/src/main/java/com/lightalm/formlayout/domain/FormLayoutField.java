package com.lightalm.formlayout.domain;

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
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

/**
 * ADR-012 §B.2. 섹션 안의 필드 배치 1건. {@code fieldSource}에 따라 {@code standardFieldKey}
 * 또는 {@code customFieldId} 중 정확히 하나만 채워진다(DB CHECK + 생성자 검증으로 이중 방어).
 * {@code customFieldId}는 패키지 경계를 유지하기 위해 {@code CustomFieldDefinition}에 대한
 * JPA 연관관계가 아니라 단순 ID 컬럼으로 보관한다(DB FK는 마이그레이션에서 건다) — ADR-012 §A의
 * {@code enumerationSetId} 처리와 같은 패턴.
 */
@Entity
@Table(name = "form_layout_fields")
@Getter
public class FormLayoutField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private FormLayoutSection section;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_source", nullable = false, length = 20)
    private FieldSource fieldSource;

    @Column(name = "standard_field_key", length = 50)
    private String standardFieldKey;

    @Column(name = "custom_field_id")
    private Long customFieldId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean visible;

    protected FormLayoutField() {
        // JPA
    }

    @Builder
    private FormLayoutField(FieldSource fieldSource, String standardFieldKey, Long customFieldId,
                             Integer displayOrder, Boolean visible) {
        if (fieldSource == FieldSource.STANDARD) {
            if (standardFieldKey == null || customFieldId != null) {
                throw new IllegalArgumentException("STANDARD 필드는 standardFieldKey만 가져야 합니다.");
            }
        } else {
            if (customFieldId == null || standardFieldKey != null) {
                throw new IllegalArgumentException("CUSTOM 필드는 customFieldId만 가져야 합니다.");
            }
        }
        this.fieldSource = fieldSource;
        this.standardFieldKey = standardFieldKey;
        this.customFieldId = customFieldId;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.visible = visible != null ? visible : Boolean.TRUE;
    }

    void assignSection(FormLayoutSection section) {
        this.section = section;
    }
}
