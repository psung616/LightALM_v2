// ADR-008(Phase 16) / 04-api.md §4.18
// 베이스라인은 테스트케이스도 담을 수 있어 common.ts의 TargetType(REQUIREMENT|ISSUE)보다 넓다.
export type BaselineTargetType = 'REQUIREMENT' | 'ISSUE' | 'TEST_CASE';

export const BASELINE_TARGET_LABEL: Record<BaselineTargetType, string> = {
  REQUIREMENT: '요구사항',
  ISSUE: '이슈',
  TEST_CASE: '테스트케이스',
};

export type SnapshotValue = string | number | boolean | null;
export type BaselineSnapshot = Record<string, SnapshotValue>;

export interface BaselineItemRef {
  targetType: BaselineTargetType;
  targetId: number;
}

export interface BaselineSummary {
  id: number;
  name: string;
  description: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  itemCount: number;
}

export interface BaselineItem {
  id: number;
  targetType: BaselineTargetType;
  targetId: number;
  snapshot: BaselineSnapshot;
  capturedAt: string;
}

export interface BaselineDetail {
  id: number;
  name: string;
  description: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  items: BaselineItem[];
}

export type BaselineFieldChangeKind = 'ADDED' | 'REMOVED' | 'MODIFIED';
export type BaselineItemChangeType = 'UNCHANGED' | 'MODIFIED' | 'DELETED';

export interface BaselineFieldChange {
  field: string;
  changeKind: BaselineFieldChangeKind;
  before: SnapshotValue;
  after: SnapshotValue;
}

export interface BaselineItemDiff {
  targetType: BaselineTargetType;
  targetId: number;
  key: string | null;
  title: string | null;
  changeType: BaselineItemChangeType;
  changes: BaselineFieldChange[];
}

export interface BaselineDiff {
  baselineId: number;
  baselineName: string;
  baselineCreatedAt: string;
  comparedAt: string;
  unchangedCount: number;
  modifiedCount: number;
  deletedCount: number;
  items: BaselineItemDiff[];
}
