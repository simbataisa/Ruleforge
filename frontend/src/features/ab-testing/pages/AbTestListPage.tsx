import { useState } from 'react';
import { FlaskConical, Plus } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { EmptyState } from '@/shared/components/EmptyState';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import { apiClient } from '@/api/client';
import { useRules, useRuleVersions } from '@/api/rules.api';
import type { ApiResponse } from '@/api/types/common.types';
import { AbTestCard } from '../components/AbTestCard';
import { CreateAbTestModal } from '../components/CreateAbTestModal';

interface AbTest {
  id: string;
  name: string;
  ruleId: string;
  versionA: number;
  versionB: number;
  trafficSplit: number;
  status: string;
  startDate?: string;
  endDate?: string;
}

function useAbTests() {
  return useQuery({
    queryKey: ['ab-tests'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<AbTest[]>>('/rules/ab-tests');
      return res.data.data;
    },
  });
}

function useStopAbTest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (testId: string) => {
      await apiClient.post(`/rules/ab-tests/${testId}/stop`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ab-tests'] });
    },
  });
}

export function AbTestListPage() {
  const addToast = useToastStore((s) => s.addToast);
  const queryClient = useQueryClient();
  const { data: tests, isLoading } = useAbTests();
  const stopTest = useStopAbTest();
  const [, setSelectedId] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [selectedRuleId, setSelectedRuleId] = useState<string>('');
  const { data: rulesData } = useRules({ size: 100 });
  const { data: versionsData } = useRuleVersions(selectedRuleId);
  const rules = rulesData?.content ?? [];
  const versions = Array.isArray(versionsData) ? versionsData : [];

  const handleStop = async (testId: string) => {
    try {
      await stopTest.mutateAsync(testId);
      addToast({ type: 'success', title: 'A/B test stopped' });
    } catch {
      addToast({ type: 'error', title: 'Failed to stop A/B test' });
    }
  };

  const handleViewResults = (testId: string) => {
    setSelectedId(testId);
    // Could navigate to detail page or show modal
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="A/B Tests"
        description="Compare rule versions with traffic splitting"
        actions={
          <div className="flex items-center gap-2">
            <select
              value={selectedRuleId}
              onChange={(e) => setSelectedRuleId(e.target.value)}
              className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
            >
              <option value="">Select a rule...</option>
              {rules.map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
            <Button
              onClick={() => setShowCreate(true)}
              disabled={!selectedRuleId || versions.length === 0}
            >
              <Plus className="h-4 w-4" />
              Create A/B Test
            </Button>
          </div>
        }
      />

      {!tests || tests.length === 0 ? (
        <EmptyState
          icon={<FlaskConical className="h-12 w-12" />}
          title="No A/B tests"
          description="Create an A/B test from the rule editor to compare rule versions."
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {tests.map((test) => (
            <AbTestCard
              key={test.id}
              test={test}
              onStop={handleStop}
              onViewResults={handleViewResults}
            />
          ))}
        </div>
      )}

      <CreateAbTestModal
        ruleId={selectedRuleId}
        versions={versions}
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        onCreated={() => {
          queryClient.invalidateQueries({ queryKey: ['ab-tests'] });
          addToast({ type: 'success', title: 'A/B test created' });
        }}
      />
    </div>
  );
}
