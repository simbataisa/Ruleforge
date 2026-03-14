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
  workflowId?: string;
  workflowName?: string;
  cronExpression: string;
  timezone: string;
  status: 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'FAILED';
  nextRunAt?: string;
  lastRunAt?: string;
  inputTemplate?: Record<string, unknown>;
  holidayCalendarId?: string;
  skipOnHoliday?: 'SKIP' | 'DEFER_NEXT' | 'RUN_ANYWAY';
  isWorkflowSchedule?: boolean;
  holidayCalendarName?: string;
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
  status: 'SUCCESS' | 'FAILED' | 'RUNNING' | 'CANCELLED';
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
  workflowId?: string;
  cronExpression: string;
  timezone?: string;
  inputTemplate?: Record<string, unknown>;
  holidayCalendarId?: string;
  skipOnHoliday?: string;
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
      const res = await apiClient.get<Schedule[]>('/schedules', { params });
      // Backend returns a plain array; wrap into PageResponse shape for consistency
      const items = Array.isArray(res.data) ? res.data : (res.data as unknown as ApiResponse<Schedule[]>).data ?? [];
      return { content: items, page: 0, size: items.length, totalElements: items.length, totalPages: 1 } as PageResponse<Schedule>;
    },
  });
}

export function useSchedule(id: string) {
  return useQuery({
    queryKey: ['schedules', id],
    queryFn: async () => {
      const res = await apiClient.get<Schedule>(`/schedules/${id}`);
      return res.data;
    },
    enabled: !!id,
  });
}

export interface ScheduleRunsPage {
  content: ScheduleRun[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export function useScheduleRuns(scheduleId: string, params?: { page?: number; size?: number }) {
  const page = params?.page ?? 0;
  const size = params?.size ?? 10;
  return useQuery({
    queryKey: ['schedules', scheduleId, 'runs', { page, size }],
    queryFn: async () => {
      const res = await apiClient.get<ScheduleRun[] | ScheduleRunsPage>(
        `/schedules/${scheduleId}/runs`,
        { params: { page, size, sort: 'startedAt,desc' } },
      );
      const data = res.data;
      if (Array.isArray(data)) {
        return { content: data, totalElements: data.length, totalPages: 1, number: 0, size: data.length };
      }
      return data as ScheduleRunsPage;
    },
    enabled: !!scheduleId,
    refetchInterval: 30_000,
  });
}

export function useTriggerSchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      const res = await apiClient.post<ScheduleRun>(`/schedules/${id}/trigger`);
      return res.data;
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
      const res = await apiClient.post<Schedule>('/schedules', data);
      return res.data;
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
      const res = await apiClient.put<Schedule>(`/schedules/${id}`, data);
      return res.data;
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
      const res = await apiClient.post<Schedule>(`/schedules/${id}/pause`);
      return res.data;
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
      const res = await apiClient.post<Schedule>(`/schedules/${id}/resume`);
      return res.data;
    },
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', id] });
    },
  });
}
