import React, { useState, useEffect, useCallback, Suspense, useRef } from 'react';
import { useParams, useNavigate, useBlocker } from 'react-router-dom';
import {
  ArrowLeft,
  Save,
  Upload,
  FileText,
  GitBranch,
  FlaskConical,
  Settings,
  Plus,
  GitCompareArrows,
  Lock,
  Copy,
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
  useApproveVersion,
  useRejectVersion,
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
import { TestBench } from '@/features/test-bench/components/TestBench';

// Lazy-loaded editor components for code splitting
const TableEditor = React.lazy(() => import('@/features/table-editor/components/TableEditor').then(m => ({ default: m.TableEditor })));
const TreeEditor = React.lazy(() => import('@/features/tree-editor/components/TreeEditor').then(m => ({ default: m.TreeEditor })));
const ScriptEditor = React.lazy(() => import('@/features/script-editor/components/ScriptEditor'));
const FlowEditor = React.lazy(() => import('@/features/flow-editor/components/FlowEditor'));
const ScorecardEditor = React.lazy(() => import('@/features/scorecard-editor/components/ScorecardEditor').then(m => ({ default: m.ScorecardEditor })));
const EnsembleEditor = React.lazy(() => import('@/features/ensemble-editor/components/EnsembleEditor').then(m => ({ default: m.EnsembleEditor })));
const DmnEditor = React.lazy(() => import('@/features/dmn-editor/components/DmnEditor').then(m => ({ default: m.DmnEditor })));
const CepEditor = React.lazy(() => import('@/features/cep-editor/components/CepEditor').then(m => ({ default: m.CepEditor })));
import { ExportRuleButton } from '@/features/rules/components/ExportRuleButton';
import { ApprovalBadge } from '@/features/rules/components/ApprovalBadge';
import { VersionDiffModal } from '@/features/rules/components/VersionDiffModal';
import { RequestApprovalModal } from '@/features/rules/components/RequestApprovalModal';
import type { RuleVersion } from '@/api/types/rule.types';
import { ViewToggle } from '@/features/rules/components/ViewToggle';
import { JsonModelEditor } from '@/features/rules/components/JsonModelEditor';
import { ModelViewStatusBar } from '@/features/rules/components/ModelViewStatusBar';
import { RuleDefinitionViewer } from '@/features/rules/components/RuleDefinitionViewer';

type EditorTab = 'definition' | 'versions' | 'test' | 'settings';

const statusColors: Record<string, string> = {
  DRAFT: 'bg-yellow-100 text-yellow-800',
  PUBLISHED: 'bg-green-100 text-green-800',
  ARCHIVED: 'bg-gray-100 text-gray-600',
  PENDING: 'bg-blue-100 text-blue-800',
  PENDING_APPROVAL: 'bg-amber-100 text-amber-800',
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
  const [diffModalOpen, setDiffModalOpen] = useState(false);
  const [compareVersion, setCompareVersion] = useState<number | null>(null);

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
  const approveVersion = useApproveVersion();
  const rejectVersion = useRejectVersion();

  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [requestApprovalModalOpen, setRequestApprovalModalOpen] = useState(false);

  const [viewMode, setViewMode] = useState<'designer' | 'model'>('designer');
  const [modelJson, setModelJson] = useState('');
  const [jsonStatus, setJsonStatus] = useState<'valid' | 'invalid' | 'syncing'>('valid');
  const syncTimerRef = useRef<number | null>(null);

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
    // Backend returns definition as JSON string — parse it
    let def: Record<string, unknown> = currentVersion.definition as Record<string, unknown>;
    if (typeof def === 'string') {
      try { def = JSON.parse(def); } catch { /* keep as-is */ }
    }
    if (rule?.type === 'TABLE') {
      loadTableDefinition(def);
    } else if (rule?.type === 'TREE') {
      loadTreeDefinition(def);
    } else if (rule?.type === 'SCRIPT') {
      setScriptDefinition({
        scriptBody: (def.scriptBody as string) ?? '',
        language: (def.language as string) ?? 'js',
        timeout: (def.timeout as number) ?? 5000,
        maxMemoryMb: (def.maxMemoryMb as number) ?? 64,
      });
    } else if (rule?.type === 'FLOW') {
      setFlowDefinition(def as unknown as Parameters<typeof setFlowDefinition>[0]);
    } else if (rule?.type === 'SCORECARD') {
      loadScorecardDefinition(def);
    } else if (rule?.type === 'ENSEMBLE') {
      loadEnsembleDefinition(def);
    } else if (rule?.type === 'DMN') {
      loadDmnDefinition(def);
    } else if (rule?.type === 'CEP') {
      loadCepDefinition(def);
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

  // Warn on browser close/refresh with unsaved changes
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (isDirty) {
        e.preventDefault();
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [isDirty]);

  // Warn on React Router navigation with unsaved changes
  const blocker = useBlocker(isDirty);
  useEffect(() => {
    if (blocker.state === 'blocked') {
      const confirmed = window.confirm('You have unsaved changes. Are you sure you want to leave?');
      if (confirmed) {
        blocker.proceed();
      } else {
        blocker.reset();
      }
    }
  }, [blocker]);

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


  const handleApprove = useCallback(() => {
    if (!id || !currentVersion) return;
    if (!confirm('Are you sure you want to approve this version?')) return;
    approveVersion.mutate(
      { ruleId: id, version: currentVersion.version },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Version approved and published' });
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to approve version' });
        },
      },
    );
  }, [id, currentVersion, approveVersion, addToast]);

  const handleReject = useCallback(() => {
    if (!id || !currentVersion || !rejectReason.trim()) return;
    rejectVersion.mutate(
      { ruleId: id, version: currentVersion.version, reason: rejectReason },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Version rejected' });
          setRejectModalOpen(false);
          setRejectReason('');
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to reject version' });
        },
      },
    );
  }, [id, currentVersion, rejectReason, rejectVersion, addToast]);

  const handleSwitchToModel = useCallback(() => {
    if (!rule) return;
    const def = getDefinitionFromStore(
      rule.type, tableToDefinition, treeToDefinition, scriptState,
      () => flowDefinition, scorecardToDefinition, ensembleToDefinition,
      dmnToDefinition, cepToDefinition,
    );
    setModelJson(JSON.stringify(def, null, 2));
    setJsonStatus('valid');
    setViewMode('model');
  }, [rule, tableToDefinition, treeToDefinition, scriptState, flowDefinition, scorecardToDefinition, ensembleToDefinition, dmnToDefinition, cepToDefinition]);

  const syncModelToDesigner = useCallback((jsonStr: string) => {
    if (syncTimerRef.current !== null) window.clearTimeout(syncTimerRef.current);
    setJsonStatus('syncing');
    syncTimerRef.current = window.setTimeout(() => {
      try {
        const def = JSON.parse(jsonStr);
        if (!rule) return;
        switch (rule.type) {
          case 'TABLE': loadTableDefinition(def); break;
          case 'TREE': loadTreeDefinition(def); break;
          case 'SCRIPT':
            setScriptDefinition({
              scriptBody: (def.scriptBody as string) ?? '',
              language: (def.language as string) ?? 'js',
              timeout: (def.timeout as number) ?? 5000,
              maxMemoryMb: (def.maxMemoryMb as number) ?? 64,
            });
            break;
          case 'SCORECARD': loadScorecardDefinition(def); break;
          case 'ENSEMBLE': loadEnsembleDefinition(def); break;
          case 'DMN': loadDmnDefinition(def); break;
          case 'CEP': loadCepDefinition(def); break;
        }
        setDirty(true);
        setJsonStatus('valid');
      } catch {
        setJsonStatus('invalid');
      }
    }, 500);
  }, [rule, loadTableDefinition, loadTreeDefinition, setScriptDefinition, loadScorecardDefinition, loadEnsembleDefinition, loadDmnDefinition, loadCepDefinition, setDirty]);

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

  // Reset view mode when version changes
  useEffect(() => {
    setViewMode('designer');
  }, [currentVersion?.id]);

  // Cleanup debounce timer on unmount
  useEffect(() => {
    return () => { if (syncTimerRef.current !== null) window.clearTimeout(syncTimerRef.current); };
  }, []);

  // Keyboard shortcuts: Ctrl/Cmd+S to save, Ctrl/Cmd+Shift+T to open test tab
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const mod = e.metaKey || e.ctrlKey;
      if (mod && e.key === 's' && !e.shiftKey) {
        e.preventDefault();
        if (isDirty && currentVersion?.status === 'DRAFT') {
          handleSave();
        }
      }
      if (mod && e.shiftKey && e.key === 'T') {
        e.preventDefault();
        setActiveTab('test');
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isDirty, currentVersion?.status, handleSave]);

  const parseDefinition = useCallback((def: Record<string, unknown> | string): Record<string, unknown> => {
    if (typeof def === 'string') {
      try { return JSON.parse(def); } catch { return {}; }
    }
    return def;
  }, []);

  const handleCompare = useCallback((versionNumber: number) => {
    setCompareVersion(versionNumber);
    setDiffModalOpen(true);
  }, []);

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
            {currentVersion && (
              <ApprovalBadge status={currentVersion.status} rejectionReason={currentVersion.rejectionReason} />
            )}
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
                  {v.status === 'DRAFT' ? '(Draft)' : v.status === 'PUBLISHED' ? '(Published)' : v.status === 'PENDING_APPROVAL' ? '(Pending)' : ''}
                </option>
              ))}
            </Select>
            <ExportRuleButton
              ruleId={id!}
              ruleName={rule.name}
            />
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
              <>
                <Button
                  variant="outline"
                  onClick={() => setRequestApprovalModalOpen(true)}
                >
                  Request Approval
                </Button>
                <Button
                  onClick={handlePublish}
                  loading={publishVersion.isPending}
                >
                  <Upload className="h-4 w-4" />
                  Publish
                </Button>
              </>
            )}
            {currentVersion?.status === 'PENDING_APPROVAL' && (
              <>
                <Button
                  onClick={handleApprove}
                  loading={approveVersion.isPending}
                >
                  Approve
                </Button>
                <Button
                  variant="outline"
                  onClick={() => { setRejectReason(''); setRejectModalOpen(true); }}
                >
                  Reject
                </Button>
              </>
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
          <Suspense fallback={<div className="flex items-center justify-center h-64"><Spinner size="lg" /></div>}>
            <div className="h-full flex flex-col">
              <div className="flex items-center justify-end px-4 py-2 border-b border-gray-100 shrink-0 bg-white">
                <ViewToggle
                  mode={viewMode}
                  onChange={(mode) => {
                    if (mode === 'model') handleSwitchToModel();
                    else setViewMode('designer');
                  }}
                />
              </div>
              <div className={`flex-1 overflow-hidden ${viewMode === 'model' ? '' : 'p-4'}`}>
                {viewMode === 'model' ? (
                  <div className="flex flex-col h-full">
                    {currentVersion?.status !== 'DRAFT' && (
                      <div className="flex items-center gap-2 px-4 py-2 border-b border-gray-200 bg-gray-50 shrink-0">
                        <Lock className="h-3.5 w-3.5 text-gray-400" />
                        <span className="text-xs text-gray-500">
                          Read-only · {currentVersion?.status} v{currentVersion?.version}
                        </span>
                        <button
                          className="ml-auto text-xs text-indigo-600 hover:text-indigo-700 flex items-center gap-1"
                          onClick={() => {
                            navigator.clipboard.writeText(modelJson);
                          }}
                        >
                          <Copy className="h-3 w-3" /> Copy JSON
                        </button>
                      </div>
                    )}
                    <div className="flex-1 min-h-0">
                      <JsonModelEditor
                        value={modelJson}
                        readOnly={currentVersion?.status !== 'DRAFT'}
                        onChange={currentVersion?.status === 'DRAFT'
                          ? (v) => { setModelJson(v); syncModelToDesigner(v); }
                          : undefined}
                        height="100%"
                      />
                    </div>
                    {currentVersion?.status === 'DRAFT' && (
                      <ModelViewStatusBar status={jsonStatus} />
                    )}
                  </div>
                ) : currentVersion?.status !== 'DRAFT' ? (
                  <div className="flex flex-col h-full">
                    <div className="flex items-center gap-2 px-4 py-2 border-b border-gray-200 bg-gray-50 shrink-0">
                      <Lock className="h-3.5 w-3.5 text-gray-400" />
                      <span className="text-xs text-gray-500">
                        Read-only · {currentVersion?.status} v{currentVersion?.version}
                      </span>
                    </div>
                    <div className="flex-1 min-h-0 overflow-hidden">
                      <RuleDefinitionViewer
                        ruleType={rule.type}
                        definition={parseDefinition(currentVersion?.definition as Record<string, unknown> | string ?? {})}
                        height="100%"
                      />
                    </div>
                  </div>
                ) : (
                  <>
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
                  </>
                )}
              </div>
            </div>
          </Suspense>
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
                        {selectedVersion !== null && v.version !== selectedVersion && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleCompare(v.version)}
                          >
                            <GitCompareArrows className="h-3.5 w-3.5" />
                            Compare
                          </Button>
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

      {/* Version Diff Modal */}
      {diffModalOpen && compareVersion !== null && currentVersion && (
        <VersionDiffModal
          isOpen={diffModalOpen}
          onClose={() => { setDiffModalOpen(false); setCompareVersion(null); }}
          versionA={{
            version: currentVersion.version,
            definition: parseDefinition(currentVersion.definition),
          }}
          versionB={{
            version: compareVersion,
            definition: parseDefinition(
              versions.find((v) => v.version === compareVersion)?.definition ?? {},
            ),
          }}
        />
      )}

      {/* Request Approval Modal */}
      {rule && requestApprovalModalOpen && (
        <RequestApprovalModal
          open={requestApprovalModalOpen}
          onClose={() => setRequestApprovalModalOpen(false)}
          rules={[rule]}
          onSubmitted={() => {
            setRequestApprovalModalOpen(false);
            addToast({ type: 'success', title: 'Approval requested successfully' });
          }}
        />
      )}

      {/* Reject Modal */}
      {rejectModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900">Reject Version</h3>
            <p className="mt-1 text-sm text-gray-500">
              Provide a reason for rejecting v{currentVersion?.version}.
            </p>
            <div className="mt-4">
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Rejection reason..."
                rows={3}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
              />
            </div>
            <div className="mt-4 flex justify-end gap-3">
              <Button variant="outline" onClick={() => setRejectModalOpen(false)}>
                Cancel
              </Button>
              <Button
                onClick={handleReject}
                loading={rejectVersion.isPending}
                disabled={!rejectReason.trim()}
              >
                Reject
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
