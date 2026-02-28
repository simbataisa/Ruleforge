import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  Rule,
  RuleVersion,
  SampleInput,
  TestRequest,
  TestResponse,
  TagCount,
} from './types/rule.types';
import type { ApiResponse, PageResponse, PageParams } from './types/common.types';

interface RuleListParams extends PageParams {
  folderId?: string;
  type?: string;
  status?: string;
  search?: string;
  tags?: string[];
  types?: string[];
  statuses?: string[];
  createdBy?: string;
  updatedAfter?: string;
  updatedBefore?: string;
}

export function useRules(params?: RuleListParams) {
  return useQuery({
    queryKey: ['rules', params],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PageResponse<Rule>>>(
        '/rules',
        { params },
      );
      return res.data.data;
    },
  });
}

export function useRule(id: string) {
  return useQuery({
    queryKey: ['rules', id],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<Rule>>(`/rules/${id}`);
      return res.data.data;
    },
    enabled: !!id,
  });
}

export function useCreateRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (
      data: { name: string; type: string; description?: string; alias?: string; folderId?: string },
    ) => {
      const res = await apiClient.post<ApiResponse<Rule>>('/rules', data);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rules'] });
    },
  });
}

export function useUpdateRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      id,
      data,
    }: {
      id: string;
      data: Partial<Pick<Rule, 'name' | 'description' | 'alias' | 'folderId'>>;
    }) => {
      const res = await apiClient.put<ApiResponse<Rule>>(`/rules/${id}`, data);
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['rules'] });
      queryClient.invalidateQueries({ queryKey: ['rules', variables.id] });
    },
  });
}

export function useDeleteRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/rules/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rules'] });
    },
  });
}

export function useRuleVersions(ruleId: string) {
  return useQuery({
    queryKey: ['rules', ruleId, 'versions'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<RuleVersion[]>>(
        `/rules/${ruleId}/versions`,
      );
      return res.data.data;
    },
    enabled: !!ruleId,
  });
}

export function useUpdateVersion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      version,
      data,
    }: {
      ruleId: string;
      version: number;
      data: { definition?: string; inputModel?: string; outputModel?: string; changeNote?: string };
    }) => {
      const res = await apiClient.put<ApiResponse<RuleVersion>>(
        `/rules/${ruleId}/versions/${version}`,
        data,
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId, 'versions'],
      });
    },
  });
}

export function useCreateVersion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      data,
    }: {
      ruleId: string;
      data: { definition?: string; inputModel?: string; outputModel?: string; changeNote?: string };
    }) => {
      const res = await apiClient.post<ApiResponse<RuleVersion>>(
        `/rules/${ruleId}/versions`,
        data,
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId, 'versions'],
      });
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId],
      });
    },
  });
}

export function usePublishVersion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      version,
    }: {
      ruleId: string;
      version: number;
    }) => {
      const res = await apiClient.post<ApiResponse<RuleVersion>>(
        `/rules/${ruleId}/versions/${version}/publish`,
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId, 'versions'],
      });
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId],
      });
    },
  });
}

export function useRequestApproval() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      version,
    }: {
      ruleId: string;
      version: number;
    }) => {
      const res = await apiClient.post<ApiResponse<RuleVersion>>(
        `/rules/${ruleId}/versions/${version}/request-approval`,
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId, 'versions'],
      });
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId],
      });
    },
  });
}

export function useApproveVersion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      version,
    }: {
      ruleId: string;
      version: number;
    }) => {
      const res = await apiClient.post<ApiResponse<RuleVersion>>(
        `/rules/${ruleId}/versions/${version}/approve`,
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['approval-requests'] });
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId, 'versions'],
      });
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId],
      });
    },
  });
}

export function useRejectVersion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      version,
      reason,
    }: {
      ruleId: string;
      version: number;
      reason: string;
    }) => {
      const res = await apiClient.post<ApiResponse<RuleVersion>>(
        `/rules/${ruleId}/versions/${version}/reject`,
        { reason },
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['approval-requests'] });
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId, 'versions'],
      });
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId],
      });
    },
  });
}

export function useSampleInputs(ruleId: string) {
  return useQuery({
    queryKey: ['rules', ruleId, 'samples'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<SampleInput[]>>(
        `/rules/${ruleId}/samples`,
      );
      return res.data.data;
    },
    enabled: !!ruleId,
  });
}

export function useCreateSampleInput() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      data,
    }: {
      ruleId: string;
      data: { name: string; description?: string; input: unknown };
    }) => {
      const res = await apiClient.post<ApiResponse<SampleInput>>(
        `/rules/${ruleId}/samples`,
        data,
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['rules', variables.ruleId, 'samples'] });
    },
  });
}

export function useUpdateSampleInput() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      sampleId,
      data,
    }: {
      ruleId: string;
      sampleId: string;
      data: { name?: string; description?: string; input?: unknown; sortOrder?: number };
    }) => {
      const res = await apiClient.put<ApiResponse<SampleInput>>(
        `/rules/${ruleId}/samples/${sampleId}`,
        data,
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['rules', variables.ruleId, 'samples'] });
    },
  });
}

export function useDeleteSampleInput() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      sampleId,
    }: {
      ruleId: string;
      sampleId: string;
    }) => {
      await apiClient.delete(`/rules/${ruleId}/samples/${sampleId}`);
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['rules', variables.ruleId, 'samples'] });
    },
  });
}

