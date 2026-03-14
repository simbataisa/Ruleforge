import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse } from './types/common.types';

export interface Organization {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
}

export interface OrgMember {
  id: string;
  fullName: string;
  email: string;
  role: string;
}

export interface OrgSpace {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  ruleCount: number;
  orgId: string;
  createdAt: string;
  updatedAt: string;
}

export function useOrganization() {
  return useQuery({
    queryKey: ['organization'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<Organization>>('/organizations/current');
      return res.data.data;
    },
  });
}

export function useOrgMembers() {
  return useQuery({
    queryKey: ['organization', 'members'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<OrgMember[]>>('/organizations/current/members');
      return res.data.data;
    },
  });
}

export function useOrgSpaces() {
  return useQuery({
    queryKey: ['organization', 'spaces'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<OrgSpace[]>>('/organizations/current/spaces');
      return res.data.data;
    },
  });
}

export function useInviteMember() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { email: string; role: string }) => {
      const res = await apiClient.post<ApiResponse<OrgMember>>(
        '/organizations/current/members/invite',
        data,
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization', 'members'] });
    },
  });
}

export function useUpdateMemberRole() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ memberId, role }: { memberId: string; role: string }) => {
      const res = await apiClient.put<ApiResponse<OrgMember>>(
        `/organizations/current/members/${memberId}/role`,
        { role },
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization', 'members'] });
    },
  });
}

export function useCreateSpace() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { name: string }) => {
      const res = await apiClient.post<ApiResponse<OrgSpace>>(
        '/organizations/current/spaces',
        data,
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization', 'spaces'] });
    },
  });
}

export function useSpace(spaceId: string) {
  return useQuery({
    queryKey: ['organization', 'spaces', spaceId],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<OrgSpace>>(
        `/organizations/current/spaces/${spaceId}`,
      );
      return res.data.data;
    },
    enabled: !!spaceId,
  });
}

export function useUpdateSpace(spaceId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { name: string; description?: string }) => {
      const res = await apiClient.put<ApiResponse<OrgSpace>>(
        `/organizations/current/spaces/${spaceId}`,
        data,
      );
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization', 'spaces'] });
    },
  });
}

export function useDeleteSpace() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (spaceId: string) => {
      await apiClient.delete(`/organizations/current/spaces/${spaceId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization', 'spaces'] });
    },
  });
}
