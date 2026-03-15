import { useState, useMemo, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import {
  Plus, Search, RefreshCw, ChevronLeft, ChevronRight, Upload,
  LayoutList, LayoutGrid, MoreVertical, Pencil, Play, SendHorizontal,
  Copy, Trash2, Lock, X, FolderOpen, Layers,
} from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { EmptyState } from '@/shared/components/EmptyState';
import { SkeletonRow } from '@/components/Skeleton';
import { useRules, useSpaceRules, useCreateRule, useCreateSpaceRule, useCopyRule, useTags, useDeleteRule } from '@/api/rules.api';
import { useSpace } from '@/api/org.api';
import { useFolders, useCreateFolder } from '@/api/folders.api';
import { useToastStore } from '@/stores/toast.store';
import { useAuthStore } from '@/stores/auth.store';
import { RulesExplorer } from '@/features/rules/components/RulesExplorer';
import { CreateRuleModal } from '@/features/rules/components/CreateRuleModal';
import { CreateFolderModal } from '@/features/rules/components/CreateFolderModal';
import { ImportRuleModal } from '@/features/rules/components/ImportRuleModal';
import { ExportRuleButton } from '@/features/rules/components/ExportRuleButton';
import { RequestApprovalModal } from '@/features/rules/components/RequestApprovalModal';
import { AssignToSpaceModal } from '@/features/rules/components/AssignToSpaceModal';
import { FilterChips } from '@/features/rules/components/FilterChips';
import { TagBadge } from '@/features/rules/components/TagBadge';
import { RuleTypeIcon } from '@/features/rules/components/RuleTypeIcon';
import { StatusBadge } from '@/features/rules/components/StatusBadge';
import { RuleCard } from '@/features/rules/components/RuleCard';
import { BatchActionBar } from '@/features/rules/components/BatchActionBar';
import { formatRelativeTime } from '@/features/rules/utils/formatRelativeTime';
import { cn } from '@/shared/utils/cn';
import type { Rule, RuleType } from '@/api/types/rule.types';

type ViewMode = 'table' | 'card';
type SortOption = 'updatedAt' | 'createdAt' | 'name';

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

function getDateFromPreset(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString();
}

export function RuleListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const addToast = useToastStore((s) => s.addToast);
  const user = useAuthStore((s) => s.user);

  // Space context from URL query param
  const spaceIdFromUrl = searchParams.get('spaceId') ?? undefined;
  const { data: spaceContext } = useSpace(spaceIdFromUrl ?? '');

  const clearSpaceFilter = useCallback(() => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.delete('spaceId');
      return next;
    });
  }, [setSearchParams]);

  // Search state
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // Filter state
  const [selectedTypes, setSelectedTypes] = useState<string[]>([]);
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>([]);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);
  const [datePreset, setDatePreset] = useState<number | null>(null);

  // View & pagination
  const [viewMode, setViewMode] = useState<ViewMode>(() => {
    return (localStorage.getItem('ruleforge:rules-view-mode') as ViewMode) || 'table';
  });
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [sortBy, setSortBy] = useState<SortOption>('updatedAt');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  // Selection
  const [selectedRuleIds, setSelectedRuleIds] = useState<Set<string>>(new Set());

  // Modals
  const [showCreateRule, setShowCreateRule] = useState(false);
  const [showCreateFolder, setShowCreateFolder] = useState(false);
  const [showImportRule, setShowImportRule] = useState(false);
  const [createFolderParentId, setCreateFolderParentId] = useState<string | undefined>();
  const [actionMenuRuleId, setActionMenuRuleId] = useState<string | null>(null);
  const [approvalModalRules, setApprovalModalRules] = useState<Rule[]>([]);
  const [assignToSpaceRules, setAssignToSpaceRules] = useState<Rule[]>([]);

  // Persist view mode
  useEffect(() => {
    localStorage.setItem('ruleforge:rules-view-mode', viewMode);
  }, [viewMode]);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 300);
    return () => clearTimeout(timer);
  }, [search]);

  // Reset page on filter change
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, selectedTypes, selectedStatuses, selectedTags, selectedFolderId, datePreset]);

  // Clear selection on filter/page change
  useEffect(() => {
    setSelectedRuleIds(new Set());
  }, [page, debouncedSearch, selectedTypes, selectedStatuses, selectedTags, selectedFolderId, datePreset]);

  // Build query params
  const queryParams = useMemo(() => {
    const params: Record<string, unknown> = {
      page,
      size: pageSize,
      sort: `${sortBy},${sortDir}`,
    };
    if (debouncedSearch) params.search = debouncedSearch;
    if (selectedTypes.length > 0) params.types = selectedTypes;
    if (selectedStatuses.length > 0) params.statuses = selectedStatuses;
    if (selectedTags.length > 0) params.tags = selectedTags;
    if (selectedFolderId) params.folderId = selectedFolderId;
    if (datePreset != null) params.updatedAfter = getDateFromPreset(datePreset);
    return params;
  }, [page, pageSize, sortBy, sortDir, debouncedSearch, selectedTypes, selectedStatuses, selectedTags, selectedFolderId, datePreset]);

  const orgRulesQuery = useRules(spaceIdFromUrl ? undefined : queryParams as any);
  const spaceRulesQuery = useSpaceRules(spaceIdFromUrl ?? '', spaceIdFromUrl ? { page: queryParams.page as number, size: queryParams.size as number } : undefined);
  const rulesData = spaceIdFromUrl ? spaceRulesQuery.data : orgRulesQuery.data;
  const rulesLoading = spaceIdFromUrl ? spaceRulesQuery.isLoading : orgRulesQuery.isLoading;
  const rulesFetching = spaceIdFromUrl ? spaceRulesQuery.isFetching : orgRulesQuery.isFetching;
  const refetchRules = spaceIdFromUrl ? spaceRulesQuery.refetch : orgRulesQuery.refetch;
  const { data: foldersData, isLoading: foldersLoading, refetch: refetchFolders } = useFolders();
  const { data: tagsData } = useTags();

  const createRule = useCreateRule();
  const createSpaceRule = useCreateSpaceRule(spaceIdFromUrl ?? '');
  const copyRule = useCopyRule();
  const createFolder = useCreateFolder();
  const deleteRule = useDeleteRule();

  const rules: Rule[] = useMemo(() => {
    const allRules = rulesData?.content ?? [];
    return Array.isArray(allRules) ? allRules : [];
  }, [rulesData]);

  const totalPages = rulesData?.totalPages ?? 0;
  const totalElements = rulesData?.totalElements ?? 0;
  const tags = useMemo(() => tagsData ?? [], [tagsData]);
  const folders = useMemo(() => (Array.isArray(foldersData) ? foldersData : []), [foldersData]);

  // Check if all selected rules are DRAFT
  const allSelectedDraft = useMemo(() => {
    if (selectedRuleIds.size === 0) return false;
    return rules
      .filter((r) => selectedRuleIds.has(r.id))
      .every((r) => r.status === 'DRAFT');
  }, [rules, selectedRuleIds]);

  // Build filter chips
  const filterChips = useMemo(() => {
    const chips: { key: string; label: string; value: string }[] = [];
    selectedTypes.forEach((t) => chips.push({ key: 'type', label: 'Type', value: t }));
    selectedStatuses.forEach((s) => chips.push({ key: 'status', label: 'Status', value: s }));
    selectedTags.forEach((t) => chips.push({ key: 'tag', label: 'Tag', value: t }));
    if (datePreset != null) {
      const label = datePreset === 0 ? 'Today' : datePreset === 7 ? 'Last 7 days' : `Last ${datePreset} days`;
      chips.push({ key: 'date', label: 'Updated', value: label });
    }
    return chips;
  }, [selectedTypes, selectedStatuses, selectedTags, datePreset]);

  const handleRemoveChip = useCallback((key: string, value: string) => {
    switch (key) {
      case 'type':
        setSelectedTypes((prev) => prev.filter((t) => t !== value));
        break;
      case 'status':
        setSelectedStatuses((prev) => prev.filter((s) => s !== value));
        break;
      case 'tag':
        setSelectedTags((prev) => prev.filter((t) => t !== value));
        break;
      case 'date':
        setDatePreset(null);
        break;
    }
  }, []);

  const handleClearAllFilters = useCallback(() => {
    setSelectedTypes([]);
    setSelectedStatuses([]);
    setSelectedTags([]);
    setDatePreset(null);
    setSearch('');
    setSelectedFolderId(null);
  }, []);

  // Selection handlers
  const toggleSelect = useCallback((ruleId: string) => {
    setSelectedRuleIds((prev) => {
      const next = new Set(prev);
      if (next.has(ruleId)) next.delete(ruleId);
      else next.add(ruleId);
      return next;
    });
  }, []);

  const toggleSelectAll = useCallback(() => {
    if (selectedRuleIds.size === rules.length) {
      setSelectedRuleIds(new Set());
    } else {
      setSelectedRuleIds(new Set(rules.map((r) => r.id)));
    }
  }, [rules, selectedRuleIds.size]);

  const handleSort = useCallback((key: string) => {
    setSortBy((prev) => {
      if (prev === key) {
        setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
        return prev;
      }
      setSortDir('desc');
      return key as SortOption;
    });
  }, []);

  // Action handlers
  const handleRefresh = () => {
    refetchRules();
    refetchFolders();
  };

  const handleCreateRule = (data: { name: string; type: RuleType; description?: string; alias?: string; folderId?: string }) => {
    const mutator = spaceIdFromUrl ? createSpaceRule : createRule;
    mutator.mutate(data, {
      onSuccess: (response) => {
        setShowCreateRule(false);
        addToast({ type: 'success', title: 'Rule created successfully' });
        if (response?.id) navigate(`/rules/${response.id}/edit`);
      },
      onError: () => addToast({ type: 'error', title: 'Failed to create rule' }),
    });
  };

  const handleCreateFolder = (data: { name: string; parentId?: string }) => {
    createFolder.mutate(data, {
      onSuccess: () => {
        setShowCreateFolder(false);
        addToast({ type: 'success', title: 'Folder created successfully' });
      },
      onError: () => addToast({ type: 'error', title: 'Failed to create folder' }),
    });
  };

  const handleCopyRule = (rule: Rule) => {
    copyRule.mutate(
      { ruleId: rule.id, name: `${rule.name} (Copy)` },
      {
        onSuccess: (response) => {
          addToast({ type: 'success', title: `Rule copied as "${response?.name ?? 'Copy'}"` });
          if (response?.id) navigate(`/rules/${response.id}/edit`);
        },
        onError: () => addToast({ type: 'error', title: 'Failed to copy rule' }),
      },
    );
  };

  const handleDeleteRule = (rule: Rule) => {
    if (!confirm(`Are you sure you want to delete "${rule.name}"? This action cannot be undone.`)) return;
    deleteRule.mutate(rule.id, {
      onSuccess: () => addToast({ type: 'success', title: 'Rule deleted' }),
      onError: () => addToast({ type: 'error', title: 'Failed to delete rule' }),
    });
  };

  const handleBatchRequestApproval = () => {
    const selected = rules.filter((r) => selectedRuleIds.has(r.id));
    setApprovalModalRules(selected);
  };

  const handleBatchExport = () => {
    addToast({ type: 'info', title: `Exporting ${selectedRuleIds.size} rules (coming soon)` });
  };

  // Action menu
  const ActionMenu = ({ rule }: { rule: Rule }) => {
    const isOpen = actionMenuRuleId === rule.id;

    return (
      <div className="relative">
        <button
          onClick={(e) => {
            e.stopPropagation();
            setActionMenuRuleId(isOpen ? null : rule.id);
          }}
          className="rounded p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
        >
          <MoreVertical className="h-4 w-4" />
        </button>
        {isOpen && (
          <>
            <div className="fixed inset-0 z-40" onClick={() => setActionMenuRuleId(null)} />
            <div className="absolute right-0 top-8 z-50 w-48 rounded-lg border border-gray-200 bg-white py-1 shadow-lg">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setActionMenuRuleId(null);
                  navigate(`/rules/${rule.id}/edit`);
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                <Pencil className="h-3.5 w-3.5" /> Edit
              </button>
              {(rule.status === 'DRAFT' || rule.status === 'PUBLISHED') && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setActionMenuRuleId(null);
                    navigate(`/rules/${rule.id}/edit?tab=test`);
                  }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                >
                  <Play className="h-3.5 w-3.5" /> Test
                </button>
              )}
              {rule.status === 'DRAFT' && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setActionMenuRuleId(null);
                    setApprovalModalRules([rule]);
                  }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                >
                  <SendHorizontal className="h-3.5 w-3.5" /> Request Approval
                </button>
              )}
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setActionMenuRuleId(null);
                  handleCopyRule(rule);
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                <Copy className="h-3.5 w-3.5" /> Duplicate
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setActionMenuRuleId(null);
                  setAssignToSpaceRules([rule]);
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                <Layers className="h-3.5 w-3.5" /> Assign to Space
              </button>
              <div className="px-3 py-1">
                <ExportRuleButton ruleId={rule.id} ruleName={rule.name} variant="menu" />
              </div>
              <div className="my-1 border-t border-gray-100" />
              {(rule.status === 'DRAFT' || rule.status === 'ARCHIVED') && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setActionMenuRuleId(null);
                    handleDeleteRule(rule);
                  }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                >
                  <Trash2 className="h-3.5 w-3.5" /> Delete
                </button>
              )}
            </div>
          </>
        )}
      </div>
    );
  };

  // Loading state
  if (rulesLoading || foldersLoading) {
    return (
      <div className="flex flex-col h-full">
        <PageHeader title="Rules" />
        <div className="flex flex-1 overflow-hidden">
          <div className="w-[260px] flex-shrink-0 border-r border-gray-200 bg-[#f8f8f8] p-3">
            <div className="space-y-2">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="h-[22px] animate-pulse rounded bg-gray-200" style={{ width: `${50 + i * 8}%` }} />
              ))}
            </div>
          </div>
          <div className="flex-1 p-6">
            <div className="mb-4 h-10 w-full animate-pulse rounded-lg bg-gray-100" />
            <div className="rounded-lg border border-gray-200 bg-white">
              <div className="flex items-center gap-4 px-4 py-3 border-b border-gray-200 bg-gray-50">
                {['40%', '12%', '12%', '10%', '15%'].map((w, i) => (
                  <div key={i} className="h-3 animate-pulse rounded bg-gray-200" style={{ width: w }} />
                ))}
              </div>
              {Array.from({ length: 8 }).map((_, i) => (
                <SkeletonRow key={i} columns={6} />
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  const hasActiveFilters = filterChips.length > 0 || !!debouncedSearch || !!selectedFolderId;

  return (
    <div className="flex flex-col h-full">
      <PageHeader
        title="Rules"
        actions={
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={handleRefresh} disabled={rulesFetching}>
              <RefreshCw className={`h-4 w-4 ${rulesFetching ? 'animate-spin' : ''}`} />
            </Button>
            <Button variant="outline" size="sm" onClick={() => setShowImportRule(true)}>
              <Upload className="h-4 w-4" />
              Import
            </Button>
            <Button size="sm" onClick={() => setShowCreateRule(true)}>
              <Plus className="h-4 w-4" />
              Create Rule
            </Button>
          </div>
        }
      />

      {/* Space context banner */}
      {spaceIdFromUrl && (
        <div className="flex items-center gap-3 px-6 py-2 bg-blue-50 border-b border-blue-200 text-sm">
          <FolderOpen className="h-4 w-4 text-blue-600 shrink-0" />
          <span className="text-blue-800">
            Viewing rules in:{' '}
            <span className="font-semibold">{spaceContext?.name ?? 'Loading...'}</span>
          </span>
          <div className="flex items-center gap-2 ml-auto">
            {spaceContext && (
              <Link
                to={`/spaces/${spaceIdFromUrl}`}
                className="text-blue-600 hover:text-blue-800 underline underline-offset-2"
              >
                View Space
              </Link>
            )}
            <button
              onClick={clearSpaceFilter}
              className="flex items-center gap-1 text-blue-600 hover:text-blue-800"
              aria-label="Clear space filter"
            >
              <X className="h-3.5 w-3.5" />
              Clear
            </button>
          </div>
        </div>
      )}

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        <RulesExplorer
          folders={folders}
          selectedFolderId={selectedFolderId}
          onFolderSelect={setSelectedFolderId}
          onCreateFolder={(parentId) => {
            setCreateFolderParentId(parentId);
            setShowCreateFolder(true);
          }}
          selectedTypes={selectedTypes}
          onTypeChange={setSelectedTypes}
          selectedStatuses={selectedStatuses}
          onStatusChange={setSelectedStatuses}
          selectedTags={selectedTags}
          onTagChange={setSelectedTags}
          tags={tags}
          datePreset={datePreset}
          onDatePresetChange={setDatePreset}
          activeFilterCount={filterChips.length}
        />

        {/* Main Content */}
        <div className="flex-1 min-w-0 overflow-auto flex flex-col">
          <div className="p-6 pb-0">
            {/* Search Bar + View Toggle */}
            <div className="flex items-center gap-3 mb-3">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search rules by name, description, or tags..."
                  className="block w-full h-10 rounded-lg border border-gray-200 bg-white pl-9 pr-9 text-sm text-gray-900 placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                />
                {search && (
                  <button
                    onClick={() => setSearch('')}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>

              {/* Sort dropdown */}
              <select
                value={`${sortBy}:${sortDir}`}
                onChange={(e) => {
                  const [key, dir] = e.target.value.split(':');
                  setSortBy(key as SortOption);
                  setSortDir(dir as 'asc' | 'desc');
                }}
                className="h-10 rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
              >
                <option value="name:asc">Name A-Z</option>
                <option value="name:desc">Name Z-A</option>
                <option value="createdAt:desc">Recently Created</option>
                <option value="updatedAt:desc">Last Modified</option>
              </select>

              {/* View toggle */}
              <div className="flex items-center rounded-lg border border-gray-200 bg-white">
                <button
                  onClick={() => setViewMode('table')}
                  className={cn(
                    'p-2 rounded-l-lg transition-colors',
                    viewMode === 'table'
                      ? 'text-indigo-600 bg-indigo-50'
                      : 'text-gray-400 hover:text-gray-600',
                  )}
                  title="Table view"
                >
                  <LayoutList className="h-4 w-4" />
                </button>
                <button
                  onClick={() => setViewMode('card')}
                  className={cn(
                    'p-2 rounded-r-lg transition-colors',
                    viewMode === 'card'
                      ? 'text-indigo-600 bg-indigo-50'
                      : 'text-gray-400 hover:text-gray-600',
                  )}
                  title="Card view"
                >
                  <LayoutGrid className="h-4 w-4" />
                </button>
              </div>
            </div>

            {/* Filter Chips */}
            {filterChips.length > 0 && (
              <div className="mb-3">
                <FilterChips
                  chips={filterChips}
                  onRemove={handleRemoveChip}
                  onClearAll={handleClearAllFilters}
                />
              </div>
            )}
          </div>

          {/* Content area */}
          <div className="flex-1 px-6 pb-6">
            {rules.length === 0 ? (
              <EmptyState
                title={hasActiveFilters ? 'No rules found' : 'No rules yet'}
                description={
                  hasActiveFilters
                    ? 'Try adjusting your filters or search terms.'
                    : 'Create your first rule to get started.'
                }
                actionLabel={hasActiveFilters ? 'Clear filters' : 'Create Rule'}
                onAction={hasActiveFilters ? handleClearAllFilters : () => setShowCreateRule(true)}
              />
            ) : viewMode === 'table' ? (
              /* TABLE VIEW */
              <div className="rounded-lg border border-gray-200 bg-white overflow-hidden">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="bg-gray-50 border-b border-gray-200">
                      <th className="w-10 px-3 py-3">
                        <input
                          type="checkbox"
                          checked={selectedRuleIds.size === rules.length && rules.length > 0}
                          onChange={toggleSelectAll}
                          className="h-3.5 w-3.5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                          aria-label="Select all rules on this page"
                        />
                      </th>
                      <th
                        className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 cursor-pointer hover:text-gray-700 min-w-[240px]"
                        onClick={() => handleSort('name')}
                      >
                        <span className="inline-flex items-center gap-1">
                          Name
                          {sortBy === 'name' && (
                            sortDir === 'asc'
                              ? <span className="text-indigo-500">&#9650;</span>
                              : <span className="text-indigo-500">&#9660;</span>
                          )}
                        </span>
                      </th>
                      <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[120px]">
                        Type
                      </th>
                      <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[160px]">
                        Status
                      </th>
                      <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[80px]">
                        Version
                      </th>
                      <th
                        className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[120px] cursor-pointer hover:text-gray-700"
                        onClick={() => handleSort('updatedAt')}
                      >
                        <span className="inline-flex items-center gap-1">
                          Updated
                          {sortBy === 'updatedAt' && (
                            sortDir === 'asc'
                              ? <span className="text-indigo-500">&#9650;</span>
                              : <span className="text-indigo-500">&#9660;</span>
                          )}
                        </span>
                      </th>
                      <th className="px-4 py-3 w-[48px]" />
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {rules.map((rule) => {
                      const isSelected = selectedRuleIds.has(rule.id);
                      return (
                        <tr
                          key={rule.id}
                          className={cn(
                            'transition-colors hover:bg-gray-50 cursor-pointer',
                            isSelected && 'bg-indigo-50 border-l-2 border-l-indigo-500',
                          )}
                          onClick={() => navigate(`/rules/${rule.id}/edit`)}
                        >
                          <td className="px-3 py-3" onClick={(e) => e.stopPropagation()}>
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => toggleSelect(rule.id)}
                              className="h-3.5 w-3.5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                              aria-label={`Select ${rule.name}`}
                            />
                          </td>
                          <td className="px-4 py-3">
                            <div>
                              <span className="font-semibold text-sm text-gray-900">{rule.name}</span>
                              {rule.description && (
                                <p className="text-xs text-gray-500 mt-0.5 line-clamp-1">{rule.description}</p>
                              )}
                              {rule.tags && rule.tags.length > 0 && (
                                <div className="flex gap-1 mt-1">
                                  {rule.tags.slice(0, 3).map((tag) => (
                                    <TagBadge key={tag} tag={tag} />
                                  ))}
                                  {rule.tags.length > 3 && (
                                    <span className="text-[11px] text-gray-400 self-center">
                                      +{rule.tags.length - 3}
                                    </span>
                                  )}
                                </div>
                              )}
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <RuleTypeIcon type={rule.type} showLabel />
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-1.5">
                              <StatusBadge status={rule.status} />
                              {rule.locked && <Lock className="h-3.5 w-3.5 text-gray-400" />}
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <span className="text-[13px] text-gray-600">v{rule.latestVersion}</span>
                          </td>
                          <td className="px-4 py-3">
                            <span
                              className="text-[13px] text-gray-500"
                              title={new Date(rule.updatedAt).toLocaleString()}
                            >
                              {formatRelativeTime(rule.updatedAt)}
                            </span>
                          </td>
                          <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                            <ActionMenu rule={rule} />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              /* CARD VIEW */
              <div className="grid gap-4 grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
                {rules.map((rule) => (
                  <RuleCard
                    key={rule.id}
                    rule={rule}
                    selected={selectedRuleIds.has(rule.id)}
                    onSelect={() => toggleSelect(rule.id)}
                    onClick={() => navigate(`/rules/${rule.id}/edit`)}
                    onEdit={() => navigate(`/rules/${rule.id}/edit`)}
                    onMenuOpen={(e) => {
                      e.stopPropagation();
                      setActionMenuRuleId(actionMenuRuleId === rule.id ? null : rule.id);
                    }}
                  />
                ))}
              </div>
            )}

            {/* Pagination */}
            {totalPages > 0 && rules.length > 0 && (
              <div className="flex items-center justify-between mt-4 pt-4 border-t border-gray-200 text-sm text-gray-500">
                <span>
                  Showing {page * pageSize + 1}-{Math.min((page + 1) * pageSize, totalElements)} of {totalElements} rules
                </span>
                <div className="flex items-center gap-3">
                  <select
                    value={pageSize}
                    onChange={(e) => {
                      setPageSize(Number(e.target.value));
                      setPage(0);
                    }}
                    className="h-8 rounded border border-gray-200 bg-white px-2 text-sm focus:outline-none"
                  >
                    {PAGE_SIZE_OPTIONS.map((size) => (
                      <option key={size} value={size}>{size} per page</option>
                    ))}
                  </select>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="h-8 w-8 flex items-center justify-center rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </button>
                    {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                      let pageNum: number;
                      if (totalPages <= 5) {
                        pageNum = i;
                      } else if (page < 3) {
                        pageNum = i;
                      } else if (page > totalPages - 4) {
                        pageNum = totalPages - 5 + i;
                      } else {
                        pageNum = page - 2 + i;
                      }
                      return (
                        <button
                          key={pageNum}
                          onClick={() => setPage(pageNum)}
                          className={cn(
                            'h-8 w-8 flex items-center justify-center rounded text-sm transition-colors',
                            page === pageNum
                              ? 'bg-indigo-50 text-indigo-600 font-medium'
                              : 'hover:bg-gray-100 text-gray-600',
                          )}
                        >
                          {pageNum + 1}
                        </button>
                      );
                    })}
                    <button
                      onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                      disabled={page >= totalPages - 1}
                      className="h-8 w-8 flex items-center justify-center rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Batch Action Bar */}
      <BatchActionBar
        selectedCount={selectedRuleIds.size}
        allDraft={allSelectedDraft}
        onRequestApproval={handleBatchRequestApproval}
        onExport={handleBatchExport}
        onClearSelection={() => setSelectedRuleIds(new Set())}
        onAssignToSpace={() => {
          const selected = rules.filter((r) => selectedRuleIds.has(r.id));
          setAssignToSpaceRules(selected);
        }}
      />

      {/* Modals */}
      <CreateRuleModal
        open={showCreateRule}
        onClose={() => setShowCreateRule(false)}
        onSubmit={handleCreateRule}
        loading={createRule.isPending}
        folders={folders}
      />

      <CreateFolderModal
        open={showCreateFolder}
        onClose={() => setShowCreateFolder(false)}
        onSubmit={handleCreateFolder}
        loading={createFolder.isPending}
        parentId={createFolderParentId}
      />

      {showImportRule && (
        <ImportRuleModal
          spaceId={selectedFolderId ?? ''}
          createdBy={user?.id ?? ''}
          onClose={() => setShowImportRule(false)}
          onSuccess={() => {
            refetchRules();
            addToast({ type: 'success', title: 'Rule imported successfully' });
          }}
        />
      )}

      <RequestApprovalModal
        open={approvalModalRules.length > 0}
        onClose={() => setApprovalModalRules([])}
        rules={approvalModalRules}
        onSubmitted={() => {
          setSelectedRuleIds(new Set());
          refetchRules();
        }}
      />

      {assignToSpaceRules.length > 0 && (
        <AssignToSpaceModal
          rules={assignToSpaceRules}
          onClose={() => setAssignToSpaceRules([])}
          onSuccess={() => {
            setSelectedRuleIds(new Set());
            refetchRules();
          }}
        />
      )}
    </div>
  );
}
