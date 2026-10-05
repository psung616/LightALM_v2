package com.lightalm.customfield.domain;

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
 * ADR-012 §A.3. EAV의 Value 쪽 — 대상(target_type/target_id)별로 필드 1건당 1행.
 * target_type은 정의와 동일한 값을 중복 저장한다({@link com.lightalm.service.support.PolymorphicTargetValidator}
 * 재사용을 위함, ADR-010 패턴). 값 변경은 {@link #changeValue(String, User)}로만 한다.
 */
@Entity
@Table(name = "custom_field_values")
@Getter
public class CustomFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private CustomFieldDefinition field;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CustomFieldValue() {
        // JPA
    }

    @Builder
    private CustomFieldValue(CustomFieldDefinition field, TargetType targetType, Long targetId, String value,
                              User updatedBy) {
        this.field = field;
        this.targetType = targetType;
        this.targetId = targetId;
        this.value = value;
        this.updatedBy = updatedBy;
    }

    public void changeValue(String value, User updatedBy) {
        this.value = value;
        this.updatedBy = updatedBy;
    }

    @PrePersist
    void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
