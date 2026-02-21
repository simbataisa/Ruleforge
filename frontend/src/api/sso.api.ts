import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse } from './types/common.types';

export type SsoProviderType = 'SAML' | 'OIDC';

export interface SsoConfig {
  id: string;
  providerType: SsoProviderType;
  enabled: boolean;
  issuerUrl?: string;
  clientId?: string;
  clientSecret?: string;
  metadataUrl?: string;
  entityId?: string;
  acsUrl?: string;
  sloUrl?: string;
  certificate?: string;
  scopes?: string[];
  attributeMapping?: Record<string, string>;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateSsoConfigRequest {
  providerType: SsoProviderType;
  enabled: boolean;
  issuerUrl?: string;
  clientId?: string;
  clientSecret?: string;
  metadataUrl?: string;
  entityId?: string;
  certificate?: string;
  scopes?: string[];
  attributeMapping?: Record<string, string>;
}

export function useSsoConfig() {
  return useQuery({
    queryKey: ['sso-config'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<SsoConfig>>('/sso/config');
      return res.data.data;
    },
  });
}

export function useUpdateSsoConfig() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: UpdateSsoConfigRequest) => {
      const res = await apiClient.put<ApiResponse<SsoConfig>>('/sso/config', data);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sso-config'] });
    },
  });
}
