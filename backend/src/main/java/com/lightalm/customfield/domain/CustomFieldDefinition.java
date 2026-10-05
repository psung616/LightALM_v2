package com.lightalm.customfield.domain;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * ADR-012 §A.3. 프로젝트+target_type별 커스텀 필드 정의(EAV의 Attribute 쪽).
 * {@code dataType}/{@code fieldKey}는 생성 후 불변이다 — 세터를 두지 않고,
 * 상태 변경은 {@link #updateDetails(String, Boolean, String, Integer)}/{@link #deprecate()}로만 한다.
 */
@Entity
@Table(name = "custom_field_definitions")
@Getter
public class CustomFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "field_key", nullable = false, length = 50)
    private String fieldKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private CustomFieldDataType dataType;

    /**
     * Phase 22(ADR-012 §C)의 project_enumeration_sets.id를 가리킬 예정이나,
     * 아직 그 테이블이 없어 FK 제약 없이 값만 보관한다(V13 마이그레이션 주석 참고).
     */
    @Column(name = "enumeration_set_id")
    private Long enumerationSetId;

    @Column(nullable = false)
    private Boolean required;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomFieldStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CustomFieldDefinition() {
        // JPA
    }

    @Builder
    private CustomFieldDefinition(Project project, TargetType targetType, String fieldKey, String label,
                                   CustomFieldDataType dataType, Long enumerationSetId, Boolean required,
                                   String defaultValue, Integer displayOrder, User createdBy) {
        this.project = project;
        this.targetType = targetType;
        this.fieldKey = fieldKey;
        this.label = label;
        this.dataType = dataType;
        this.enumerationSetId = enumerationSetId;
        this.required = required != null ? required : Boolean.FALSE;
        this.defaultValue = defaultValue;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.status = CustomFieldStatus.ACTIVE;
        this.createdBy = createdBy;
    }

    /**
     * label/required/defaultValue/displayOrder만 수정 가능하다 — dataType/fieldKey는 불변(ADR-012 §A.4).
     */
    public void updateDetails(String label, Boolean required, String defaultValue, Integer displayOrder) {
        this.label = label;
        this.required = required != null ? required : Boolean.FALSE;
        this.defaultValue = defaultValue;
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
    }

    public void deprecate() {
        this.status = CustomFieldStatus.DEPRECATED;
    }

    public boolean isActive() {
        return status == CustomFieldStatus.ACTIVE;
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
