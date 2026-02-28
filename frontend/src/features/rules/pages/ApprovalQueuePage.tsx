import { useState, useEffect, useCallback, useMemo } from 'react';
import { Check, X, ClipboardCheck, Clock, Search } from 'lucide-react';
import { Badge } from '@/shared/components/Badge';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import {
  useApprovalRequests,
  useApproveVersion,
  useRejectVersion,
  useCancelApprovalRequest,
} from '@/api/rules.api';
import type { ApprovalRequestDto, ApprovalAssigneeDto } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import { useAuthStore } from '@/stores/auth.store';
import { ApprovalDetailPanel } from '../components/ApprovalDetailPanel';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/shared/utils/cn';
import type { Rule } from '@/api/types/rule.types';

type TabId = 'my-queue' | 'my-requests' | 'all-requests';

function slaHoursRemaining(deadline: string | null): number | null {
  if (!deadline) return null;
  return (new Date(deadline).getTime() - Date.now()) / (1000 * 60 * 60);
}

function SlaBar({ deadline }: { deadline: string | null }) {
  const remaining = slaHoursRemaining(deadline);
  if (remaining == null) return <span className="text-xs text-gray-400">No SLA</span>;

  const totalHours = 48;
  const elapsed = totalHours - remaining;
  const pct = Math.min(100, Math.max(0, (elapsed / totalHours) * 100));

  let barColor = 'bg-green-500';
  let textColor = 'text-green-600';
  let label = `${Math.round(remaining)}h remaining`;

  if (remaining <= 0) {
    barColor = 'bg-red-500';
    textColor = 'text-red-500';
    label = `Overdue by ${Math.abs(Math.round(remaining))}h`;
  } else if (remaining < totalHours * 0.25) {
    barColor = 'bg-red-500';
    textColor = 'text-red-500';
  } else if (remaining < totalHours * 0.5) {
    barColor = 'bg-amber-500';
    textColor = 'text-amber-600';
  }

  return (
    <div className="flex items-center gap-2">
      <div className="w-20 h-1 rounded-full bg-gray-200 overflow-hidden">
        <div className={cn('h-full rounded-full', barColor)} style={{ width: `${pct}%` }} />
      </div>
      <span className={cn('text-[11px] font-medium', textColor)}>{label}</span>
    </div>
  );
}

function AvatarStack({ assignees }: { assignees: ApprovalAssigneeDto[] }) {
  const shown = assignees.slice(0, 3);
  const overflow = assignees.length - 3;

  return (
    <div className="flex items-center -space-x-1.5">
      {shown.map((a) => (
        <div
          key={a.memberId}
          className={cn(
            'flex h-6 w-6 items-center justify-center rounded-full border-2 border-white text-[9px] font-medium',
            a.status === 'APPROVED'
              ? 'bg-green-100 text-green-700'
              : a.status === 'REJECTED'
                ? 'bg-red-100 text-red-700'
                : 'bg-indigo-100 text-indigo-700',
          )}
          title={`${a.name ?? 'Unknown'} - ${a.status}`}
        >
          {(a.name ?? '?')
            .split(' ')
            .map((p) => p[0])
            .slice(0, 2)
            .join('')
            .toUpperCase()}
        </div>
      ))}
      {overflow > 0 && (
        <div className="flex h-6 w-6 items-center justify-center rounded-full border-2 border-white bg-gray-100 text-[9px] font-medium text-gray-600">
          +{overflow}
        </div>
      )}
    </div>
  );
}

function ApprovalStatusBadge({ status }: { status: string }) {
  switch (status) {
    case 'PENDING':
      return <Badge color="yellow">Pending</Badge>;
    case 'APPROVED':
      return <Badge color="green">Approved</Badge>;
    case 'REJECTED':
      return <Badge color="red">Rejected</Badge>;
    case 'CANCELLED':
      return <Badge color="gray">Cancelled</Badge>;
    default:
      return <Badge color="gray">{status}</Badge>;
  }
}

function toRuleLike(req: ApprovalRequestDto): Rule {
  return {
    id: req.ruleId,
    spaceId: '',
    name: req.ruleName,
    type: req.ruleType as Rule['type'],
    status: 'DRAFT',
    latestVersion: req.version,
    locked: false,
    createdAt: req.requestedAt,
    updatedAt: req.requestedAt,
  };
}

