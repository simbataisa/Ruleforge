import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { FlaskConical, Plus } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
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

type StatusTab = 'ALL' | 'ACTIVE' | 'SCHEDULED' | 'COMPLETED';

const STATUS_TABS: StatusTab[] = ['ALL', 'ACTIVE', 'SCHEDULED', 'COMPLETED'];

function matchesTab(status: string, tab: StatusTab): boolean {
  if (tab === 'ALL') return true;
  const upper = status.toUpperCase();
  if (tab === 'ACTIVE') return upper === 'ACTIVE' || upper === 'RUNNING';
  if (tab === 'SCHEDULED') return upper === 'SCHEDULED' || upper === 'PENDING';
  if (tab === 'COMPLETED')
    return upper === 'COMPLETED' || upper === 'STOPPED' || upper === 'CANCELLED';
  return false;
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
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const queryClient = useQueryClient();
  const { data: tests, isLoading } = useAbTests();
  const stopTest = useStopAbTest();
  const [showCreate, setShowCreate] = useState(false);
  const [selectedRuleId, setSelectedRuleId] = useState<string>('');
  const [activeTab, setActiveTab] = useState<StatusTab>('ALL');
  const { data: rulesData } = useRules({ size: 100 });
  const { data: versionsData } = useRuleVersions(selectedRuleId);
  const rules = rulesData?.content ?? [];
  const versions = Array.isArray(versionsData) ? versionsData : [];

  const allTests = tests ?? [];

  const tabCounts = useMemo(() => {
    const counts: Record<StatusTab, number> = { ALL: allTests.length, ACTIVE: 0, SCHEDULED: 0, COMPLETED: 0 };
    for (const t of allTests) {
      for (const tab of STATUS_TABS) {
        if (tab !== 'ALL' && matchesTab(t.status, tab)) counts[tab]++;
      }
    }
    return counts;
  }, [allTests]);

  const filteredTests = useMemo(
    () => allTests.filter((t) => matchesTab(t.status, activeTab)),
    [allTests, activeTab],
  );

  const handleStop = async (testId: string) => {
    try {
      await stopTest.mutateAsync(testId);
      addToast({ type: 'success', title: 'A/B test stopped' });
    } catch {
      addToast({ type: 'error', title: 'Failed to stop A/B test' });
    }
  };

  const handleViewResults = (testId: string) => {
    navigate(`/ab-tests/${testId}`);
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

      {/* Status tabs */}
      <div className="mb-4 border-b border-gray-200">
        <nav className="flex gap-4">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex items-center gap-2 border-b-2 pb-3 text-sm font-medium transition-colors ${
                activeTab === tab
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
              }`}
            >
              {tab === 'ALL' ? 'All' : tab.charAt(0) + tab.slice(1).toLowerCase()}
              <Badge
                color={activeTab === tab ? 'blue' : 'gray'}
                className="text-xs"
              >
                {tabCounts[tab]}
              </Badge>
            </button>
          ))}
        </nav>
      </div>

      {filteredTests.length === 0 ? (
        <EmptyState
          icon={<FlaskConical className="h-12 w-12" />}
          title={activeTab === 'ALL' ? 'No A/B tests' : `No ${activeTab.toLowerCase()} tests`}
          description={
            activeTab === 'ALL'
              ? 'Create an A/B test from the rule editor to compare rule versions.'
              : `No tests with ${activeTab.toLowerCase()} status.`
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filteredTests.map((test) => (
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
