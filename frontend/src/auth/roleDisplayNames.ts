import type { ProjectRole, SystemRole } from '../types/common';

/**
 * ADR-015 D1. 역할의 화면 표시명 매핑 — 이 파일이 유일한 출처다.
 * DB/API 값(ADMIN, USER, PROJECT_ADMIN, MEMBER, VIEWER)은 그대로 두고 라벨에만 표시명을 쓴다.
 * 주의: 시스템 역할 USER("User")와 프로젝트 역할 VIEWER("Project User")는 서로 다른 개념이다.
 */
export const SYSTEM_ROLE_DISPLAY_NAMES: Record<SystemRole, string> = {
  ADMIN: 'System Admin',
  USER: 'User',
};

export const PROJECT_ROLE_DISPLAY_NAMES: Record<ProjectRole, string> = {
  PROJECT_ADMIN: 'Project Admin',
  MEMBER: 'Project Assignable',
  VIEWER: 'Project User',
};

/** 사용자 관리 화면 드롭다운 순서. */
export const SYSTEM_ROLE_OPTIONS: SystemRole[] = ['USER', 'ADMIN'];

/** 프로젝트 멤버 역할 드롭다운 순서 — 권한이 높은 쪽부터. */
export const PROJECT_ROLE_OPTIONS: ProjectRole[] = ['PROJECT_ADMIN', 'MEMBER', 'VIEWER'];