export function ApprovalQueuePage() {
  const addToast = useToastStore((s) => s.addToast);
  const user = useAuthStore((s) => s.user);
  const userRole = user?.role ?? '';
  const approveVersion = useApproveVersion();
  const rejectVersion = useRejectVersion();
  const cancelRequest = useCancelApprovalRequest();
  const navigate = useNavigate();

  const canSeeMyQueue = ['OWNER', 'ADMIN', 'APPROVER'].includes(userRole);
  const canSeeAllRequests = ['OWNER', 'ADMIN'].includes(userRole);

  const [activeTab, setActiveTab] = useState<TabId>('my-requests');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [nameFilter, setNameFilter] = useState('');
  const [selectedRequestId, setSelectedRequestId] = useState<string | null>(null);
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  const queryParams = useMemo(() => {
    const params: Record<string, unknown> = { size: 50 };
    if (activeTab === 'my-queue') params.assignedToMe = true;
    if (activeTab === 'my-requests') params.requestedByMe = true;
    if (statusFilter) params.status = statusFilter;
    else if (activeTab === 'my-queue') params.status = 'PENDING';
    return params;
  }, [activeTab, statusFilter]);

  const { data, isLoading } = useApprovalRequests(queryParams as never);
  const allRequests: ApprovalRequestDto[] = data?.content ?? [];
  const requests = useMemo(() => {
    if (!nameFilter.trim()) return allRequests;
    const q = nameFilter.toLowerCase();
    return allRequests.filter((r) => r.ruleName?.toLowerCase().includes(q));
  }, [allRequests, nameFilter]);

  const selectedRequest = requests.find((r) => r.id === selectedRequestId) ?? null;

  useEffect(() => {
    if (requests.length > 0 && !selectedRequestId) {
      setSelectedRequestId(requests[0].id);
    }
  }, [requests, selectedRequestId]);

  useEffect(() => {
    setSelectedRequestId(null);
    setStatusFilter('');
    setNameFilter('');
  }, [activeTab]);

  useEffect(() => {
    if (canSeeMyQueue && activeTab === 'my-requests') {
      setActiveTab('my-queue');
    }
  }, [canSeeMyQueue]); // eslint-disable-line react-hooks/exhaustive-deps

  const pendingCount = allRequests.filter((r) => r.status === 'PENDING').length;
  const overdueCount = allRequests.filter((r) => {
    const rem = slaHoursRemaining(r.deadline);
    return r.status === 'PENDING' && rem != null && rem <= 0;
  }).length;
  const approvedTodayCount = allRequests.filter((r) => {
    if (r.status !== 'APPROVED' || !r.resolvedAt) return false;
    const resolved = new Date(r.resolvedAt);
    const today = new Date();
    return resolved.toDateString() === today.toDateString();
  }).length;

  const navigateItem = useCallback(
    (direction: 'next' | 'prev') => {
      if (requests.length === 0) return;
      const idx = requests.findIndex((r) => r.id === selectedRequestId);
      if (direction === 'next') {
        const next = idx < requests.length - 1 ? idx + 1 : 0;
        setSelectedRequestId(requests[next].id);
      } else {
        const prev = idx > 0 ? idx - 1 : requests.length - 1;
        setSelectedRequestId(requests[prev].id);
      }
    },
    [requests, selectedRequestId],
  );

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) || target.isContentEditable) return;
      if (e.key === 'j') { e.preventDefault(); navigateItem('next'); }
      if (e.key === 'k') { e.preventDefault(); navigateItem('prev'); }
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [navigateItem]);

  const handleApprove = (req: ApprovalRequestDto) => {
    approveVersion.mutate(
      { ruleId: req.ruleId, version: req.version },
      {
        onSuccess: () => { addToast({ type: 'success', title: `Approved ${req.ruleName}` }); },
        onError: () => addToast({ type: 'error', title: 'Failed to approve' }),
      },
    );
  };

  const handleReject = (req: ApprovalRequestDto, reason: string) => {
    if (!reason.trim()) return;
    rejectVersion.mutate(
      { ruleId: req.ruleId, version: req.version, reason: reason.trim() },
      {
        onSuccess: () => {
          addToast({ type: 'success', title: `Rejected ${req.ruleName}` });
          setRejectingId(null);
          setRejectReason('');
        },
        onError: () => addToast({ type: 'error', title: 'Failed to reject' }),
      },
    );
  };

  const handleCancel = (req: ApprovalRequestDto) => {
    if (!confirm(`Cancel approval request for "${req.ruleName}"?`)) return;
    cancelRequest.mutate(req.id, {
      onSuccess: () => { addToast({ type: 'success', title: `Cancelled request for ${req.ruleName}` }); },
      onError: () => addToast({ type: 'error', title: 'Failed to cancel request' }),
    });
  };

  const tabs: { id: TabId; label: string; visible: boolean; count?: number }[] = [
    { id: 'my-queue', label: 'My Queue', visible: canSeeMyQueue, count: pendingCount > 0 ? pendingCount : undefined },
    { id: 'my-requests', label: 'My Requests', visible: true, count: activeTab === 'my-requests' && pendingCount > 0 ? pendingCount : undefined },
    { id: 'all-requests', label: 'All Requests', visible: canSeeAllRequests },
  ];

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
      <div className="mb-4">
        <h1 className="text-2xl font-bold text-gray-900">Approvals</h1>

        {/* Tabs */}
        <div className="flex items-center gap-1 mt-3 border-b border-gray-200">
          {tabs
            .filter((t) => t.visible)
            .map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={cn(
                  'relative px-4 py-2 text-sm font-medium transition-colors',
                  activeTab === tab.id
                    ? 'text-indigo-600 border-b-2 border-indigo-600'
                    : 'text-gray-500 hover:text-gray-700',
                )}
              >
                {tab.label}
                {tab.count != null && tab.count > 0 && (
                  <span className="ml-1.5 inline-flex items-center justify-center rounded-full bg-indigo-100 px-1.5 py-0.5 text-[10px] font-semibold text-indigo-600">
                    {tab.count}
                  </span>
                )}
              </button>
            ))}
        </div>

        {/* Summary bar */}
        <div className="flex items-center gap-4 mt-2 text-sm">
          <span className="text-gray-700">
            <span className="font-semibold">{pendingCount}</span> pending
          </span>
          {overdueCount > 0 && (
            <>
              <span className="text-gray-300">&middot;</span>
              <span className="text-red-500">
                <span className="font-semibold">{overdueCount}</span> overdue
              </span>
            </>
          )}
          {approvedTodayCount > 0 && (
            <>
              <span className="text-gray-300">&middot;</span>
              <span className="text-green-600">
                <span className="font-semibold">{approvedTodayCount}</span> approved today
              </span>
            </>
          )}
        </div>
      </div>

      {/* Filters bar */}
      <div className="mb-3 flex items-center gap-2 flex-wrap">
        {/* Search */}
        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-gray-400 pointer-events-none" />
          <input
            value={nameFilter}
            onChange={(e) => setNameFilter(e.target.value)}
            placeholder="Search by rule name..."
            className="h-8 rounded-lg border border-gray-200 bg-white pl-8 pr-3 text-sm text-gray-700 focus:border-indigo-500 focus:outline-none w-52"
          />
        </div>
        {/* Status pills */}
        <div className="flex items-center gap-1">
          {['', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'].map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={cn(
                'h-7 rounded-full px-3 text-xs font-medium transition-colors',
                statusFilter === s
                  ? 'bg-indigo-100 text-indigo-700 border border-indigo-300'
                  : 'bg-white text-gray-500 border border-gray-200 hover:bg-gray-50',
              )}
            >
              {s === '' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
        <span className="text-xs text-gray-400 ml-auto">J/K to navigate</span>
      </div>

      {/* Content */}
      {requests.length === 0 ? (
        <EmptyState
          title={
            activeTab === 'my-queue'
              ? 'All caught up'
              : activeTab === 'my-requests'
                ? 'No pending requests'
                : 'No approval requests'
          }
          description={
            activeTab === 'my-queue'
              ? 'No approval requests assigned to you.'
              : activeTab === 'my-requests'
                ? 'When you submit rules for approval, they will appear here.'
                : 'Requests will appear here when rules are submitted for review.'
          }
        />
      ) : activeTab === 'all-requests' ? (
        /* ALL REQUESTS -- Split view: table + detail panel */
        <div className="flex flex-1 gap-0 overflow-hidden rounded-lg border border-gray-200 bg-white">
          {/* Table */}
          <div className={`flex flex-col overflow-hidden transition-all ${selectedRequest ? 'flex-1 min-w-0' : 'flex-1'}`}>
            <div className="overflow-auto flex-1">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="bg-gray-50 border-b border-gray-200">
                    <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 min-w-[200px]">Rule</th>
                    <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[80px]">Version</th>
                    <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[160px]">Requested by</th>
                    <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[120px]">Requested at</th>
                    <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[140px]">Assignees</th>
                    <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[140px]">SLA</th>
                    <th className="px-4 py-3 text-xs font-semibold uppercase tracking-wider text-gray-500 w-[100px]">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {requests.map((req) => (
                    <tr
                      key={req.id}
                      className={cn(
                        'hover:bg-gray-50 cursor-pointer transition-colors',
                        selectedRequestId === req.id && 'bg-indigo-50',
                      )}
                      onClick={() => setSelectedRequestId(req.id)}
                    >
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-gray-900">{req.ruleName}</span>
                          <Badge className="bg-gray-100 text-gray-600 text-[10px]">{req.ruleType}</Badge>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-gray-600">v{req.version}</td>
                      <td className="px-4 py-3 text-gray-700 text-[13px]">{req.requestedByName}</td>
                      <td className="px-4 py-3 text-gray-500 text-[13px]">{formatRelativeTime(req.requestedAt)}</td>
                      <td className="px-4 py-3"><AvatarStack assignees={req.assignees} /></td>
                      <td className="px-4 py-3"><SlaBar deadline={req.deadline} /></td>
                      <td className="px-4 py-3"><ApprovalStatusBadge status={req.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Detail panel — slides in when a row is selected */}
          {selectedRequest && (
            <div className="w-[440px] shrink-0 border-l border-gray-200 flex flex-col overflow-hidden">
              {/* Panel header with close */}
              <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-gray-50 shrink-0">
                <div className="flex items-center gap-2">
                  <AvatarStack assignees={selectedRequest.assignees} />
                  <span className="text-xs text-gray-500">
                    {selectedRequest.assignees.length} assignee{selectedRequest.assignees.length !== 1 ? 's' : ''}
                  </span>
                </div>
                <button
                  onClick={() => setSelectedRequestId(null)}
                  className="p-1 rounded hover:bg-gray-200 transition-colors"
                  title="Close panel"
                >
                  <X className="h-4 w-4 text-gray-500" />
                </button>
              </div>
              <div className="flex-1 min-h-0 overflow-hidden">
                <ApprovalDetailPanel rule={toRuleLike(selectedRequest)} onOpenInEditor={(id) => navigate(`/rules/${id}/edit`)} />
              </div>
            </div>
          )}
        </div>
      ) : (
        /* MY QUEUE / MY REQUESTS -- Two-panel layout */
        <div className="flex flex-1 gap-0 overflow-hidden rounded-lg border border-gray-200 bg-white">
          {/* Left panel -- queue list */}
          <div className="w-[380px] shrink-0 border-r border-gray-200 flex flex-col">
            <div className="flex-1 overflow-y-auto">
              {requests.map((req) => {
                const isSelected = selectedRequestId === req.id;
                const slaHrs = slaHoursRemaining(req.deadline);
                const slaAccentColor = req.status !== 'PENDING'
                  ? 'border-l-gray-200'
                  : slaHrs == null
                    ? 'border-l-transparent'
                    : slaHrs <= 0
                      ? 'border-l-red-500'
                      : slaHrs < 12
                        ? 'border-l-amber-400'
                        : 'border-l-green-400';
                return (
                  <div
                    key={req.id}
                    className={cn(
                      'cursor-pointer border-b border-gray-100 px-4 py-3 transition-colors border-l-[3px]',
                      slaAccentColor,
                      isSelected ? 'bg-indigo-50' : 'hover:bg-gray-50',
                    )}
                    onClick={() => setSelectedRequestId(req.id)}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <Badge className="bg-gray-100 text-gray-600 text-[10px]">{req.ruleType}</Badge>
                      <span className="truncate text-sm font-semibold text-gray-900">{req.ruleName}</span>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-gray-500 mb-1.5">
                      <span>
                        {activeTab === 'my-requests' ? 'Submitted' : `Requested by: ${req.requestedByName}`}
                      </span>
                      <span>&middot;</span>
                      <span>{formatRelativeTime(req.requestedAt)}</span>
                    </div>
                    {activeTab === 'my-queue' && (
                      <div className="flex items-center gap-3 mb-1.5">
                        <span className="text-xs text-gray-400">Assignees:</span>
                        <AvatarStack assignees={req.assignees} />
                      </div>
                    )}
                    <SlaBar deadline={req.deadline} />

                    {/* Inline actions */}
                    <div className="flex items-center gap-2 mt-2">
                      <ApprovalStatusBadge status={req.status} />
                      {activeTab === 'my-queue' && req.status === 'PENDING' && (
                        <>
                          <Button
                            size="sm"
                            className="ml-auto h-7 bg-green-600 hover:bg-green-700 text-white text-xs"
                            onClick={(e) => { e.stopPropagation(); handleApprove(req); }}
                            loading={approveVersion.isPending}
                          >
                            <Check className="h-3 w-3" /> Approve
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-7 border-red-300 text-red-600 hover:bg-red-50 text-xs"
                            onClick={(e) => { e.stopPropagation(); setRejectingId(req.id); }}
                          >
                            <X className="h-3 w-3" /> Reject
                          </Button>
                        </>
                      )}
                      {activeTab === 'my-requests' && req.status === 'PENDING' && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="ml-auto h-7 text-xs"
                          onClick={(e) => { e.stopPropagation(); handleCancel(req); }}
                          loading={cancelRequest.isPending}
                        >
                          Cancel
                        </Button>
                      )}
                    </div>

                    {/* Reject reason inline */}
                    {rejectingId === req.id && (
                      <div className="mt-2 flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                        <input
                          value={rejectReason}
                          onChange={(e) => setRejectReason(e.target.value)}
                          placeholder="Rejection reason..."
                          className="flex-1 rounded border border-red-300 px-2 py-1 text-xs focus:border-red-500 focus:ring-1 focus:ring-red-500"
                          autoFocus
                          onKeyDown={(e) => {
                            if (e.key === 'Enter' && rejectReason.trim()) handleReject(req, rejectReason);
                            if (e.key === 'Escape') { setRejectingId(null); setRejectReason(''); }
                          }}
                        />
                        <Button
                          size="sm"
                          variant="danger"
                          className="h-6 text-xs"
                          disabled={!rejectReason.trim()}
                          onClick={() => handleReject(req, rejectReason)}
                        >
                          Reject
                        </Button>
                        <button
                          className="text-xs text-gray-400 hover:text-gray-600"
                          onClick={() => { setRejectingId(null); setRejectReason(''); }}
                        >
                          Cancel
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right panel -- detail */}
          <div className="flex-1 min-w-0 flex flex-col">
            {selectedRequest ? (
              <>
                {/* Assignee section */}
                <div className="border-b border-gray-200 px-6 py-4">
                  <h4 className="text-sm font-semibold text-gray-700 mb-3">Assignees</h4>
                  <div className="space-y-2">
                    {selectedRequest.assignees.map((a) => (
                      <div key={a.memberId} className="flex items-center gap-3">
                        <div
                          className={cn(
                            'flex h-8 w-8 items-center justify-center rounded-full text-xs font-medium',
                            a.status === 'APPROVED'
                              ? 'bg-green-100 text-green-700'
                              : a.status === 'REJECTED'
                                ? 'bg-red-100 text-red-700'
                                : 'bg-indigo-100 text-indigo-700',
                          )}
                        >
                          {(a.name ?? '?').split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()}
                        </div>
                        <span className="text-sm font-medium text-gray-900">{a.name ?? 'Unknown'}</span>
                        <div className="ml-auto flex items-center gap-1.5">
                          {a.status === 'PENDING' && (
                            <>
                              <Clock className="h-3.5 w-3.5 text-amber-500" />
                              <span className="text-xs text-amber-600">Pending</span>
                            </>
                          )}
                          {a.status === 'APPROVED' && (
                            <>
                              <Check className="h-3.5 w-3.5 text-green-500" />
                              <span className="text-xs text-green-600">Approved</span>
                              {a.respondedAt && (
                                <span className="text-xs text-gray-400 ml-1">{formatRelativeTime(a.respondedAt)}</span>
                              )}
                            </>
                          )}
                          {a.status === 'REJECTED' && (
                            <>
                              <X className="h-3.5 w-3.5 text-red-500" />
                              <span className="text-xs text-red-600">Rejected</span>
                              {a.respondedAt && (
                                <span className="text-xs text-gray-400 ml-1">{formatRelativeTime(a.respondedAt)}</span>
                              )}
                            </>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                  {selectedRequest.note && (
                    <div className="mt-3 rounded-md bg-gray-50 border border-gray-200 p-2.5">
                      <p className="text-[11px] font-semibold text-gray-500 mb-0.5">Note from requester</p>
                      <p className="text-sm text-gray-700">{selectedRequest.note}</p>
                    </div>
                  )}
                </div>
                <div className="flex-1 min-h-0">
                  <ApprovalDetailPanel rule={toRuleLike(selectedRequest)} onOpenInEditor={(id) => navigate(`/rules/${id}/edit`)} />
                </div>
              </>
            ) : (
              <div className="flex h-full items-center justify-center">
                <div className="text-center">
                  <ClipboardCheck className="mx-auto h-10 w-10 text-gray-300" />
                  <p className="mt-2 text-sm text-gray-500">Select a request to view details</p>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
