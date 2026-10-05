package com.lightalm.theme.domain;

import com.lightalm.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * ADR-014 §3. 시스템 전역 테마 설정 — 항상 정확히 1행만 존재하는 단일 행 테이블
 * ({@code id=1} 고정, DB의 {@code CHECK (id = 1)}로 강제). 상태 변경은
 * {@link #changeColorPreset(ThemeColorPreset, User)} 도메인 동사 메서드로만 한다(세터 금지).
 */
@Entity
@Table(name = "system_theme_settings")
@Getter
public class SystemThemeSettings {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_preset", nullable = false, length = 20)
    private ThemeColorPreset colorPreset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SystemThemeSettings() {
        // JPA
    }

    @Builder
    private SystemThemeSettings(Long id, ThemeColorPreset colorPreset, User updatedBy, LocalDateTime updatedAt) {
        this.id = id;
        this.colorPreset = colorPreset;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public void changeColorPreset(ThemeColorPreset colorPreset, User updatedBy) {
        this.colorPreset = colorPreset;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
