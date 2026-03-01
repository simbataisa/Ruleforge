import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse, PageResponse, PageParams } from './types/common.types';

export interface Schedule {
  id: string;
  name: string;
  description?: string;
  ruleId: string;
  ruleName?: string;
  ruleType?: string;
  cronExpression: string;
  timezone: string;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'FAILED';
  nextRunAt?: string;
  lastRunAt?: string;
  inputPayload?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleStats {
  total: number;
  active: number;
  paused: number;
  successLast24h: number;
  failedLast24h: number;
}

export interface ScheduleRun {
  id: string;
  scheduleId: string;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING' | 'SKIPPED';
  startedAt: string;
  completedAt?: string;
  result?: Record<string, unknown>;
  error?: string;
}

export interface CreateScheduleRequest {
  name: string;
  description?: string;
  ruleId: string;
  ruleName?: string;
  ruleType?: string;
  cronExpression: string;
  timezone?: string;
  inputPayload?: Record<string, unknown>;
}

export function useScheduleStats() {
  return useQuery({
    queryKey: ['schedules', 'stats'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<ScheduleStats>>('/schedules/stats');
      return res.data.data;
    },
    refetchInterval: 30_000,
  });
}

export function useSchedules(params?: PageParams) {
  return useQuery({
    queryKey: ['schedules', params],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<PageResponse<Schedule>>>(
        '/schedules',
        { params },
      );
      return res.data.data;
    },
  });
}

export function useSchedule(id: string) {
  return useQuery({
    queryKey: ['schedules', id],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<Schedule>>(`/schedules/${id}`);
      return res.data.data;
    },
    enabled: !!id,
  });
}

export function useScheduleRuns(scheduleId: string) {
  return useQuery({
    queryKey: ['schedules', scheduleId, 'runs'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<ScheduleRun[]>>(
        `/schedules/${scheduleId}/runs`,
      );
      return res.data.data;
    },
    enabled: !!scheduleId,
    refetchInterval: 30_000,
  });
}

export function useTriggerSchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      const res = await apiClient.post<ApiResponse<ScheduleRun>>(`/schedules/${id}/trigger`);
      return res.data.data;
    },
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ['schedules', id, 'runs'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', 'stats'] });
    },
  });
}

export function useCreateSchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateScheduleRequest) => {
      const res = await apiClient.post<ApiResponse<Schedule>>('/schedules', data);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });
}

export function useUpdateSchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: Partial<CreateScheduleRequest> }) => {
      const res = await apiClient.put<ApiResponse<Schedule>>(`/schedules/${id}`, data);
      return res.data.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', variables.id] });
    },
  });
}

export function useDeleteSchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/schedules/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });
}

export function usePauseSchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      const res = await apiClient.post<ApiResponse<Schedule>>(`/schedules/${id}/pause`);
      return res.data.data;
    },
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', id] });
    },
  });
}

export function useResumeSchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      const res = await apiClient.post<ApiResponse<Schedule>>(`/schedules/${id}/resume`);
      return res.data.data;
    },
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', id] });
    },
  });
}
