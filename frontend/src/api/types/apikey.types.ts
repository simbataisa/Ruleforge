export interface ApiKey {
  id: string;
  name: string;
  prefix: string;
  permissions: string[];
  expiresAt?: string;
  lastUsedAt?: string;
  createdAt: string;
}

export interface CreateApiKeyRequest {
  name: string;
  permissions: string[];
  expiresInDays?: number;
}

export interface CreateApiKeyResponse {
  id: string;
  name: string;
  key: string;
  prefix: string;
  permissions: string[];
  expiresAt?: string;
}
