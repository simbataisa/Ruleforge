import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, X } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { Spinner } from '@/shared/components/Spinner';
import { useRules, useApproveVersion, useRejectVersion } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function ApprovalQueuePage() {
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const approveVersion = useApproveVersion();
  const rejectVersion = useRejectVersion();

  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectTarget, setRejectTarget] = useState<{ ruleId: string; version: number; ruleName: string } | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  // Fetch rules — the API supports status filter for listing
  const { data: rulesData, isLoading } = useRules({ status: 'PENDING_APPROVAL' } as never);

  const handleApprove = (ruleId: string, version: number) => {
    if (!confirm('Are you sure you want to approve this version?')) return;
    approveVersion.mutate(
      { ruleId, version },
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

  const openRejectModal = (ruleId: string, version: number, ruleName: string) => {
    setRejectTarget({ ruleId, version, ruleName });
    setRejectReason('');
    setRejectModalOpen(true);
  };

  const handleReject = () => {
    if (!rejectTarget) return;
    rejectVersion.mutate(
      { ruleId: rejectTarget.ruleId, version: rejectTarget.version, reason: rejectReason },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: 'Version rejected' });
          setRejectModalOpen(false);
          setRejectTarget(null);
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to reject version' });
        },
      },
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  const rules = rulesData?.content ?? [];

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Approval Queue</h1>
        <p className="mt-1 text-sm text-gray-500">
          Review and approve rule versions pending publication.
        </p>
      </div>

      {rules.length === 0 ? (
        <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
          <p className="text-gray-500">No rules pending approval.</p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  Rule Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  Version
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  Created
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {rules.map((rule) => (
                <tr key={rule.id} className="hover:bg-gray-50">
                  <td className="whitespace-nowrap px-6 py-4">
                    <button
                      className="font-medium text-indigo-600 hover:text-indigo-800 hover:underline"
                      onClick={() => navigate(`/rules/${rule.id}/edit`)}
                    >
                      {rule.name}
                    </button>
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                    v{rule.latestVersion}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4">
                    <Badge color="blue">{rule.type}</Badge>
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-gray-500">
                    {formatDate(rule.createdAt)}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <Button
                        size="sm"
                        onClick={() => handleApprove(rule.id, rule.latestVersion)}
                        loading={approveVersion.isPending}
                      >
                        <Check className="h-4 w-4" />
                        Approve
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => openRejectModal(rule.id, rule.latestVersion, rule.name)}
                      >
                        <X className="h-4 w-4" />
                        Reject
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Reject Modal */}
      {rejectModalOpen && rejectTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900">
              Reject Version
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              Rejecting v{rejectTarget.version} of &quot;{rejectTarget.ruleName}&quot;.
            </p>
            <div className="mt-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Rejection Reason
              </label>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Provide a reason for rejection..."
                rows={3}
                className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
              />
            </div>
            <div className="mt-4 flex justify-end gap-3">
              <Button
                variant="outline"
                onClick={() => {
                  setRejectModalOpen(false);
                  setRejectTarget(null);
                }}
              >
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