export function useCopyRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      name,
    }: {
      ruleId: string;
      name?: string;
    }) => {
      const res = await apiClient.post<ApiResponse<Rule>>(
        `/rules/${ruleId}/copy`,
        name ? { name } : {},
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rules'] });
    },
  });
}

// --- Approval Comments ---

export interface ApprovalComment {
  id: string;
  ruleId: string;
  version: number;
  userId: string;
  userName: string;
  body: string;
  parentId?: string;
  createdAt: string;
}

export function useApprovalComments(ruleId: string, version: number) {
  return useQuery({
    queryKey: ['rules', ruleId, 'versions', version, 'comments'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<ApprovalComment[]>>(
        `/rules/${ruleId}/versions/${version}/comments`,
      );
      return res.data.data;
    },
    enabled: !!ruleId && version > 0,
  });
}

export function useAddApprovalComment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      ruleId,
      version,
      body,
      parentId,
    }: {
      ruleId: string;
      version: number;
      body: string;
      parentId?: string;
    }) => {
      const res = await apiClient.post<ApiResponse<ApprovalComment>>(
        `/rules/${ruleId}/versions/${version}/comments`,
        { body, parentId },
      );
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId, 'versions', variables.version, 'comments'],
      });
    },
  });
}

export function usePendingApprovalCount() {
  return useQuery({
    queryKey: ['rules', 'pending-approval-count'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PageResponse<Rule>>>(
        '/rules',
        { params: { status: 'PENDING_APPROVAL', page: 0, size: 1 } },
      );
      return res.data.data.totalElements;
    },
    refetchInterval: 60000,
  });
}

export function useTestRule() {
  return useMutation({
    mutationFn: async ({
      ruleId,
      version,
      data,
    }: {
      ruleId: string;
      version?: number;
      data: TestRequest;
    }) => {
      const params = version != null ? `?version=${version}` : '';
      const res = await apiClient.post<ApiResponse<TestResponse>>(
        `/rules/${ruleId}/test${params}`,
        data,
      );
      return res.data.data;
    },
  });
}

// --- Tags ---

export function useTags(query?: string) {
  return useQuery({
    queryKey: ['tags', query],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<TagCount[]>>('/tags', {
        params: query ? { q: query } : {},
      });
      return res.data.data;
    },
  });
}

// --- Batch Operations ---

export function useBatchRequestApproval() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: {
      ruleIds: string[];
      assigneeIds?: string[];
      note?: string;
    }) => {
      const res = await apiClient.post<
        ApiResponse<{
          succeeded: { ruleId: string; version: number }[];
          failed: { ruleId: string; reason: string }[];
        }>
      >('/rules/batch/request-approval', data);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rules'] });
    },
  });
}

export function useBatchExport() {
  return useMutation({
    mutationFn: async (data: { ruleIds: string[]; format?: string }) => {
      const res = await apiClient.post('/rules/batch/export', data, {
        responseType: 'blob',
      });
      return res.data;
    },
  });
}

// --- Approval Requests ---

export interface ApprovalAssigneeDto {
  memberId: string;
  name: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  respondedAt: string | null;
}

export interface ApprovalRequestDto {
  id: string;
  ruleId: string;
  ruleName: string;
  ruleType: string;
  version: number;
  requestedBy: string;
  requestedByName: string;
  requestedAt: string;
  note: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  resolvedBy: string | null;
  resolvedAt: string | null;
  deadline: string | null;
  assignees: ApprovalAssigneeDto[];
}

interface ApprovalRequestListParams extends PageParams {
  assignedToMe?: boolean;
  requestedByMe?: boolean;
  status?: string;
  ruleId?: string;
  from?: string;
  to?: string;
}

export function useApprovalRequests(params?: ApprovalRequestListParams) {
  return useQuery({
    queryKey: ['approval-requests', params],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PageResponse<ApprovalRequestDto>>>(
        '/approval-requests',
        { params },
      );
      return res.data.data;
    },
  });
}

export function useApprovalRequest(requestId: string) {
  return useQuery({
    queryKey: ['approval-requests', requestId],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<ApprovalRequestDto>>(
        `/approval-requests/${requestId}`,
      );
      return res.data.data;
    },
    enabled: !!requestId,
  });
}

export function useCancelApprovalRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (requestId: string) => {
      const res = await apiClient.post<ApiResponse<ApprovalRequestDto>>(
        `/approval-requests/${requestId}/cancel`,
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['approval-requests'] });
      queryClient.invalidateQueries({ queryKey: ['rules'] });
    },
  });
}

// --- Eligible Approvers ---

export interface EligibleApproverDto {
  memberId: string;
  name: string;
  email: string;
  role: string;
  isDefaultApprover: boolean;
}

export function useEligibleApprovers(ruleId: string) {
  return useQuery({
    queryKey: ['rules', ruleId, 'eligible-approvers'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<EligibleApproverDto[]>>(
        `/rules/${ruleId}/eligible-approvers`,
      );
      return res.data.data;
    },
    enabled: !!ruleId,
  });
}

// --- Space Approval Settings ---

export interface SpaceApprovalSettingsDto {
  spaceId: string;
  defaultApprovers: string[];
  slaHours: number;
  requireAllApprovers: boolean;
}

export function useSpaceApprovalSettings(spaceId: string) {
  return useQuery({
    queryKey: ['spaces', spaceId, 'approval-settings'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<SpaceApprovalSettingsDto>>(
        `/spaces/${spaceId}/approval-settings`,
      );
      return res.data.data;
    },
    enabled: !!spaceId,
  });
}
