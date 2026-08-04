import type { ReleaseStatus, TargetType } from './common';

export interface ReleaseItem {
  id: number;
  targetType: TargetType;
  targetId: number;
  targetKey: string | null;
  targetTitle: string | null;
  targetStatus: string | null;
  addedById: number | null;
  addedByName: string | null;
  addedAt: string;
}

export interface Release {
  id: number;
  projectId: number;
  version: string;
  name: string | null;
  status: ReleaseStatus;
  releaseDate: string | null;
  description: string | null;
  createdById: number | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
  items: ReleaseItem[];
}
