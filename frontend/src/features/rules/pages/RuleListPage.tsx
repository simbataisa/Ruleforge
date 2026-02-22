import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Badge } from '@/shared/components/Badge';
import { DataTable } from '@/shared/components/DataTable';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import { useRules, useCreateRule } from '@/api/rules.api';
import { useFolders, useCreateFolder } from '@/api/folders.api';
import { useToastStore } from '@/stores/toast.store';
import { FolderTree } from '@/features/rules/components/FolderTree';
import { CreateRuleModal } from '@/features/rules/components/CreateRuleModal';
import { CreateFolderModal } from '@/features/rules/components/CreateFolderModal';
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

  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);
  const [showCreateRule, setShowCreateRule] = useState(false);
  const [showCreateFolder, setShowCreateFolder] = useState(false);
  const [createFolderParentId, setCreateFolderParentId] = useState<
    string | undefined
  >();

  const { data: rulesData, isLoading: rulesLoading } = useRules();
  const { data: foldersData, isLoading: foldersLoading } = useFolders();
  const createRule = useCreateRule();
  const createFolder = useCreateFolder();

  const rules: Rule[] = useMemo(() => {
    const allRules = rulesData?.content ?? [];
    const ruleList = Array.isArray(allRules) ? allRules : [];
    return ruleList.filter((rule: Rule) => {
      if (search && !rule.name.toLowerCase().includes(search.toLowerCase())) {
        return false;
      }
      if (typeFilter && rule.type !== typeFilter) return false;
      if (statusFilter && rule.status !== statusFilter) return false;
      if (selectedFolderId && rule.folderId !== selectedFolderId) return false;
      return true;
    });
  }, [rulesData, search, typeFilter, statusFilter, selectedFolderId]);

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
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <PageHeader
        title="Rules"
        actions={
          <Button onClick={() => setShowCreateRule(true)}>
            <Plus className="h-4 w-4" />
            Create Rule
          </Button>
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
            <DataTable
              data={rules}
              columns={columns}
              onRowClick={(rule) => navigate(`/rules/${rule.id}/edit`)}
              className="cursor-pointer hover:bg-gray-50"
            />
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
    </div>
  );
}
