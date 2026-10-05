package com.lightalm.baseline.domain;

import com.lightalm.domain.TargetType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ADR-008(Phase 16) / 03-data-model.md §3.21. 베이스라인 생성 시점의 대상 필드 값을 그대로
 * 얼려서 저장한 스냅샷 한 건. {@code snapshot}은 생성 시점에 확정되며 이후 절대 수정하지 않는다
 * (수정 메서드를 두지 않음 — 불변).
 *
 * <p>비교(diff)는 이 테이블에 저장하지 않는다. 조회 시점에 서비스 레이어가 이 snapshot과
 * 원본 테이블의 현재 값을 필드 단위로 비교해서 계산한다(03-data-model.md §3.21 각주).</p>
 */
@Entity
@Table(name = "baseline_items")
@Getter
public class BaselineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baseline_id", nullable = false)
    private Baseline baseline;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * 생성 시점 주요 필드 스냅샷(title/description/status/priority 등)을 JSON 문자열로 보관한다.
     * Hibernate 6 네이티브 JSON 매핑({@code @JdbcTypeCode(SqlTypes.JSON)})을 사용해 DB 컬럼은
     * jsonb지만 자바 쪽은 이미 직렬화된 JSON 문자열을 그대로 주고받는다(별도 라이브러리 추가 없음).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String snapshot;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private LocalDateTime capturedAt;

    protected BaselineItem() {
        // JPA
    }

    @Builder
    private BaselineItem(Baseline baseline, TargetType targetType, Long targetId, String snapshot) {
        this.baseline = baseline;
        this.targetType = targetType;
        this.targetId = targetId;
        this.snapshot = snapshot;
    }

    @PrePersist
    void onCreate() {
        this.capturedAt = LocalDateTime.now();
    }
}
