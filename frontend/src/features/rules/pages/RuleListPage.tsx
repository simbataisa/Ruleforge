import { useState, useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, RefreshCw, ChevronLeft, ChevronRight, FileDown } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Badge } from '@/shared/components/Badge';
import { DataTable } from '@/shared/components/DataTable';
import { SkeletonRow } from '@/components/Skeleton';
import { EmptyState } from '@/shared/components/EmptyState';
import { useRules, useCreateRule } from '@/api/rules.api';
import { useFolders, useCreateFolder } from '@/api/folders.api';
import { useToastStore } from '@/stores/toast.store';
import { FolderTree } from '@/features/rules/components/FolderTree';
import { CreateRuleModal } from '@/features/rules/components/CreateRuleModal';
import { CreateFolderModal } from '@/features/rules/components/CreateFolderModal';
import { ImportRuleModal } from '@/features/rules/components/ImportRuleModal';
import { useAuthStore } from '@/stores/auth.store';
import type { Rule, RuleType, RuleStatus } from '@/api/types/rule.types';

const statusColors: Record<RuleStatus, string> = {
  DRAFT: 'bg-yellow-100 text-yellow-800',
  PUBLISHED: 'bg-green-100 text-green-800',
  ARCHIVED: 'bg-gray-100 text-gray-600',
};

