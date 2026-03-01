import { useState } from 'react';
import { Check, X, User, Calendar, Layers, ExternalLink, Maximize2, FileText, AlertCircle } from 'lucide-react';
import { DiffEditor } from '@monaco-editor/react';
import { Badge } from '@/shared/components/Badge';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { cn } from '@/shared/utils/cn';
import { useRuleVersions, useApproveVersion, useRejectVersion } from '@/api/rules.api';
import type { ApprovalRequestDto } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import { ApprovalCommentThread } from './ApprovalCommentThread';
import { ViewToggle } from './ViewToggle';
import { JsonModelEditor } from './JsonModelEditor';
import { RuleDefinitionViewer } from './RuleDefinitionViewer';
import { MonacoDiffModal } from './MonacoDiffModal';
import type { Rule } from '@/api/types/rule.types';

const RULE_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
  TABLE: { bg: 'bg-blue-100', text: 'text-blue-700' },
  TREE: { bg: 'bg-emerald-100', text: 'text-emerald-700' },
  SCRIPT: { bg: 'bg-orange-100', text: 'text-orange-700' },
  FLOW: { bg: 'bg-violet-100', text: 'text-violet-700' },
  SCORECARD: { bg: 'bg-teal-100', text: 'text-teal-700' },
  DMN: { bg: 'bg-indigo-100', text: 'text-indigo-700' },
  ENSEMBLE: { bg: 'bg-rose-100', text: 'text-rose-700' },
  CEP: { bg: 'bg-amber-100', text: 'text-amber-700' },
  HIERARCHY: { bg: 'bg-cyan-100', text: 'text-cyan-700' },
};

interface ApprovalDetailPanelProps {
  rule: Rule;
  approvalRequest?: ApprovalRequestDto;
  onOpenInEditor?: (ruleId: string) => void;
}

