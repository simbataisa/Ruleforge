import { useState, useMemo } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import {
  ArrowLeft, Settings, BookOpen, Calendar, ChevronRight,
  Plus, Search, MoreVertical, Pencil, Copy, ArrowRightLeft, Trash2,
  RefreshCw, X, FlaskConical, Shield,
} from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { Select } from '@/shared/components/Select';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';
import { useSpace } from '@/api/org.api';
import { useSpaceRules, useCreateSpaceRule, useDeleteRule } from '@/api/rules.api';
import { useFolders } from '@/api/folders.api';
import { MoveRuleModal } from '@/features/spaces/components/MoveRuleModal';
import { CopyRuleModal } from '@/features/spaces/components/CopyRuleModal';
import { CreateRuleModal } from '@/features/rules/components/CreateRuleModal';
import { useToastStore } from '@/stores/toast.store';
import type { Rule, RuleType } from '@/api/types/rule.types';

const RULE_TYPES = ['TABLE', 'TREE', 'SCORECARD', 'FLOW', 'SCRIPT', 'DMN', 'CEP', 'ENSEMBLE'];
const RULE_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED', 'PENDING_APPROVAL'];

function getRuleTypeBadgeClass(type: string): string {
  switch (type) {
    case 'TABLE': return 'bg-blue-100 text-blue-700';
    case 'TREE': return 'bg-purple-100 text-purple-700';
    case 'SCORECARD': return 'bg-orange-100 text-orange-700';
    case 'FLOW': return 'bg-teal-100 text-teal-700';
    case 'SCRIPT': return 'bg-yellow-100 text-yellow-700';
    case 'DMN': return 'bg-indigo-100 text-indigo-700';
    case 'CEP': return 'bg-rose-100 text-rose-700';
    case 'ENSEMBLE': return 'bg-cyan-100 text-cyan-700';
    default: return 'bg-gray-100 text-gray-700';
  }
}

function getStatusBadgeClass(status: string): string {
  switch (status) {
    case 'PUBLISHED': return 'bg-green-100 text-green-700';
    case 'DRAFT': return 'bg-gray-100 text-gray-600';
    case 'ARCHIVED': return 'bg-red-100 text-red-600';
    case 'PENDING_APPROVAL': return 'bg-amber-100 text-amber-700';
    default: return 'bg-gray-100 text-gray-600';
  }
}

function formatRelative(dateStr: string): string {
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  const diff = now - then;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 30) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString();
}

