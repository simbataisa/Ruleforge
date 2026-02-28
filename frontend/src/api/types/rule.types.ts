export type RuleType =
  | 'TABLE'
  | 'TREE'
  | 'SCRIPT'
  | 'FLOW'
  | 'SCORECARD'
  | 'DMN'
  | 'CEP'
  | 'LOOKUP'
  | 'ENSEMBLE';

export type RuleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type VersionStatus = 'DRAFT' | 'PENDING' | 'PENDING_APPROVAL' | 'PUBLISHED' | 'DEPRECATED';

export interface Rule {
  id: string;
  spaceId: string;
  folderId?: string;
  name: string;
  alias?: string;
  type: RuleType;
  description?: string;
  status: RuleStatus;
  latestVersion: number;
  locked: boolean;
  lockedBy?: string;
  tags?: string[];
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TagCount {
  tag: string;
  count: number;
}

export interface RuleVersion {
  id: string;
  ruleId: string;
  version: number;
  definition: Record<string, unknown> | string;
  inputModel: Record<string, unknown>;
  outputModel: Record<string, unknown>;
  status: VersionStatus;
  changeNote?: string;
  createdAt: string;
  publishedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  rejectionReason?: string;
}

export interface SampleInput {
  id: string;
  name: string;
  description: string | null;
  input: string | Record<string, unknown>;
  sortOrder: number;
}

export interface TestRequest {
  input: Record<string, unknown>;
  options?: Record<string, unknown>;
}

export interface TestResponse {
  output: Record<string, unknown>;
  executionTimeMs: number;
  logs: string[];
  success: boolean;
  error: string | null;
}
