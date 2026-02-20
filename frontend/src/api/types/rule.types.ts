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
export type VersionStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'DEPRECATED';

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
  createdAt: string;
  updatedAt: string;
}

export interface RuleVersion {
  id: string;
  ruleId: string;
  version: number;
  definition: Record<string, unknown>;
  inputModel: Record<string, unknown>;
  outputModel: Record<string, unknown>;
  status: VersionStatus;
  changeNote?: string;
  createdAt: string;
  publishedAt?: string;
}

export interface SolveRequest {
  data: Record<string, unknown>;
  options?: {
    strategy?: 'STANDARD' | 'FIRST_MATCH' | 'ARRAY';
    audit?: boolean;
  };
}

export interface SolveResponse {
  correlationId: string;
  ruleId: string;
  version: number;
  executionTimeMs: number;
  results: Record<string, unknown>[];
}
