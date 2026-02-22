import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type {
  Rule,
  RuleVersion,
  TestRequest,
  TestResponse,
} from './types/rule.types';
import type { ApiResponse, PageResponse, PageParams } from './types/common.types';

interface RuleListParams extends PageParams {
  folderId?: string;
  type?: string;
  status?: string;
  search?: string;
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
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId, 'versions'],
      });
      queryClient.invalidateQueries({
        queryKey: ['rules', variables.ruleId],
      });
    },
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
