package com.lightalm.theme.domain;

/**
 * ADR-014 §2. 시스템 전역 색상 프리셋 5종. {@code DEFAULT}는 기존 {@code index.css}의
 * 라벤더-블루 토큰 값과 100% 동일해, 이 프리셋이 선택된 상태는 기능 배포 전후로 화면이
 * 픽셀 단위로 동일해야 한다(하위 호환성). 특정 기업의 등록 브랜드 컬러가 아니라 통용되는
 * 웹 디자인 팔레트 색조에서 이 프로젝트가 자체적으로 고른 값이다.
 *
 * <p>여기 담긴 {@code primaryColor}는 DB에는 저장하지 않고(03-data-model.md §3.30 참고),
 * 관리자 화면 스와치 미리보기 용도로만 API 응답에 실어 보낸다. 실제 화면 적용은
 * 프론트엔드 {@code index.css}의 속성 선택자(data-theme-color)가 담당한다.</p>
 */
public enum ThemeColorPreset {
    DEFAULT("기본(라벤더)", "#5e6ad2"),
    RED("레드", "#dc2626"),
    BLUE("블루", "#2563eb"),
    GREEN("그린", "#16a34a"),
    PURPLE("퍼플", "#7c3aed");

    private final String label;
    private final String primaryColor;

    ThemeColorPreset(String label, String primaryColor) {
        this.label = label;
        this.primaryColor = primaryColor;
    }

    public String getLabel() {
        return label;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }
}
