import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Save,
  Upload,
  FileText,
  GitBranch,
  FlaskConical,
  Settings,
  Plus,
} from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Spinner } from '@/shared/components/Spinner';
import { Breadcrumb } from '@/shared/components/Breadcrumb';
import {
  useRule,
  useUpdateRule,
  useRuleVersions,
  usePublishVersion,
  useUpdateVersion,
  useCreateVersion,
} from '@/api/rules.api';
import { useEditorStore } from '@/stores/editor.store';
import { useToastStore } from '@/stores/toast.store';
import { useTableEditorStore } from '@/stores/table-editor.store';
import { useTreeEditorStore } from '@/stores/tree-editor.store';
import { useScriptEditorStore } from '@/stores/script-editor.store';
import { useFlowEditorStore } from '@/stores/flow-editor.store';
import { useScorecardEditorStore } from '@/stores/scorecard-editor.store';
import { useEnsembleEditorStore } from '@/stores/ensemble-editor.store';
import { useDmnEditorStore } from '@/stores/dmn-editor.store';
import { useCepEditorStore } from '@/stores/cep-editor.store';
import { TableEditor } from '@/features/table-editor/components/TableEditor';
import { TreeEditor } from '@/features/tree-editor/components/TreeEditor';
import { ScriptEditor } from '@/features/script-editor/components/ScriptEditor';
import { FlowEditor } from '@/features/flow-editor/components/FlowEditor';
import { ScorecardEditor } from '@/features/scorecard-editor/components/ScorecardEditor';
import { EnsembleEditor } from '@/features/ensemble-editor/components/EnsembleEditor';
import { DmnEditor } from '@/features/dmn-editor/components/DmnEditor';
import { CepEditor } from '@/features/cep-editor/components/CepEditor';
import { TestBench } from '@/features/test-bench/components/TestBench';
import type { RuleVersion } from '@/api/types/rule.types';

type EditorTab = 'definition' | 'versions' | 'test' | 'settings';

const statusColors: Record<string, string> = {
  DRAFT: 'bg-yellow-100 text-yellow-800',
  PUBLISHED: 'bg-green-100 text-green-800',
  ARCHIVED: 'bg-gray-100 text-gray-600',
  PENDING: 'bg-blue-100 text-blue-800',
  DEPRECATED: 'bg-red-100 text-red-800',
};

const typeColors: Record<string, string> = {
  TABLE: 'bg-blue-100 text-blue-800',
  TREE: 'bg-purple-100 text-purple-800',
  SCRIPT: 'bg-orange-100 text-orange-800',
  FLOW: 'bg-cyan-100 text-cyan-800',
  SCORECARD: 'bg-amber-100 text-amber-800',
  ENSEMBLE: 'bg-rose-100 text-rose-800',
  DMN: 'bg-teal-100 text-teal-800',
  CEP: 'bg-violet-100 text-violet-800',
};

const tabs: { key: EditorTab; label: string; icon: React.ReactNode }[] = [
  { key: 'definition', label: 'Definition', icon: <FileText className="h-4 w-4" /> },
  { key: 'versions', label: 'Versions', icon: <GitBranch className="h-4 w-4" /> },
  { key: 'test', label: 'Test', icon: <FlaskConical className="h-4 w-4" /> },
  { key: 'settings', label: 'Settings', icon: <Settings className="h-4 w-4" /> },
];

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function getDefinitionFromStore(
  ruleType: string,
  tableToDefinition: () => unknown,
  treeToDefinition: () => unknown,
  scriptState: { scriptBody: string; language: string; timeout: number; maxMemoryMb: number },
  flowToDefinition: () => unknown,
  scorecardToDefinition: () => unknown,
  ensembleToDefinition: () => unknown,
  dmnToDefinition: () => unknown,
  cepToDefinition: () => unknown,
): unknown {
  switch (ruleType) {
    case 'TABLE': return tableToDefinition();
    case 'TREE': return treeToDefinition();
    case 'SCRIPT': return scriptState;
    case 'FLOW': return flowToDefinition();
    case 'SCORECARD': return scorecardToDefinition();
    case 'ENSEMBLE': return ensembleToDefinition();
    case 'DMN': return dmnToDefinition();
    case 'CEP': return cepToDefinition();
    default: return null;
  }
}

