package com.lightalm.theme.repository;

import com.lightalm.theme.domain.SystemThemeSettings;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ADR-014 §3. 항상 {@code id=1} 단일 행만 조회/수정한다({@link #findById}로 충분하다).
 */
public interface SystemThemeSettingsRepository extends JpaRepository<SystemThemeSettings, Long> {
}