export function ApprovalDetailPanel({ rule, approvalRequest, onOpenInEditor }: ApprovalDetailPanelProps) {
  const addToast = useToastStore((s) => s.addToast);
  const approveVersion = useApproveVersion();
  const rejectVersion = useRejectVersion();
  const { data: versions, isLoading: versionsLoading } = useRuleVersions(rule.id);

  const [showRejectInput, setShowRejectInput] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [defViewMode, setDefViewMode] = useState<'designer' | 'model'>('model');
  const [modelSubMode, setModelSubMode] = useState<'diff' | 'full'>('diff');
  const [diffModalOpen, setDiffModalOpen] = useState(false);

  const currentVersion = versions?.find(
    (v) => v.version === rule.latestVersion,
  );
  const previousVersion = versions?.find(
    (v) => v.version === rule.latestVersion - 1,
  );

  const handleApprove = () => {
    approveVersion.mutate(
      { ruleId: rule.id, version: rule.latestVersion },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Version approved and published' });
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to approve version' });
        },
      },
    );
  };

  const handleReject = () => {
    if (!rejectReason.trim()) return;
    rejectVersion.mutate(
      { ruleId: rule.id, version: rule.latestVersion, reason: rejectReason.trim() },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Version rejected' });
          setShowRejectInput(false);
          setRejectReason('');
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to reject version' });
        },
      },
    );
  };

  if (versionsLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <Spinner size="lg" />
      </div>
    );
  }

  const currentDefStr = currentVersion?.definition
    ? JSON.stringify(
        typeof currentVersion.definition === 'string'
          ? JSON.parse(currentVersion.definition)
          : currentVersion.definition,
        null,
        2,
      )
    : '{}';

  const previousDefStr = previousVersion?.definition
    ? JSON.stringify(
        typeof previousVersion.definition === 'string'
          ? JSON.parse(previousVersion.definition)
          : previousVersion.definition,
        null,
        2,
      )
    : '{}';

  let parsedCurrentDef: Record<string, unknown> = {};
  try { parsedCurrentDef = JSON.parse(currentDefStr); } catch { /* keep empty */ }

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="border-b border-gray-200 px-6 py-4">
        <div className="flex items-center gap-3">
          <h2 className="text-lg font-semibold text-gray-900">{rule.name}</h2>
          <Badge color="blue">v{rule.latestVersion}</Badge>
          {(() => {
            const colors = RULE_TYPE_COLORS[rule.type] ?? { bg: 'bg-gray-100', text: 'text-gray-600' };
            return (
              <span className={cn('inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold', colors.bg, colors.text)}>
                {rule.type}
              </span>
            );
          })()}
          {onOpenInEditor && (
            <Button
              size="sm"
              variant="outline"
              className="ml-auto"
              onClick={() => onOpenInEditor(rule.id)}
            >
              Open in Editor
              <ExternalLink className="h-3.5 w-3.5 ml-1" />
            </Button>
          )}
        </div>
        {approvalRequest?.requestedByName && (
          <p className="mt-1.5 text-xs text-gray-500">
            <User className="inline h-3 w-3 mr-1 -mt-0.5" />
            Submitted by {approvalRequest.requestedByName}
          </p>
        )}
      </div>

      {/* Scrollable content */}
      <div className="flex-1 overflow-y-auto px-6 py-4 space-y-6">
        {/* Rule metadata */}
        <div className="grid grid-cols-3 gap-4 text-sm">
          <div className="flex items-center gap-2 text-gray-500">
            <Layers className="h-4 w-4" />
            <span>Space: {rule.spaceId || 'Default'}</span>
          </div>
          <div className="flex items-center gap-2 text-gray-500">
            <User className="h-4 w-4" />
            <span>Submitted by: {currentVersion?.changeNote || 'Unknown'}</span>
          </div>
          <div className="flex items-center gap-2 text-gray-500">
            <Calendar className="h-4 w-4" />
            <span>
              {currentVersion
                ? new Date(currentVersion.createdAt).toLocaleDateString('en-US', {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })
                : '--'}
            </span>
          </div>
        </div>

        {/* Change note callout */}
        {currentVersion?.changeNote && (
          <div className="rounded-md border border-amber-200 bg-amber-50 p-3">
            <p className="text-xs font-semibold text-amber-700 mb-1 flex items-center gap-1">
              <FileText className="h-3 w-3" />
              What changed
            </p>
            <p className="text-sm text-amber-900">{currentVersion.changeNote}</p>
          </div>
        )}

        {/* Definition Changes section */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-gray-900">Definition Changes</h3>
            <div className="flex items-center gap-2">
              {defViewMode === 'model' && previousVersion && (
                <div className="flex rounded-md border border-gray-200 overflow-hidden text-xs">
                  {(['full', 'diff'] as const).map((m) => (
                    <button
                      key={m}
                      onClick={() => setModelSubMode(m)}
                      className={cn(
                        'px-2 py-1 font-medium transition-colors',
                        modelSubMode === m
                          ? 'bg-indigo-100 text-indigo-700'
                          : 'bg-white text-gray-500 hover:bg-gray-50'
                      )}
                    >
                      {m === 'full' ? 'Full' : 'Diff'}
                    </button>
                  ))}
                </div>
              )}
              {defViewMode === 'model' && modelSubMode === 'diff' && previousVersion && (
                <button
                  onClick={() => setDiffModalOpen(true)}
                  title="Fullscreen diff"
                  className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors"
                >
                  <Maximize2 className="h-3.5 w-3.5" />
                </button>
              )}
              <ViewToggle
                mode={defViewMode}
                onChange={(mode) => {
                  setDefViewMode(mode);
                  if (mode === 'model') setModelSubMode(previousVersion ? 'diff' : 'full');
                }}
              />
            </div>
          </div>

          {defViewMode === 'model' && modelSubMode === 'diff' && previousVersion ? (
            <DiffEditor
              height="320px"
              language="json"
              theme="vs"
              original={previousDefStr}
              modified={currentDefStr}
              options={{
                renderSideBySide: true,
                readOnly: true,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                automaticLayout: true,
                fontSize: 13,
                wordWrap: 'on',
              }}
            />
          ) : defViewMode === 'model' && modelSubMode === 'diff' && !previousVersion ? (
            <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-gray-300 bg-gray-50 py-12">
              <AlertCircle className="h-8 w-8 text-gray-400 mb-3" />
              <p className="text-sm font-medium text-gray-600">This is version 1</p>
              <p className="text-xs text-gray-400 mt-1">No previous version to compare</p>
            </div>
          ) : defViewMode === 'model' ? (
            <JsonModelEditor readOnly value={currentDefStr} height="320px" />
          ) : (
            <div
              className="rounded-lg border border-gray-200 overflow-hidden"
              style={{ height: '320px' }}
            >
              <RuleDefinitionViewer
                ruleType={rule.type}
                definition={parsedCurrentDef}
                height="320px"
              />
            </div>
          )}
        </div>

        {diffModalOpen && previousVersion && (
          <MonacoDiffModal
            isOpen={diffModalOpen}
            onClose={() => setDiffModalOpen(false)}
            original={previousDefStr}
            modified={currentDefStr}
            originalLabel={`v${previousVersion.version} (Previous)`}
            modifiedLabel={`v${currentVersion!.version} (Pending)`}
          />
        )}

        {/* Comment thread */}
        <ApprovalCommentThread ruleId={rule.id} version={rule.latestVersion} />
      </div>

      {/* Action bar */}
      <div className="border-t border-gray-200 px-6 py-4">
        {showRejectInput ? (
          <div className="space-y-3">
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Provide a reason for rejection..."
              rows={2}
              className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm placeholder:text-gray-400 focus:border-red-500 focus:ring-1 focus:ring-red-500"
              autoFocus
            />
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant="danger"
                onClick={handleReject}
                disabled={!rejectReason.trim()}
                loading={rejectVersion.isPending}
              >
                Confirm Reject
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  setShowRejectInput(false);
                  setRejectReason('');
                }}
              >
                Cancel
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <Button
              onClick={handleApprove}
              loading={approveVersion.isPending}
              className="bg-green-600 hover:bg-green-700 text-white"
            >
              <Check className="h-4 w-4" />
              Approve
            </Button>
            <Button
              variant="outline"
              onClick={() => setShowRejectInput(true)}
              className="border-red-300 text-red-600 hover:bg-red-50"
            >
              <X className="h-4 w-4" />
              Reject
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
