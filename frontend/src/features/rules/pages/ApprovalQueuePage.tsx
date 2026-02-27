import { useState, useEffect, useCallback } from 'react';
import { Check, X, ClipboardCheck } from 'lucide-react';
import { Badge } from '@/shared/components/Badge';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { useRules, useApproveVersion, useRejectVersion } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import { ApprovalDetailPanel } from '../components/ApprovalDetailPanel';
import type { Rule } from '@/api/types/rule.types';

function formatTimeAgo(dateStr: string): string {
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  const diffMs = now - then;
  const seconds = Math.floor(diffMs / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

const SLA_HOURS = 48; // 48h SLA window from submission

function slaHoursRemaining(createdAt: string): number {
  const submitted = new Date(createdAt).getTime();
  const deadline = submitted + SLA_HOURS * 60 * 60 * 1000;
  return (deadline - Date.now()) / (1000 * 60 * 60);
}

function SlaIndicator({ createdAt }: { createdAt: string }) {
  const remaining = slaHoursRemaining(createdAt);
  if (remaining <= 0) return <Badge color="red">Overdue</Badge>;
  if (remaining < 2) return <Badge color="red">Due in {Math.ceil(remaining)}h</Badge>;
  if (remaining < 24) return <Badge color="yellow">Due in {Math.round(remaining)}h</Badge>;
  return <Badge color="green">Due in {Math.round(remaining)}h</Badge>;
}

export function ApprovalQueuePage() {
  const addToast = useToastStore((s) => s.addToast);
  const approveVersion = useApproveVersion();
  const rejectVersion = useRejectVersion();

  const { data: rulesData, isLoading } = useRules({
    status: 'PENDING_APPROVAL',
  } as never);

  // Sort by urgency: most overdue (oldest) first
  const rules: Rule[] = (rulesData?.content ?? []).slice().sort(
    (a, b) => slaHoursRemaining(a.createdAt) - slaHoursRemaining(b.createdAt),
  );

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [checkedIds, setCheckedIds] = useState<Set<string>>(new Set());
  const [bulkRejectOpen, setBulkRejectOpen] = useState(false);
  const [bulkRejectReason, setBulkRejectReason] = useState('');
  const [keyboardRejectOpen, setKeyboardRejectOpen] = useState(false);
  const [keyboardRejectReason, setKeyboardRejectReason] = useState('');

  const selectedRule = rules.find((r) => r.id === selectedId) ?? null;
  const selectedIndex = rules.findIndex((r) => r.id === selectedId);

  // Auto-select first item
  useEffect(() => {
    if (rules.length > 0 && !selectedId) {
      setSelectedId(rules[0].id);
    }
  }, [rules, selectedId]);

  const navigateItem = useCallback(
    (direction: 'next' | 'prev') => {
      if (rules.length === 0) return;
      if (direction === 'next') {
        const nextIdx =
          selectedIndex < rules.length - 1 ? selectedIndex + 1 : 0;
        setSelectedId(rules[nextIdx].id);
      } else {
        const prevIdx =
          selectedIndex > 0 ? selectedIndex - 1 : rules.length - 1;
        setSelectedId(rules[prevIdx].id);
      }
    },
    [rules, selectedIndex],
  );

  const handleBulkApprove = () => {
    checkedIds.forEach((ruleId) => {
      const rule = rules.find((r) => r.id === ruleId);
      if (!rule) return;
      approveVersion.mutate(
        { ruleId, version: rule.latestVersion },
        {
          onSuccess: () => {
            addToast({ type: 'success', title: `Approved ${rule.name}` });
          },
        },
      );
    });
    setCheckedIds(new Set());
  };

  const handleBulkReject = () => {
    if (!bulkRejectReason.trim()) return;
    checkedIds.forEach((ruleId) => {
      const rule = rules.find((r) => r.id === ruleId);
      if (!rule) return;
      rejectVersion.mutate(
        { ruleId, version: rule.latestVersion, reason: bulkRejectReason.trim() },
        {
          onSuccess: () => {
            addToast({ type: 'success', title: `Rejected ${rule.name}` });
          },
        },
      );
    });
    setCheckedIds(new Set());
    setBulkRejectOpen(false);
    setBulkRejectReason('');
  };

  // Keyboard shortcuts
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      if (
        target.tagName === 'INPUT' ||
        target.tagName === 'TEXTAREA' ||
        target.tagName === 'SELECT' ||
        target.isContentEditable
      )
        return;

      switch (e.key.toLowerCase()) {
        case 'j':
        case 'n':
          e.preventDefault();
          navigateItem('next');
          break;
        case 'k':
        case 'p':
          e.preventDefault();
          navigateItem('prev');
          break;
        case 'a':
          if (selectedRule) {
            e.preventDefault();
            approveVersion.mutate(
              { ruleId: selectedRule.id, version: selectedRule.latestVersion },
              {
                onSuccess: () =>
                  addToast({ type: 'success', title: 'Version approved' }),
                onError: () =>
                  addToast({ type: 'error', title: 'Failed to approve' }),
              },
            );
          }
          break;
        case 'r':
          if (selectedRule) {
            e.preventDefault();
            setKeyboardRejectOpen(true);
          }
          break;
      }
    };

    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [navigateItem, selectedRule, approveVersion, addToast, keyboardRejectOpen]);

  const toggleChecked = (id: string) => {
    setCheckedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleAll = () => {
    if (checkedIds.size === rules.length) {
      setCheckedIds(new Set());
    } else {
      setCheckedIds(new Set(rules.map((r) => r.id)));
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-8rem)]">
      {/* Header */}
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Approval Queue</h1>
          <p className="mt-1 text-sm text-gray-500">
            Review and approve rule versions pending publication.
            <span className="ml-2 text-xs text-gray-400">
              Shortcuts: J/K navigate, A approve, R reject
            </span>
          </p>
        </div>
        {rules.length > 0 && (
          <Badge color="yellow">{rules.length} pending</Badge>
        )}
      </div>

      {rules.length === 0 ? (
        <div className="flex flex-1 items-center justify-center rounded-lg border border-gray-200 bg-white">
          <div className="text-center">
            <ClipboardCheck className="mx-auto h-12 w-12 text-gray-300" />
            <p className="mt-3 text-gray-500">No rules pending approval.</p>
            <p className="text-sm text-gray-400">All caught up!</p>
          </div>
        </div>
      ) : (
        <div className="flex flex-1 gap-0 overflow-hidden rounded-lg border border-gray-200 bg-white">
          {/* Left panel -- queue list */}
          <div className="w-[360px] shrink-0 border-r border-gray-200 flex flex-col">
            {/* Select all header */}
            <div className="flex items-center gap-3 border-b border-gray-200 px-4 py-2 bg-gray-50">
              <input
                type="checkbox"
                checked={checkedIds.size === rules.length && rules.length > 0}
                onChange={toggleAll}
                className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
              />
              <span className="text-xs text-gray-500">
                {checkedIds.size > 0
                  ? `${checkedIds.size} selected`
                  : 'Select all'}
              </span>
            </div>

            {/* Scrollable list */}
            <div className="flex-1 overflow-y-auto">
              {rules.map((rule) => (
                <div
                  key={rule.id}
                  className={`flex items-start gap-3 cursor-pointer border-b border-gray-100 px-4 py-3 transition-colors ${
                    selectedId === rule.id
                      ? 'bg-indigo-50 border-l-2 border-l-indigo-600'
                      : 'hover:bg-gray-50 border-l-2 border-l-transparent'
                  }`}
                  onClick={() => setSelectedId(rule.id)}
                >
                  <input
                    type="checkbox"
                    checked={checkedIds.has(rule.id)}
                    onChange={(e) => {
                      e.stopPropagation();
                      toggleChecked(rule.id);
                    }}
                    onClick={(e) => e.stopPropagation()}
                    className="mt-1 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="truncate text-sm font-medium text-gray-900">
                        {rule.name}
                      </span>
                      <Badge color="blue">{rule.type}</Badge>
                    </div>
                    <div className="mt-1 flex items-center gap-2 text-xs text-gray-500">
                      <span>{formatTimeAgo(rule.createdAt)}</span>
                      <SlaIndicator createdAt={rule.createdAt} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Right panel -- detail */}
          <div className="flex-1 min-w-0 flex flex-col">
            {/* Inline keyboard-triggered reject bar */}
            {keyboardRejectOpen && selectedRule && (
              <div className="flex items-center gap-2 border-b border-red-200 bg-red-50 px-4 py-2">
                <span className="text-sm text-red-700">Reject {selectedRule.name}:</span>
                <input
                  type="text"
                  value={keyboardRejectReason}
                  onChange={(e) => setKeyboardRejectReason(e.target.value)}
                  placeholder="Rejection reason (required)..."
                  className="flex-1 rounded-md border border-red-300 px-3 py-1.5 text-sm focus:border-red-500 focus:ring-1 focus:ring-red-500"
                  autoFocus
                  onKeyDown={(e) => {
                    if (e.key === 'Escape') {
                      setKeyboardRejectOpen(false);
                      setKeyboardRejectReason('');
                    }
                    if (e.key === 'Enter' && keyboardRejectReason.trim()) {
                      rejectVersion.mutate(
                        {
                          ruleId: selectedRule.id,
                          version: selectedRule.latestVersion,
                          reason: keyboardRejectReason.trim(),
                        },
                        {
                          onSuccess: () => {
                            addToast({ type: 'success', title: 'Version rejected' });
                            setKeyboardRejectOpen(false);
                            setKeyboardRejectReason('');
                          },
                          onError: () => {
                            addToast({ type: 'error', title: 'Failed to reject' });
                          },
                        },
                      );
                    }
                  }}
                />
                <Button
                  size="sm"
                  variant="danger"
                  disabled={!keyboardRejectReason.trim()}
                  onClick={() => {
                    if (!keyboardRejectReason.trim()) return;
                    rejectVersion.mutate(
                      {
                        ruleId: selectedRule.id,
                        version: selectedRule.latestVersion,
                        reason: keyboardRejectReason.trim(),
                      },
                      {
                        onSuccess: () => {
                          addToast({ type: 'success', title: 'Version rejected' });
                          setKeyboardRejectOpen(false);
                          setKeyboardRejectReason('');
                        },
                        onError: () => {
                          addToast({ type: 'error', title: 'Failed to reject' });
                        },
                      },
                    );
                  }}
                >
                  Reject
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setKeyboardRejectOpen(false);
                    setKeyboardRejectReason('');
                  }}
                >
                  Cancel
                </Button>
              </div>
            )}
            <div className="flex-1 min-h-0">
              {selectedRule ? (
                <ApprovalDetailPanel rule={selectedRule} />
              ) : (
                <div className="flex h-full items-center justify-center">
                  <div className="text-center">
                    <ClipboardCheck className="mx-auto h-10 w-10 text-gray-300" />
                    <p className="mt-2 text-sm text-gray-500">
                      Select an approval request
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Bulk action floating bar */}
      {checkedIds.size > 0 && (
        <div className="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 flex items-center gap-3 rounded-lg border border-gray-200 bg-white px-5 py-3 shadow-lg">
          <span className="text-sm font-medium text-gray-700">
            {checkedIds.size} selected
          </span>
          <Button size="sm" onClick={handleBulkApprove} className="bg-green-600 hover:bg-green-700 text-white">
            <Check className="h-4 w-4" />
            Approve {checkedIds.size} selected
          </Button>
          {bulkRejectOpen ? (
            <div className="flex items-center gap-2">
              <input
                type="text"
                value={bulkRejectReason}
                onChange={(e) => setBulkRejectReason(e.target.value)}
                placeholder="Rejection reason..."
                className="rounded-md border border-gray-300 px-3 py-1.5 text-sm"
                autoFocus
              />
              <Button
                size="sm"
                variant="danger"
                onClick={handleBulkReject}
                disabled={!bulkRejectReason.trim()}
              >
                Confirm
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  setBulkRejectOpen(false);
                  setBulkRejectReason('');
                }}
              >
                Cancel
              </Button>
            </div>
          ) : (
            <Button
              size="sm"
              variant="outline"
              onClick={() => setBulkRejectOpen(true)}
              className="border-red-300 text-red-600 hover:bg-red-50"
            >
              <X className="h-4 w-4" />
              Reject {checkedIds.size} selected
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
