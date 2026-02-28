import { useState, useMemo } from 'react';
import { Search, X, Info } from 'lucide-react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import {
  useEligibleApprovers,
  useSpaceApprovalSettings,
  useBatchRequestApproval,
} from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import type { Rule } from '@/api/types/rule.types';

interface RequestApprovalModalProps {
  open: boolean;
  onClose: () => void;
  rules: Rule[];
  onSubmitted: () => void;
}

function getInitials(name: string): string {
  return name
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

export function RequestApprovalModal({
  open,
  onClose,
  rules,
  onSubmitted,
}: RequestApprovalModalProps) {
  const addToast = useToastStore((s) => s.addToast);
  const batchRequest = useBatchRequestApproval();

  // Use the first rule to fetch eligible approvers and space settings
  const firstRule = rules[0];
  const { data: approvers, isLoading: approversLoading } = useEligibleApprovers(
    firstRule?.id ?? '',
  );
  const { data: spaceSettings } = useSpaceApprovalSettings(
    firstRule?.spaceId ?? '',
  );

  const [selectedApproverIds, setSelectedApproverIds] = useState<Set<string>>(new Set());
  const [approverSearch, setApproverSearch] = useState('');
  const [note, setNote] = useState('');

  // Filter out non-DRAFT rules
  const draftRules = useMemo(() => rules.filter((r) => r.status === 'DRAFT'), [rules]);
  const skippedCount = rules.length - draftRules.length;

  // Pre-populate default approvers when data loads
  const allApprovers = useMemo(() => approvers ?? [], [approvers]);

  // Auto-select default approvers on first load
  useState(() => {
    if (allApprovers.length > 0 && selectedApproverIds.size === 0) {
      const defaults = allApprovers
        .filter((a) => a.isDefaultApprover)
        .map((a) => a.memberId);
      if (defaults.length > 0) {
        setSelectedApproverIds(new Set(defaults));
      }
    }
  });

  // Filter approvers by search
  const filteredApprovers = useMemo(() => {
    if (!approverSearch) return allApprovers;
    const q = approverSearch.toLowerCase();
    return allApprovers.filter(
      (a) =>
        a.name.toLowerCase().includes(q) ||
        a.email.toLowerCase().includes(q),
    );
  }, [allApprovers, approverSearch]);

  const toggleApprover = (id: string) => {
    setSelectedApproverIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const removeApprover = (id: string) => {
    setSelectedApproverIds((prev) => {
      const next = new Set(prev);
      next.delete(id);
      return next;
    });
  };

  const selectedApprovers = allApprovers.filter((a) =>
    selectedApproverIds.has(a.memberId),
  );

  const handleSubmit = () => {
    if (draftRules.length === 0) return;

    batchRequest.mutate(
      {
        ruleIds: draftRules.map((r) => r.id),
        assigneeIds: Array.from(selectedApproverIds),
        note: note.trim() || undefined,
      },
      {
        onSuccess: (result) => {
          const count = result.succeeded?.length ?? draftRules.length;
          addToast({
            type: 'success',
            title: `Approval requested for ${count} rule${count !== 1 ? 's' : ''}`,
          });
          if (result.failed && result.failed.length > 0) {
            addToast({
              type: 'warning',
              title: `${result.failed.length} rule(s) failed: ${result.failed.map((f) => f.reason).join('; ')}`,
            });
          }
          handleClose();
          onSubmitted();
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to request approval' });
        },
      },
    );
  };

  const handleClose = () => {
    setSelectedApproverIds(new Set());
    setApproverSearch('');
    setNote('');
    onClose();
  };

  const slaHours = spaceSettings?.slaHours ?? 48;

  return (
    <Modal
      open={open}
      onClose={handleClose}
      title={
        draftRules.length === 1
          ? 'Request Approval'
          : `Request Approval for ${draftRules.length} Rules`
      }
      className="max-w-[520px]"
      footer={
        <>
          <Button variant="ghost" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={selectedApproverIds.size === 0 || draftRules.length === 0}
            loading={batchRequest.isPending}
          >
            {batchRequest.isPending ? 'Requesting...' : 'Request Approval'}
          </Button>
        </>
      }
    >
      {/* Rule info */}
      {draftRules.length === 1 ? (
        <div className="mb-4">
          <p className="text-base font-semibold text-gray-900">{draftRules[0].name}</p>
          <p className="text-sm text-gray-500">Version v{draftRules[0].latestVersion}</p>
        </div>
      ) : (
        <div className="mb-4">
          <div className="max-h-[160px] overflow-y-auto rounded-lg border border-gray-200 divide-y divide-gray-100">
            {draftRules.map((rule) => (
              <div key={rule.id} className="flex items-center gap-2 px-3 py-2 text-sm">
                <span className="inline-flex items-center rounded px-1.5 py-0.5 text-[10px] font-medium bg-gray-100 text-gray-600">
                  {rule.type}
                </span>
                <span className="font-medium text-gray-900 truncate">{rule.name}</span>
                <span className="ml-auto text-gray-400 text-xs">v{rule.latestVersion}</span>
              </div>
            ))}
          </div>
          {skippedCount > 0 && (
            <p className="mt-2 text-xs text-amber-600">
              Note: {skippedCount} rule{skippedCount !== 1 ? 's were' : ' was'} skipped
              (not in DRAFT status).
            </p>
          )}
        </div>
      )}

      <div className="border-t border-gray-200 my-4" />

      {/* Assignee picker */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Assign to approvers
        </label>
        <div className="rounded-lg border border-gray-200 overflow-hidden">
          {/* Search input */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-gray-400" />
            <input
              value={approverSearch}
              onChange={(e) => setApproverSearch(e.target.value)}
              placeholder="Search members..."
              className="w-full h-9 pl-9 pr-3 text-sm border-b border-gray-200 focus:outline-none focus:ring-1 focus:ring-inset focus:ring-indigo-500"
            />
          </div>

          {/* Approver list */}
          <div className="max-h-[180px] overflow-y-auto">
            {approversLoading ? (
              <div className="px-3 py-4 text-center text-sm text-gray-400">
                Loading approvers...
              </div>
            ) : filteredApprovers.length === 0 ? (
              <div className="px-3 py-4 text-center text-sm text-gray-400">
                No approvers found
              </div>
            ) : (
              filteredApprovers.map((approver) => (
                <label
                  key={approver.memberId}
                  className="flex items-center gap-3 px-3 py-2.5 hover:bg-gray-50 cursor-pointer"
                >
                  <input
                    type="checkbox"
                    checked={selectedApproverIds.has(approver.memberId)}
                    onChange={() => toggleApprover(approver.memberId)}
                    className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                  <div className="flex h-6 w-6 items-center justify-center rounded-full bg-indigo-100 text-[10px] font-medium text-indigo-700">
                    {getInitials(approver.name)}
                  </div>
                  <span className="text-sm text-gray-900 flex-1">{approver.name}</span>
                  <span className="text-[11px] text-gray-500">{approver.role}</span>
                  {approver.isDefaultApprover && (
                    <span className="inline-flex items-center rounded px-1.5 py-0.5 text-[10px] font-medium bg-indigo-50 text-indigo-600">
                      suggested
                    </span>
                  )}
                </label>
              ))
            )}
          </div>
        </div>

        {/* Selected chips */}
        {selectedApprovers.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-2">
            {selectedApprovers.map((a) => (
              <span
                key={a.memberId}
                className="inline-flex items-center gap-1 rounded-full bg-gray-100 pl-1 pr-2 py-0.5 text-xs text-gray-700"
              >
                <span className="flex h-4 w-4 items-center justify-center rounded-full bg-indigo-100 text-[8px] font-medium text-indigo-700">
                  {getInitials(a.name)}
                </span>
                {a.name}
                <button
                  onClick={() => removeApprover(a.memberId)}
                  className="ml-0.5 rounded-full p-0.5 hover:bg-gray-200 text-gray-400 hover:text-gray-600"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Note textarea */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          Note to approvers (optional)
        </label>
        <div className="relative">
          <textarea
            value={note}
            onChange={(e) => {
              if (e.target.value.length <= 500) setNote(e.target.value);
            }}
            placeholder="Add context for the reviewers..."
            rows={3}
            maxLength={500}
            className="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 resize-none"
          />
          <span
            className={`absolute bottom-2 right-2 text-[11px] ${
              note.length >= 480 ? 'text-red-500' : 'text-gray-400'
            }`}
          >
            {note.length}/500
          </span>
        </div>
      </div>

      {/* SLA info */}
      <div className="flex items-center gap-1.5 text-[13px] text-gray-500">
        <Info className="h-3.5 w-3.5 text-gray-400" />
        <span>SLA: {slaHours} hours (from space settings)</span>
      </div>
    </Modal>
  );
}
