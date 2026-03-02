import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';

export type EntryType = 'PUBLIC_HOLIDAY' | 'AD_HOC' | 'DAY_IN_LIEU' | 'COMPANY_HOLIDAY';
export type SkipOnHoliday = 'SKIP' | 'DEFER_NEXT' | 'RUN_ANYWAY';

export interface HolidayCalendar {
  id: string;
  name: string;
  description?: string;
  market: string;
  region?: string;
  weekendDays: string[];
  enabled: boolean;
  entryCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface HolidayEntry {
  id: string;
  calendarId: string;
  date: string;
  name: string;
  entryType: EntryType;
  recurring: boolean;
  createdAt: string;
}

export interface CreateHolidayCalendarRequest {
  name: string;
  description?: string;
  market: string;
  region?: string;
  weekendDays?: string[];
  enabled?: boolean;
}

export interface CreateHolidayEntryRequest {
  date: string;
  name: string;
  entryType: EntryType;
  recurring?: boolean;
}

// Calendar hooks

export function useHolidayCalendars() {
  return useQuery({
    queryKey: ['holiday-calendars'],
    queryFn: async () => {
      const res = await apiClient.get<HolidayCalendar[]>('/holiday-calendars');
      const items = Array.isArray(res.data) ? res.data : [];
      return items;
    },
  });
}

export function useHolidayCalendar(id: string) {
  return useQuery({
    queryKey: ['holiday-calendars', id],
    queryFn: async () => {
      const res = await apiClient.get<HolidayCalendar>(`/holiday-calendars/${id}`);
      return res.data;
    },
    enabled: !!id,
  });
}

export function useCreateHolidayCalendar() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateHolidayCalendarRequest) => {
      const res = await apiClient.post<HolidayCalendar>('/holiday-calendars', data);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['holiday-calendars'] });
    },
  });
}

export function useUpdateHolidayCalendar() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: Partial<CreateHolidayCalendarRequest> }) => {
      const res = await apiClient.put<HolidayCalendar>(`/holiday-calendars/${id}`, data);
      return res.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['holiday-calendars'] });
      queryClient.invalidateQueries({ queryKey: ['holiday-calendars', variables.id] });
    },
  });
}

export function useDeleteHolidayCalendar() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/holiday-calendars/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['holiday-calendars'] });
    },
  });
}

// Entry hooks

export function useHolidayEntries(calendarId: string) {
  return useQuery({
    queryKey: ['holiday-calendars', calendarId, 'entries'],
    queryFn: async () => {
      const res = await apiClient.get<HolidayEntry[]>(`/holiday-calendars/${calendarId}/entries`);
      const items = Array.isArray(res.data) ? res.data : [];
      return items;
    },
    enabled: !!calendarId,
  });
}

export function useCreateHolidayEntry() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ calendarId, data }: { calendarId: string; data: CreateHolidayEntryRequest }) => {
      const res = await apiClient.post<HolidayEntry>(`/holiday-calendars/${calendarId}/entries`, data);
      return res.data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['holiday-calendars', variables.calendarId, 'entries'] });
      queryClient.invalidateQueries({ queryKey: ['holiday-calendars'] });
    },
  });
}

export function useDeleteHolidayEntry() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ calendarId, entryId }: { calendarId: string; entryId: string }) => {
      await apiClient.delete(`/holiday-calendars/${calendarId}/entries/${entryId}`);
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['holiday-calendars', variables.calendarId, 'entries'] });
      queryClient.invalidateQueries({ queryKey: ['holiday-calendars'] });
    },
  });
}

export function useCheckHoliday(calendarId: string, date: string) {
  return useQuery({
    queryKey: ['holiday-calendars', calendarId, 'check', date],
    queryFn: async () => {
      const res = await apiClient.get<{ isHoliday: boolean; entries: HolidayEntry[] }>(
        `/holiday-calendars/${calendarId}/check`,
        { params: { date } },
      );
      return res.data;
    },
    enabled: !!calendarId && !!date,
  });
}