const typeColors: Record<string, string> = {
  TABLE: 'bg-blue-100 text-blue-800',
  TREE: 'bg-purple-100 text-purple-800',
  SCRIPT: 'bg-orange-100 text-orange-800',
  FLOW: 'bg-cyan-100 text-cyan-800',
  SCORECARD: 'bg-pink-100 text-pink-800',
  DMN: 'bg-indigo-100 text-indigo-800',
  CEP: 'bg-red-100 text-red-800',
  LOOKUP: 'bg-teal-100 text-teal-800',
  ENSEMBLE: 'bg-amber-100 text-amber-800',
};

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export function RuleListPage() {
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const user = useAuthStore((s) => s.user);

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const [showCreateRule, setShowCreateRule] = useState(false);
  const [showCreateFolder, setShowCreateFolder] = useState(false);
  const [showImportRule, setShowImportRule] = useState(false);
  const [createFolderParentId, setCreateFolderParentId] = useState<
    string | undefined
  >();

  // Debounce search input by 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const { data: rulesData, isLoading: rulesLoading, isFetching: rulesFetching, refetch: refetchRules } = useRules({
    page,
    size: PAGE_SIZE,
    search: debouncedSearch || undefined,
    type: typeFilter || undefined,
    status: statusFilter || undefined,
    folderId: selectedFolderId || undefined,
  });
  const { data: foldersData, isLoading: foldersLoading, refetch: refetchFolders } = useFolders();

  const handleRefresh = () => {
    refetchRules();
    refetchFolders();
  };
  const createRule = useCreateRule();
  const createFolder = useCreateFolder();

  const rules: Rule[] = useMemo(() => {
    const allRules = rulesData?.content ?? [];
    return Array.isArray(allRules) ? allRules : [];
  }, [rulesData]);

  const totalPages = rulesData?.totalPages ?? 0;
  const totalElements = rulesData?.totalElements ?? 0;

  // Reset to page 0 when filters change
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, typeFilter, statusFilter, selectedFolderId]);

  const folders = useMemo(() => {
    return Array.isArray(foldersData) ? foldersData : [];
  }, [foldersData]);

  const handleCreateRule = (data: {
    name: string;
    type: RuleType;
    description?: string;
    alias?: string;
    folderId?: string;
  }) => {
    createRule.mutate(data, {
      onSuccess: (response) => {
        setShowCreateRule(false);
        addToast({ type: 'success', title: 'Rule created successfully' });
        if (response?.id) {
          navigate(`/rules/${response.id}/edit`);
        }
      },
      onError: () => {
        addToast({ type: 'error', title: 'Failed to create rule' });
      },
    });
  };

  const handleCreateFolder = (data: { name: string; parentId?: string }) => {
    createFolder.mutate(data, {
      onSuccess: () => {
        setShowCreateFolder(false);
        addToast({ type: 'success', title: 'Folder created successfully' });
      },
      onError: () => {
        addToast({ type: 'error', title: 'Failed to create folder' });
      },
    });
  };

  const handleOpenCreateFolder = (parentId?: string) => {
    setCreateFolderParentId(parentId);
    setShowCreateFolder(true);
  };

  const columns = [
    {
      key: 'name' as const,
      header: 'Name',
      render: (rule: Rule) => (
        <span className="font-medium text-gray-900">{rule.name}</span>
      ),
    },
    {
      key: 'type' as const,
      header: 'Type',
      render: (rule: Rule) => (
        <Badge className={typeColors[rule.type] ?? 'bg-gray-100 text-gray-800'}>
          {rule.type}
        </Badge>
      ),
    },
    {
      key: 'status' as const,
      header: 'Status',
      render: (rule: Rule) => (
        <Badge className={statusColors[rule.status]}>{rule.status}</Badge>
      ),
    },
    {
      key: 'latestVersion' as const,
      header: 'Version',
      render: (rule: Rule) => (
        <span className="text-gray-600">v{rule.latestVersion}</span>
      ),
    },
    {
      key: 'updatedAt' as const,
      header: 'Last Updated',
      render: (rule: Rule) => (
        <span className="text-gray-500">{formatDate(rule.updatedAt)}</span>
      ),
    },
  ];

  if (rulesLoading || foldersLoading) {
    return (
      <div className="flex flex-col h-full">
        <PageHeader title="Rules" />
        <div className="flex flex-1 overflow-hidden">
          <div className="w-60 flex-shrink-0 border-r border-gray-200 bg-gray-50 p-4">
            <div className="space-y-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="h-4 animate-pulse rounded bg-gray-200" style={{ width: `${60 + i * 10}%` }} />
              ))}
            </div>
          </div>
          <div className="flex-1 p-6">
            <div className="rounded-lg border border-gray-200 bg-white">
              <div className="flex items-center gap-4 px-6 py-3 border-b border-gray-200">
                {['40%', '12%', '12%', '10%', '15%'].map((w, i) => (
                  <div key={i} className="h-3 animate-pulse rounded bg-gray-300" style={{ width: w }} />
                ))}
              </div>
              {Array.from({ length: 8 }).map((_, i) => (
                <SkeletonRow key={i} columns={5} />
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <PageHeader
        title="Rules"
        actions={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={handleRefresh} disabled={rulesFetching}>
              <RefreshCw className={`h-4 w-4 ${rulesFetching ? 'animate-spin' : ''}`} />
            </Button>
            <Button variant="outline" onClick={() => setShowImportRule(true)}>
              <FileDown className="h-4 w-4" />
              Import Rule
            </Button>
            <Button onClick={() => setShowCreateRule(true)}>
              <Plus className="h-4 w-4" />
              Create Rule
            </Button>
          </div>
        }
      />

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar - Folder Tree */}
        <div className="w-60 flex-shrink-0 border-r border-gray-200 bg-gray-50 overflow-y-auto">
          <FolderTree
            folders={folders}
            selectedFolderId={selectedFolderId}
            onSelect={setSelectedFolderId}
            onCreateFolder={handleOpenCreateFolder}
          />
        </div>

        {/* Main Content */}
        <div className="flex-1 overflow-auto p-6">
          {/* Filters */}
          <div className="flex items-center gap-3 mb-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search rules..."
                className="pl-9"
              />
            </div>
            <Select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
            >
              <option value="">All Types</option>
              <option value="TABLE">Table</option>
              <option value="TREE">Tree</option>
              <option value="SCRIPT">Script</option>
              <option value="FLOW">Flow</option>
              <option value="SCORECARD">Scorecard</option>
            </Select>
            <Select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="DRAFT">Draft</option>
              <option value="PUBLISHED">Published</option>
              <option value="ARCHIVED">Archived</option>
            </Select>
          </div>

          {/* Rules Table */}
          {rules.length === 0 ? (
            <EmptyState
              title="No rules found"
              description={
                search || typeFilter || statusFilter
                  ? 'Try adjusting your filters.'
                  : 'Create your first rule to get started.'
              }
            />
          ) : (
            <>
              <DataTable
                data={rules}
                columns={columns}
                onRowClick={(rule) => navigate(`/rules/${rule.id}/edit`)}
                className="cursor-pointer hover:bg-gray-50"
              />
              {totalPages > 1 && (
                <div className="flex items-center justify-between mt-4 text-sm text-gray-600">
                  <span>
                    {totalElements} rules &middot; Page {page + 1} of {totalPages}
                  </span>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                      className="p-1 rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                      disabled={page >= totalPages - 1}
                      className="p-1 rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>

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
    </div>
  );
}
