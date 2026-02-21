import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from './client';
import type { ApiResponse } from './types/common.types';

export interface AiMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

export interface AiConversation {
  id: string;
  title: string;
  messages: AiMessage[];
  createdAt: string;
  updatedAt: string;
}

export interface AiSuggestion {
  type: 'OPTIMIZATION' | 'ERROR_FIX' | 'SIMPLIFICATION' | 'COVERAGE';
  description: string;
  suggestedChanges?: string;
  confidence: number;
}

export interface AiChatRequest {
  conversationId?: string;
  message: string;
  ruleId?: string;
}

export interface AiChatResponse {
  conversationId: string;
  message: AiMessage;
}

export function useAiConversations() {
  return useQuery({
    queryKey: ['ai-conversations'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<AiConversation[]>>('/ai/conversations');
      return res.data.data;
    },
  });
}

export function useAiConversation(id: string) {
  return useQuery({
    queryKey: ['ai-conversations', id],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<AiConversation>>(`/ai/conversations/${id}`);
      return res.data.data;
    },
    enabled: !!id,
  });
}

export function useAiChat() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: AiChatRequest) => {
      const res = await apiClient.post<ApiResponse<AiChatResponse>>('/ai/chat', data);
      return res.data.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['ai-conversations'] });
      queryClient.invalidateQueries({
        queryKey: ['ai-conversations', data.conversationId],
      });
    },
  });
}

export function useAiAnalyzeRule() {
  return useMutation({
    mutationFn: async (ruleId: string) => {
      const res = await apiClient.post<ApiResponse<AiSuggestion[]>>(
        `/ai/analyze/${ruleId}`,
      );
      return res.data.data;
    },
  });
}

export function useAiGenerateRule() {
  return useMutation({
    mutationFn: async (description: string) => {
      const res = await apiClient.post<ApiResponse<{ ruleDefinition: string }>>(
        '/ai/generate',
        { description },
      );
      return res.data.data;
    },
  });
}

export function useAiExplainRule() {
  return useMutation({
    mutationFn: async (ruleId: string) => {
      const res = await apiClient.post<ApiResponse<{ explanation: string }>>(
        `/ai/explain/${ruleId}`,
      );
      return res.data.data;
    },
  });
}