export function SpaceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);

  const { data: space, isLoading, isError } = useSpace(id ?? '');
  const { data: rulesPage, isLoading: rulesLoading, isError: rulesError, refetch: refetchRules } =
    useSpaceRules(id ?? '');
  const { data: foldersData } = useFolders();
  const createSpaceRule = useCreateSpaceRule(id ?? '');
  const deleteRule = useDeleteRule();
  const folders = useMemo(() => (Array.isArray(foldersData) ? foldersData : []), [foldersData]);

  // Search / filter state
  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [actionMenuRuleId, setActionMenuRuleId] = useState<string | null>(null);

  // Modal state
  const [showCreateRule, setShowCreateRule] = useState(false);
  const [moveRule, setMoveRule] = useState<Rule | null>(null);
  const [copyRule, setCopyRule] = useState<Rule | null>(null);
  const [deleteConfirmRule, setDeleteConfirmRule] = useState<Rule | null>(null);

  const allRules: Rule[] = useMemo(() => rulesPage?.content ?? [], [rulesPage]);

  const filteredRules = useMemo(() => {
    return allRules.filter((r) => {
      if (search && !r.name.toLowerCase().includes(search.toLowerCase())) return false;
      if (filterType && r.type !== filterType) return false;
      if (filterStatus && r.status !== filterStatus) return false;
      return true;
    });
  }, [allRules, search, filterType, filterStatus]);

  const hasFilters = !!search || !!filterType || !!filterStatus;
  const clearFilters = () => { setSearch(''); setFilterType(''); setFilterStatus(''); };

  const handleCreateRule = (data: { name: string; type: RuleType; description?: string; alias?: string; folderId?: string }) => {
    createSpaceRule.mutate(data, {
      onSuccess: (response) => {
        setShowCreateRule(false);
        addToast({ type: 'success', title: 'Rule created successfully' });
        if (response?.id) navigate(`/rules/${response.id}/edit`);
      },
      onError: () => addToast({ type: 'error', title: 'Failed to create rule' }),
    });
  };

  const handleDeleteRule = () => {
    if (!deleteConfirmRule) return;
    deleteRule.mutate(deleteConfirmRule.id, {
      onSuccess: () => {
        addToast({ type: 'success', title: 'Rule deleted' });
        setDeleteConfirmRule(null);
        refetchRules();
      },
      onError: () => addToast({ type: 'error', title: 'Failed to delete rule' }),
    });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !space) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <p className="text-gray-500">Space not found or you don't have access.</p>
        <Button variant="outline" onClick={() => navigate('/spaces')}>
          Back to Spaces
        </Button>
      </div>
    );
  }

  const createdDate = new Date(space.createdAt).toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  });

  return (
    <div className="flex flex-col h-full">
      {/* Breadcrumb */}
      <div className="flex items-center gap-1.5 px-6 pt-4 text-sm text-gray-500">
        <Link to="/spaces" className="hover:text-gray-700 transition-colors">
          Spaces
        </Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="text-gray-900 font-medium">{space.name}</span>
      </div>

      {/* Header */}
      <div className="flex items-start justify-between px-6 py-4 border-b border-gray-200">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/spaces')}
            className="p-1.5 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
            aria-label="Back to spaces"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <h1 className="text-xl font-semibold text-gray-900">{space.name}</h1>
            <p className="text-sm text-gray-400 font-mono">{space.slug}</p>
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={() => navigate(`/spaces/${id}/settings`)}>
          <Settings className="h-4 w-4" />
          Settings
        </Button>
      </div>

      {/* Space Context Banner */}
      {space.name === 'Sandbox' ? (
        <div
          role="status"
          className="flex items-center gap-3 px-6 py-3 bg-amber-50 border-b border-amber-200 text-amber-800 text-sm"
        >
          <FlaskConical className="h-4 w-4 flex-shrink-0" aria-hidden="true" />
          <span>
            This is the Sandbox space. Any rules created here will be visible to all spaces.
          </span>
        </div>
      ) : (
        <div
          role="status"
          className="flex items-center gap-3 px-6 py-3 bg-blue-50 border-b border-blue-200 text-blue-800 text-sm"
        >
          <Shield className="h-4 w-4 flex-shrink-0" aria-hidden="true" />
          <span>
            This is a production space. Any rules created here will be visible to all spaces.
          </span>
        </div>
      )}

      {/* Stats Bar */}
      <div className="flex items-center gap-6 px-6 py-3 bg-gray-50 border-b border-gray-200 text-sm">
        <div className="flex items-center gap-2 text-gray-600">
          <BookOpen className="h-4 w-4 text-gray-400" />
          <span>
            <span className="font-semibold text-gray-900">{space.ruleCount}</span>{' '}
            rule{space.ruleCount !== 1 ? 's' : ''}
          </span>
        </div>
        <div className="flex items-center gap-2 text-gray-600">
          <Calendar className="h-4 w-4 text-gray-400" />
          <span>Created {createdDate}</span>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {space.description && (
          <p className="text-sm text-gray-600 mb-6 max-w-2xl">{space.description}</p>
        )}

        {/* Rules Section */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Rules</h2>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => refetchRules()}
                disabled={rulesLoading}
                aria-label="Refresh rules"
              >
                <RefreshCw className={`h-4 w-4 ${rulesLoading ? 'animate-spin' : ''}`} />
              </Button>
              <Button size="sm" onClick={() => setShowCreateRule(true)}>
                <Plus className="h-4 w-4" />
                Create Rule
              </Button>
            </div>
          </div>

          {/* Search + Filters */}
          <div className="flex items-center gap-3 mb-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search rules by name..."
                className="w-full pl-9 pr-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                aria-label="Search rules"
              />
              {search && (
                <button
                  onClick={() => setSearch('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  aria-label="Clear search"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              )}
            </div>
            <Select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              options={[{ value: '', label: 'All Types' }, ...RULE_TYPES.map((t) => ({ value: t, label: t }))]}
              className="w-36"
              aria-label="Filter by type"
            />
            <Select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              options={[{ value: '', label: 'All Statuses' }, ...RULE_STATUSES.map((s) => ({ value: s, label: s }))]}
              className="w-40"
              aria-label="Filter by status"
            />
            {hasFilters && (
              <button
                onClick={clearFilters}
                className="text-sm text-gray-500 hover:text-gray-700 flex items-center gap-1"
              >
                <X className="h-3.5 w-3.5" />
                Clear filters
              </button>
            )}
          </div>

          {/* Rules List */}
          {rulesLoading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-14 bg-gray-100 rounded-md animate-pulse" />
              ))}
            </div>
          ) : rulesError ? (
            <div className="flex flex-col items-center justify-center py-12 gap-3 text-gray-500">
              <p>Failed to load rules.</p>
              <Button variant="outline" size="sm" onClick={() => refetchRules()}>
                Retry
              </Button>
            </div>
          ) : filteredRules.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 gap-4 text-center">
              {hasFilters ? (
                <>
                  <p className="text-gray-500">No rules match your filters.</p>
                  <button onClick={clearFilters} className="text-sm text-blue-600 hover:text-blue-700 underline">
                    Clear filters
                  </button>
                </>
              ) : (
                <>
                  <BookOpen className="h-12 w-12 text-gray-300" />
                  <div>
                    <p className="text-gray-600 font-medium">No rules in this space yet</p>
                    <p className="text-sm text-gray-400 mt-1">Create your first rule to get started.</p>
                  </div>
                  <Button onClick={() => setShowCreateRule(true)}>
                    <Plus className="h-4 w-4" />
                    Create Rule
                  </Button>
                </>
              )}
            </div>
          ) : (
            <>
              <div className="rounded-lg border border-gray-200 overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      <th className="text-left px-4 py-3 font-medium text-gray-500 w-[40%]">Name</th>
                      <th className="text-left px-4 py-3 font-medium text-gray-500 w-[14%]">Type</th>
                      <th className="text-left px-4 py-3 font-medium text-gray-500 w-[16%]">Status</th>
                      <th className="text-left px-4 py-3 font-medium text-gray-500 w-[10%]">Version</th>
                      <th className="text-left px-4 py-3 font-medium text-gray-500 w-[12%]">Updated</th>
                      <th className="text-right px-4 py-3 font-medium text-gray-500 w-[8%]">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {filteredRules.map((rule) => (
                      <tr
                        key={rule.id}
                        className="hover:bg-gray-50 transition-colors cursor-pointer"
                        onClick={() => navigate(`/rules/${rule.id}/edit`)}
                      >
                        <td className="px-4 py-3">
                          <div className="font-medium text-gray-900 truncate max-w-xs">{rule.name}</div>
                          {rule.description && (
                            <div className="text-xs text-gray-400 truncate max-w-xs mt-0.5">
                              {rule.description}
                            </div>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${getRuleTypeBadgeClass(rule.type)}`}>
                            {rule.type}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${getStatusBadgeClass(rule.status)}`}>
                            {rule.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-gray-500">
                          v{rule.latestVersion}
                        </td>
                        <td className="px-4 py-3 text-gray-400 text-xs">
                          {formatRelative(rule.updatedAt)}
                        </td>
                        <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                          <div className="relative inline-block">
                            <button
                              onClick={() => setActionMenuRuleId(actionMenuRuleId === rule.id ? null : rule.id)}
                              className="rounded p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
                              aria-label={`Actions for ${rule.name}`}
                            >
                              <MoreVertical className="h-4 w-4" />
                            </button>
                            {actionMenuRuleId === rule.id && (
                              <>
                                <div className="fixed inset-0 z-40" onClick={() => setActionMenuRuleId(null)} />
                                <div className="absolute right-0 top-8 z-50 w-40 rounded-lg border border-gray-200 bg-white py-1 shadow-lg">
                                  <button
                                    className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                    onClick={() => { setActionMenuRuleId(null); navigate(`/rules/${rule.id}/edit`); }}
                                  >
                                    <Pencil className="h-4 w-4 text-gray-400" />
                                    Edit
                                  </button>
                                  <button
                                    className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                    onClick={() => { setActionMenuRuleId(null); setCopyRule(rule); }}
                                  >
                                    <Copy className="h-4 w-4 text-gray-400" />
                                    Copy
                                  </button>
                                  <button
                                    className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                    onClick={() => { setActionMenuRuleId(null); setMoveRule(rule); }}
                                  >
                                    <ArrowRightLeft className="h-4 w-4 text-gray-400" />
                                    Move
                                  </button>
                                  <div className="border-t border-gray-100 my-1" />
                                  <button
                                    className="flex w-full items-center gap-2 px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                                    onClick={() => { setActionMenuRuleId(null); setDeleteConfirmRule(rule); }}
                                  >
                                    <Trash2 className="h-4 w-4" />
                                    Delete
                                  </button>
                                </div>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <p className="mt-3 text-xs text-gray-400">
                {hasFilters
                  ? `Showing ${filteredRules.length} of ${allRules.length} rules`
                  : `Showing ${filteredRules.length} rule${filteredRules.length !== 1 ? 's' : ''}`}
              </p>
            </>
          )}
        </div>
      </div>

      {/* Modals */}
      {showCreateRule && (
        <CreateRuleModal
          open={showCreateRule}
          onClose={() => setShowCreateRule(false)}
          onSubmit={handleCreateRule}
          loading={createSpaceRule.isPending}
          folders={folders}
        />
      )}

      {moveRule && id && (
        <MoveRuleModal
          ruleId={moveRule.id}
          ruleName={moveRule.name}
          currentSpaceId={id}
          onClose={() => setMoveRule(null)}
          onSuccess={() => { setMoveRule(null); refetchRules(); }}
        />
      )}

      {copyRule && id && (
        <CopyRuleModal
          ruleId={copyRule.id}
          ruleName={copyRule.name}
          currentSpaceId={id}
          onClose={() => setCopyRule(null)}
          onSuccess={() => setCopyRule(null)}
        />
      )}

      {deleteConfirmRule && (
        <ConfirmDialog
          open
          title="Delete Rule"
          message={`Are you sure you want to delete "${deleteConfirmRule.name}"? This action cannot be undone.`}
          confirmLabel="Delete"
          confirmVariant="danger"
          onConfirm={handleDeleteRule}
          onClose={() => setDeleteConfirmRule(null)}
          isLoading={deleteRule.isPending}
        />
      )}
    </div>
  );
}
