import { useState } from 'react';
import { Check, X, User, Calendar, Layers } from 'lucide-react';
import { Badge } from '@/shared/components/Badge';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { useRuleVersions, useApproveVersion, useRejectVersion } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import { ApprovalCommentThread } from './ApprovalCommentThread';
import type { Rule } from '@/api/types/rule.types';

interface ApprovalDetailPanelProps {
  rule: Rule;
}

function JsonDiffView({
  current,
  previous,
}: {
  current: string;
  previous: string;
}) {
  const currentLines = current.split('\n');
  const previousLines = previous.split('\n');
  const previousSet = new Set(previousLines);
  const currentSet = new Set(currentLines);

  return (
    <div className="grid grid-cols-2 gap-2 text-xs font-mono">
      <div>
        <p className="mb-1 text-xs font-semibold text-gray-500 font-sans">
          Previous
        </p>
        <pre className="max-h-64 overflow-auto rounded-md border border-gray-200 bg-gray-50 p-3">
          {previousLines.map((line, i) => {
            const removed = !currentSet.has(line);
            return (
              <div
                key={i}
                className={removed ? 'bg-red-100 text-red-800' : 'text-gray-600'}
              >
                {line}
              </div>
            );
          })}
        </pre>
      </div>
      <div>
        <p className="mb-1 text-xs font-semibold text-gray-500 font-sans">
          Current
        </p>
        <pre className="max-h-64 overflow-auto rounded-md border border-gray-200 bg-gray-50 p-3">
          {currentLines.map((line, i) => {
            const added = !previousSet.has(line);
            return (
              <div
                key={i}
                className={added ? 'bg-green-100 text-green-800' : 'text-gray-600'}
              >
                {line}
              </div>
            );
          })}
        </pre>
      </div>
    </div>
  );
}

export function ApprovalDetailPanel({ rule }: ApprovalDetailPanelProps) {
  const addToast = useToastStore((s) => s.addToast);
  const approveVersion = useApproveVersion();
  const rejectVersion = useRejectVersion();
  const { data: versions, isLoading: versionsLoading } = useRuleVersions(rule.id);

  const [showRejectInput, setShowRejectInput] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

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

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="border-b border-gray-200 px-6 py-4">
        <div className="flex items-center gap-3">
          <h2 className="text-lg font-semibold text-gray-900">{rule.name}</h2>
          <Badge color="blue">v{rule.latestVersion}</Badge>
          <Badge color="gray">{rule.type}</Badge>
        </div>
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

        {/* Change note */}
        {currentVersion?.changeNote && (
          <div className="rounded-md border border-gray-200 bg-gray-50 p-3">
            <p className="text-xs font-semibold text-gray-500 mb-1">Change Note</p>
            <p className="text-sm text-gray-700">{currentVersion.changeNote}</p>
          </div>
        )}

        {/* Diff section */}
        <div>
          <h3 className="text-sm font-semibold text-gray-900 mb-3">
            Definition Changes
          </h3>
          {previousVersion ? (
            <JsonDiffView current={currentDefStr} previous={previousDefStr} />
          ) : (
            <div>
              <p className="mb-2 text-xs text-gray-500">
                First version -- no previous version to compare.
              </p>
              <pre className="max-h-64 overflow-auto rounded-md border border-gray-200 bg-gray-50 p-3 text-xs font-mono text-gray-600">
                {currentDefStr}
              </pre>
            </div>
          )}
        </div>

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