export function RuleEditorPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const { isDirty, setDirty } = useEditorStore();

  const [activeTab, setActiveTab] = useState<EditorTab>('definition');
  const [selectedVersion, setSelectedVersion] = useState<number | null>(null);

  // Settings tab local state
  const [settingsName, setSettingsName] = useState('');
  const [settingsDescription, setSettingsDescription] = useState('');
  const [settingsAlias, setSettingsAlias] = useState('');

  const { data: ruleData, isLoading: ruleLoading } = useRule(id!);
  const { data: versionsData, isLoading: versionsLoading } = useRuleVersions(id!);
  const updateRule = useUpdateRule();
  const publishVersion = usePublishVersion();
  const updateVersion = useUpdateVersion();
  const createVersion = useCreateVersion();

  const tableToDefinition = useTableEditorStore((s) => s.toDefinition);
  const treeToDefinition = useTreeEditorStore((s) => s.toDefinition);
  const loadTableDefinition = useTableEditorStore((s) => s.loadFromDefinition);
  const loadTreeDefinition = useTreeEditorStore((s) => s.loadFromDefinition);
  const setScriptDefinition = useScriptEditorStore((s) => s.setDefinition);
  const scriptState = useScriptEditorStore((s) => ({ scriptBody: s.scriptBody, language: s.language, timeout: s.timeout, maxMemoryMb: s.maxMemoryMb }));
  const setFlowDefinition = useFlowEditorStore((s) => s.setDefinition);
  const flowDefinition = useFlowEditorStore((s) => s.definition);
  const loadScorecardDefinition = useScorecardEditorStore((s) => s.loadFromDefinition);
  const scorecardToDefinition = useScorecardEditorStore((s) => s.toDefinition);
  const loadEnsembleDefinition = useEnsembleEditorStore((s) => s.loadFromDefinition);
  const ensembleToDefinition = useEnsembleEditorStore((s) => s.toDefinition);
  const loadDmnDefinition = useDmnEditorStore((s) => s.loadFromDefinition);
  const dmnToDefinition = useDmnEditorStore((s) => s.toDefinition);
  const loadCepDefinition = useCepEditorStore((s) => s.loadFromDefinition);
  const cepToDefinition = useCepEditorStore((s) => s.toDefinition);

  const rule = ruleData;
  const versions: RuleVersion[] = Array.isArray(versionsData) ? versionsData : [];

  const currentVersion = versions.find((v) => v.version === selectedVersion) ?? versions[0];

  // Load definition into editor when version changes
  useEffect(() => {
    if (!currentVersion?.definition) return;
    if (rule?.type === 'TABLE') {
      loadTableDefinition(currentVersion.definition);
    } else if (rule?.type === 'TREE') {
      loadTreeDefinition(currentVersion.definition);
    } else if (rule?.type === 'SCRIPT') {
      const def = currentVersion.definition as Record<string, unknown>;
      setScriptDefinition({
        scriptBody: (def.scriptBody as string) ?? '',
        language: (def.language as string) ?? 'js',
        timeout: (def.timeout as number) ?? 5000,
        maxMemoryMb: (def.maxMemoryMb as number) ?? 64,
      });
    } else if (rule?.type === 'FLOW') {
      setFlowDefinition(currentVersion.definition as unknown as Parameters<typeof setFlowDefinition>[0]);
    } else if (rule?.type === 'SCORECARD') {
      loadScorecardDefinition(currentVersion.definition);
    } else if (rule?.type === 'ENSEMBLE') {
      loadEnsembleDefinition(currentVersion.definition);
    } else if (rule?.type === 'DMN') {
      loadDmnDefinition(currentVersion.definition);
    } else if (rule?.type === 'CEP') {
      loadCepDefinition(currentVersion.definition);
    }
    setDirty(false);
  }, [currentVersion?.id, rule?.type, loadTableDefinition, loadTreeDefinition, setScriptDefinition, setFlowDefinition, loadScorecardDefinition, loadEnsembleDefinition, loadDmnDefinition, loadCepDefinition, setDirty]);

  // Set default selected version
  useEffect(() => {
    if (rule && selectedVersion === null) {
      setSelectedVersion(rule.latestVersion);
    }
  }, [rule, selectedVersion]);

  // Initialize settings tab from rule data
  useEffect(() => {
    if (rule) {
      setSettingsName(rule.name ?? '');
      setSettingsDescription(rule.description ?? '');
      setSettingsAlias(rule.alias ?? '');
    }
  }, [rule]);

  const handleSave = useCallback(() => {
    if (!id || !currentVersion || !rule) return;

    const definition = getDefinitionFromStore(
      rule.type, tableToDefinition, treeToDefinition, scriptState,
      () => flowDefinition, scorecardToDefinition, ensembleToDefinition,
      dmnToDefinition, cepToDefinition,
    );

    if (currentVersion.status !== 'DRAFT') {
      addToast({ type: 'error', title: 'Cannot save — version is not a draft' });
      return;
    }

    updateVersion.mutate(
      {
        ruleId: id,
        version: currentVersion.version,
        data: { definition: JSON.stringify(definition) },
      },
      {
        onSuccess: () => {
          setDirty(false);
          addToast({ type: 'success', title: 'Rule saved successfully' });
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to save rule' });
        },
      },
    );
  }, [id, currentVersion, rule, tableToDefinition, treeToDefinition, scriptState, flowDefinition, scorecardToDefinition, ensembleToDefinition, dmnToDefinition, cepToDefinition, updateVersion, setDirty, addToast]);

  const handleCreateVersion = useCallback(() => {
    if (!id || !rule) return;

    const definition = getDefinitionFromStore(
      rule.type, tableToDefinition, treeToDefinition, scriptState,
      () => flowDefinition, scorecardToDefinition, ensembleToDefinition,
      dmnToDefinition, cepToDefinition,
    );

    createVersion.mutate(
      {
        ruleId: id,
        data: {
          definition: JSON.stringify(definition),
          changeNote: 'New version',
        },
      },
      {
        onSuccess: (newVersion) => {
          setSelectedVersion(newVersion.version);
          addToast({ type: 'success', title: `Version ${newVersion.version} created` });
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to create version' });
        },
      },
    );
  }, [id, rule, tableToDefinition, treeToDefinition, scriptState, flowDefinition, scorecardToDefinition, ensembleToDefinition, dmnToDefinition, cepToDefinition, createVersion, addToast]);

  const handlePublish = useCallback(() => {
    if (!id || !currentVersion) return;
    publishVersion.mutate(
      { ruleId: id, version: currentVersion.version },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Version published successfully' });
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to publish version' });
        },
      },
    );
  }, [id, currentVersion, publishVersion, addToast]);

  const handleSaveSettings = useCallback(() => {
    if (!id) return;
    updateRule.mutate(
      { id, data: { name: settingsName, description: settingsDescription, alias: settingsAlias } },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Settings saved successfully' });
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to save settings' });
        },
      },
    );
  }, [id, settingsName, settingsDescription, settingsAlias, updateRule, addToast]);

  if (ruleLoading || versionsLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!rule) {
    return (
      <div className="p-8 text-center text-gray-500">
        Rule not found.{' '}
        <button
          className="text-indigo-600 hover:underline"
          onClick={() => navigate('/rules')}
        >
          Go back to rules
        </button>
      </div>
    );
  }

  const latestVersion = versions.length > 0
    ? versions.reduce((max, v) => v.version > max.version ? v : max, versions[0])
    : null;
  const canCreateVersion = latestVersion?.status === 'PUBLISHED' || latestVersion?.status === 'DEPRECATED';

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="border-b border-gray-200 bg-white px-6 py-3">
        <div className="flex items-center gap-3 mb-2">
          <Breadcrumb
            items={[
              { label: 'Rules', href: '/rules' },
              { label: rule.name },
            ]}
          />
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/rules')}
              className="p-1 rounded hover:bg-gray-100 transition-colors"
            >
              <ArrowLeft className="h-5 w-5 text-gray-500" />
            </button>
            <h1 className="text-lg font-semibold text-gray-900">{rule.name}</h1>
            <Badge className={typeColors[rule.type] ?? 'bg-gray-100 text-gray-800'}>
              {rule.type}
            </Badge>
            <Badge className={statusColors[rule.status]}>{rule.status}</Badge>
          </div>
          <div className="flex items-center gap-3">
            <Select
              value={String(selectedVersion ?? '')}
              onChange={(e) => setSelectedVersion(Number(e.target.value))}
              className="w-36"
            >
              {versions.map((v) => (
                <option key={v.id} value={v.version}>
                  v{v.version}{' '}
                  {v.status === 'DRAFT' ? '(Draft)' : v.status === 'PUBLISHED' ? '(Published)' : ''}
                </option>
              ))}
            </Select>
            <Button
              variant="outline"
              onClick={handleSave}
              loading={updateVersion.isPending}
              disabled={!isDirty || currentVersion?.status !== 'DRAFT'}
            >
              <Save className="h-4 w-4" />
              Save
            </Button>
            {currentVersion?.status === 'DRAFT' && (
              <Button
                onClick={handlePublish}
                loading={publishVersion.isPending}
              >
                <Upload className="h-4 w-4" />
                Publish
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 bg-white px-6">
        <nav className="flex gap-6" aria-label="Editor tabs">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex items-center gap-2 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.key
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'definition' && (
          <div className="h-full p-4">
            {rule.type === 'TABLE' && <TableEditor />}
            {rule.type === 'TREE' && <TreeEditor />}
            {rule.type === 'SCRIPT' && (
              <ScriptEditor
                scriptBody={scriptState.scriptBody}
                language={scriptState.language}
                timeout={scriptState.timeout}
                maxMemoryMb={scriptState.maxMemoryMb}
                onChange={(def) => {
                  setScriptDefinition(def);
                  setDirty(true);
                }}
              />
            )}
            {rule.type === 'FLOW' && <FlowEditor />}
            {rule.type === 'SCORECARD' && <ScorecardEditor />}
            {rule.type === 'ENSEMBLE' && <EnsembleEditor />}
            {rule.type === 'DMN' && <DmnEditor />}
            {rule.type === 'CEP' && <CepEditor />}
            {!['TABLE', 'TREE', 'SCRIPT', 'FLOW', 'SCORECARD', 'ENSEMBLE', 'DMN', 'CEP'].includes(rule.type) && (
              <div className="flex items-center justify-center h-full text-gray-500">
                Editor for {rule.type} type is not yet supported.
              </div>
            )}
          </div>
        )}

        {activeTab === 'versions' && (
          <div className="p-6 overflow-auto">
            <div className="max-w-3xl">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900">
                  Version History
                </h2>
                {canCreateVersion && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={handleCreateVersion}
                    loading={createVersion.isPending}
                  >
                    <Plus className="h-4 w-4" />
                    New Version
                  </Button>
                )}
              </div>
              <div className="space-y-3">
                {versions.length === 0 ? (
                  <p className="text-gray-500">No versions available.</p>
                ) : (
                  versions.map((v) => (
                    <div
                      key={v.id}
                      className={`flex items-center justify-between p-4 rounded-lg border transition-colors ${
                        selectedVersion === v.version
                          ? 'border-indigo-300 bg-indigo-50'
                          : 'border-gray-200 bg-white hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <span className="font-medium text-gray-900">
                          Version {v.version}
                        </span>
                        <Badge className={statusColors[v.status]}>
                          {v.status}
                        </Badge>
                      </div>
                      <div className="flex items-center gap-4 text-sm text-gray-500">
                        <span>Created {formatDate(v.createdAt)}</span>
                        {v.publishedAt && (
                          <span>Published {formatDate(v.publishedAt)}</span>
                        )}
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setSelectedVersion(v.version)}
                        >
                          {selectedVersion === v.version ? 'Current' : 'View'}
                        </Button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'test' && (
          <TestBench ruleId={id!} version={selectedVersion ?? undefined} />
        )}

        {activeTab === 'settings' && (
          <div className="p-6 overflow-auto">
            <div className="max-w-lg space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">
                Rule Settings
              </h2>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Rule Name
                </label>
                <Input
                  value={settingsName}
                  onChange={(e) => setSettingsName(e.target.value)}
                  placeholder="Rule name"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Alias
                </label>
                <Input
                  value={settingsAlias}
                  onChange={(e) => setSettingsAlias(e.target.value)}
                  placeholder="rule-alias"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description
                </label>
                <textarea
                  value={settingsDescription}
                  onChange={(e) => setSettingsDescription(e.target.value)}
                  placeholder="Enter a description for this rule"
                  rows={4}
                  className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Type
                </label>
                <p className="text-sm text-gray-900">{rule.type}</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Created
                </label>
                <p className="text-sm text-gray-500">{formatDate(rule.createdAt)}</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Last Updated
                </label>
                <p className="text-sm text-gray-500">{formatDate(rule.updatedAt)}</p>
              </div>
              <div className="pt-2">
                <Button
                  onClick={handleSaveSettings}
                  loading={updateRule.isPending}
                >
                  Save Settings
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
