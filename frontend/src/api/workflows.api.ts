import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { PageResponse, PageParams } from './types/common.types';

// Types

export interface WorkflowNode {
  id: string;
  type: 'rule' | 'condition' | 'trigger' | 'output';
  position: { x: number; y: number };
  data: {
    label: string;
    ruleId?: string;
    ruleName?: string;
    ruleType?: string;
    config?: Record<string, unknown>;
  };
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
}

export type TriggerType = 'CRON' | 'MANUAL' | 'WEBHOOK' | 'EVENT';

export interface Workflow {
  id: string;
  name: string;
  description?: string;
  definition: { nodes: WorkflowNode[]; edges: WorkflowEdge[] };
  enabled: boolean;
  cronExpression?: string;
  triggerType: TriggerType;
  triggerConfig?: Record<string, unknown>;
  status: 'ACTIVE' | 'PAUSED' | 'DRAFT';
  holidayCalendarId?: string;
  skipOnHoliday?: string;
  holidayCalendarName?: string;
  scheduleId?: string;
  scheduleName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowRun {
  id: string;
  workflowId: string;
  status: 'RUNNING' | 'SUCCESS' | 'FAILED';
  startedAt: string;
  completedAt?: string;
  stepResults?: unknown[];
  error?: string;
  createdAt: string;
}

export interface CreateWorkflowRequest {
  name: string;
  description?: string;
  definition?: { nodes: WorkflowNode[]; edges: WorkflowEdge[] };
  cronExpression?: string;
  triggerType?: TriggerType;
  triggerConfig?: Record<string, unknown>;
  holidayCalendarId?: string;
  skipOnHoliday?: string;
}

// Hooks

export function useWorkflows(params?: PageParams) {
  return useQuery({
    queryKey: ['workflows', params],
    queryFn: async () => {
      const res = await apiClient.get<Workflow[]>('/workflows', { params });
      const items = Array.isArray(res.data) ? res.data : [];
      return { content: items, page: 0, size: items.length, totalElements: items.length, totalPages: 1 } as PageResponse<Workflow>;
    },
  });
}

export function useWorkflow(id: string) {
  return useQuery({
    queryKey: ['workflows', id],
    queryFn: async () => {
      const res = await apiClient.get<Workflow>(`/workflows/${id}`);
      return res.data;
    },
    enabled: !!id && id !== 'new',
  });
}

export function useCreateWorkflow() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateWorkflowRequest) => {
      const res = await apiClient.post<Workflow>('/workflows', data);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
  });
}

export function useUpdateWorkflow() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: Partial<CreateWorkflowRequest & { enabled: boolean; status: string; triggerType: TriggerType; triggerConfig: Record<string, unknown> }> }) => {
      const res = await apiClient.put<Workflow>(`/workflows/${id}`, data);
      return res.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
      queryClient.invalidateQueries({ queryKey: ['workflows', variables.id] });
    },
  });
}

export function useDeleteWorkflow() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/workflows/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
  });
}

export function useWorkflowRuns(workflowId: string) {
  return useQuery({
    queryKey: ['workflows', workflowId, 'runs'],
    queryFn: async () => {
      const res = await apiClient.get<WorkflowRun[] | { content: WorkflowRun[] }>(
        `/workflows/${workflowId}/runs`,
      );
      const data = res.data;
      return Array.isArray(data) ? data : (data as { content: WorkflowRun[] }).content ?? [];
    },
    enabled: !!workflowId,
    refetchInterval: 30_000,
  });
}
