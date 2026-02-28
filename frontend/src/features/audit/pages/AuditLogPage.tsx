import { useState, useEffect, useMemo } from 'react';
import {
  Search, ChevronDown, ChevronUp, ChevronLeft, ChevronRight,
  Download, FileText, PanelLeftClose, PanelLeft, X,
  CheckCircle, XCircle, Plus, Edit, Play, LogIn, LogOut, Key,
  Clock, AlertCircle,
} from 'lucide-react';
import { Badge } from '@/shared/components/Badge';
import { Button } from '@/shared/components/Button';
import { EmptyState } from '@/shared/components/EmptyState';
import { SkeletonRow } from '@/components/Skeleton';
import { useAuditEvents, exportAuditEventsCsv, type AuditEventParams } from '@/api/audit.api';
import { useAuthStore } from '@/stores/auth.store';
import { formatRelativeTime } from '@/features/rules/utils/formatRelativeTime';
import { cn } from '@/shared/utils/cn';

// --- Constants ---

const PAGE_SIZE_OPTIONS = [25, 50, 100];

const EVENT_TYPE_GROUPS: Record<string, string[]> = {
  Rules: ['RULE_CREATED', 'RULE_UPDATED', 'RULE_PUBLISHED', 'RULE_REJECTED', 'RULE_DELETED'],
  Approvals: ['APPROVAL_REQUESTED', 'APPROVAL_APPROVED', 'APPROVAL_REJECTED'],
  Auth: ['USER_LOGIN', 'USER_LOGOUT', 'USER_PROFILE_UPDATED'],
  System: ['API_KEY_CREATED', 'API_KEY_REVOKED', 'FOLDER_CREATED', 'RULE_EXECUTED'],
};

const RESOURCE_TYPES = ['RULE', 'FOLDER', 'USER', 'API_KEY', 'APPROVAL'];

const EVENT_COLORS: Record<string, string> = {
  RULE_CREATED: 'text-blue-500',
  RULE_UPDATED: 'text-blue-400',
  RULE_PUBLISHED: 'text-green-500',
  RULE_APPROVED: 'text-green-500',
  APPROVAL_APPROVED: 'text-green-500',
  RULE_REJECTED: 'text-red-500',
  APPROVAL_REJECTED: 'text-red-500',
  RULE_DELETED: 'text-red-600',
  APPROVAL_REQUESTED: 'text-indigo-500',
  USER_LOGIN: 'text-gray-500',
  USER_LOGOUT: 'text-gray-400',
  USER_PROFILE_UPDATED: 'text-blue-400',
  RULE_EXECUTED: 'text-purple-500',
  API_KEY_CREATED: 'text-orange-500',
  API_KEY_REVOKED: 'text-red-400',
  FOLDER_CREATED: 'text-yellow-600',
};

const EVENT_VERBS: Record<string, string> = {
  RULE_CREATED: 'created',
  RULE_UPDATED: 'updated',
  RULE_PUBLISHED: 'published',
  RULE_REJECTED: 'rejected',
  RULE_DELETED: 'deleted',
  APPROVAL_REQUESTED: 'requested approval for',
  APPROVAL_APPROVED: 'approved',
  APPROVAL_REJECTED: 'rejected',
  USER_LOGIN: 'logged in',
  USER_LOGOUT: 'logged out',
  USER_PROFILE_UPDATED: 'updated profile',
  RULE_EXECUTED: 'executed',
  API_KEY_CREATED: 'created API key',
  API_KEY_REVOKED: 'revoked API key',
  FOLDER_CREATED: 'created folder',
};

function getEventIcon(eventType: string) {
  const size = 'h-4 w-4';
  switch (eventType) {
    case 'RULE_PUBLISHED':
    case 'RULE_APPROVED':
    case 'APPROVAL_APPROVED':
      return <CheckCircle className={size} />;
    case 'RULE_REJECTED':
    case 'APPROVAL_REJECTED':
    case 'RULE_DELETED':
      return <XCircle className={size} />;
    case 'RULE_CREATED':
    case 'FOLDER_CREATED':
      return <Plus className={size} />;
    case 'RULE_UPDATED':
    case 'USER_PROFILE_UPDATED':
      return <Edit className={size} />;
    case 'RULE_EXECUTED':
      return <Play className={size} />;
    case 'USER_LOGIN':
      return <LogIn className={size} />;
    case 'USER_LOGOUT':
      return <LogOut className={size} />;
    case 'API_KEY_CREATED':
    case 'API_KEY_REVOKED':
      return <Key className={size} />;
    case 'APPROVAL_REQUESTED':
      return <Clock className={size} />;
    default:
      return <AlertCircle className={size} />;
  }
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

function hashColor(str: string): string {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  const colors = [
    'bg-indigo-500', 'bg-blue-500', 'bg-green-500', 'bg-purple-500',
    'bg-pink-500', 'bg-orange-500', 'bg-teal-500', 'bg-cyan-500',
  ];
  return colors[Math.abs(hash) % colors.length];
}

function getDefaultFrom(): string {
  const d = new Date();
  d.setDate(d.getDate() - 7);
  return d.toISOString().split('T')[0];
}

// --- Component ---

export function AuditLogPage() {
  const token = useAuthStore((s) => s.token);

  // Filter state
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedEventTypes, setSelectedEventTypes] = useState<Set<string>>(new Set());
  const [resourceType, setResourceType] = useState('');
  const [dateFrom, setDateFrom] = useState(getDefaultFrom);
  const [dateTo, setDateTo] = useState('');

  // Pagination
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);

  // Expand state
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // Debounce search 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  // Reset page on filter change
  useEffect(() => {
    setPage(0);
  }, [selectedEventTypes, resourceType, dateFrom, dateTo]);

  // Build query params
  const queryParams = useMemo((): AuditEventParams => {
    const params: AuditEventParams = { page, size: pageSize };
    if (debouncedSearch) params.search = debouncedSearch;
    if (selectedEventTypes.size === 1) {
      params.eventType = [...selectedEventTypes][0];
    } else if (selectedEventTypes.size > 1) {
      params.eventType = [...selectedEventTypes].join(',');
    }
    if (resourceType) params.resourceType = resourceType;
    if (dateFrom) params.from = new Date(dateFrom).toISOString();
    if (dateTo) params.to = new Date(dateTo + 'T23:59:59').toISOString();
    return params;
  }, [page, pageSize, debouncedSearch, selectedEventTypes, resourceType, dateFrom, dateTo]);

  const { data, isLoading } = useAuditEvents(queryParams, !!token);

  const events = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  // Active filter count
  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (selectedEventTypes.size > 0) count++;
    if (resourceType) count++;
    if (dateFrom) count++;
    if (dateTo) count++;
    return count;
  }, [selectedEventTypes, resourceType, dateFrom, dateTo]);

  const handleToggleEventType = (type: string) => {
    setSelectedEventTypes((prev) => {
      const next = new Set(prev);
      if (next.has(type)) next.delete(type);
      else next.add(type);
      return next;
    });
  };

  const handleClearAll = () => {
    setSearch('');
    setSelectedEventTypes(new Set());
    setResourceType('');
    setDateFrom('');
    setDateTo('');
  };

  const handleExport = () => {
    const params: Omit<AuditEventParams, 'page' | 'size'> = {};
    if (debouncedSearch) params.search = debouncedSearch;
    if (selectedEventTypes.size > 0) params.eventType = [...selectedEventTypes].join(',');
    if (resourceType) params.resourceType = resourceType;
    if (dateFrom) params.from = new Date(dateFrom).toISOString();
    if (dateTo) params.to = new Date(dateTo + 'T23:59:59').toISOString();
    exportAuditEventsCsv(params);
  };

  const startIndex = page * pageSize + 1;
  const endIndex = Math.min((page + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="mb-4 flex items-start justify-between px-0">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Audit Log</h1>
          <p className="mt-1 text-sm text-gray-500">
            Track all changes and activities across your organization.
          </p>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden rounded-lg border border-gray-200 bg-white">
        {/* Filter Sidebar */}
        {sidebarOpen && (
          <aside className="w-[280px] flex-shrink-0 border-r border-gray-200 bg-gray-50/50 overflow-y-auto">
            <div className="p-4">
              {/* Sidebar header */}
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-gray-700">Filters</span>
                  {activeFilterCount > 0 && (
                    <span className="inline-flex items-center justify-center h-5 min-w-[20px] rounded-full bg-indigo-100 text-indigo-700 text-xs font-medium px-1.5">
                      {activeFilterCount}
                    </span>
                  )}
                </div>
                <button
                  onClick={() => setSidebarOpen(false)}
                  className="p-1 rounded text-gray-400 hover:text-gray-600 hover:bg-gray-200"
                  title="Collapse filters"
                >
                  <PanelLeftClose className="h-4 w-4" />
                </button>
              </div>

              {/* Search */}
              <div className="relative mb-4">
                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-gray-400" />
                <input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search by rule, user..."
                  className="block w-full h-8 rounded-md border border-gray-200 bg-white pl-8 pr-8 text-sm text-gray-900 placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
                {search && (
                  <button
                    onClick={() => setSearch('')}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                )}
              </div>

              {/* Event Type checkboxes */}
              <div className="mb-4">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-2">
                  Event Type
                </h3>
                {Object.entries(EVENT_TYPE_GROUPS).map(([group, types]) => (
                  <div key={group} className="mb-2">
                    <span className="text-[11px] font-medium text-gray-400 uppercase tracking-wide">
                      {group}
                    </span>
                    <div className="mt-1 space-y-0.5">
                      {types.map((type) => (
                        <label
                          key={type}
                          className="flex items-center gap-2 py-0.5 text-sm text-gray-700 cursor-pointer hover:text-gray-900"
                        >
                          <input
                            type="checkbox"
                            checked={selectedEventTypes.has(type)}
                            onChange={() => handleToggleEventType(type)}
                            className="h-3.5 w-3.5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                          />
                          <span className="text-xs">{type.replace(/_/g, ' ')}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                ))}
              </div>

              {/* Resource Type */}
              <div className="mb-4">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-2">
                  Resource Type
                </h3>
                <select
                  value={resourceType}
                  onChange={(e) => setResourceType(e.target.value)}
                  className="block w-full h-8 rounded-md border border-gray-200 bg-white px-2 text-sm text-gray-700 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value="">All Resources</option>
                  {RESOURCE_TYPES.map((rt) => (
                    <option key={rt} value={rt}>{rt}</option>
                  ))}
                </select>
              </div>

              {/* Date Range */}
              <div className="mb-4">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-2">
                  Date Range
                </h3>
                <div className="space-y-2">
                  <div>
                    <label className="text-[11px] text-gray-500">From</label>
                    <input
                      type="date"
                      value={dateFrom}
                      onChange={(e) => setDateFrom(e.target.value)}
                      className="block w-full h-8 rounded-md border border-gray-200 bg-white px-2 text-sm text-gray-700 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] text-gray-500">To</label>
                    <input
                      type="date"
                      value={dateTo}
                      onChange={(e) => setDateTo(e.target.value)}
                      className="block w-full h-8 rounded-md border border-gray-200 bg-white px-2 text-sm text-gray-700 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                  </div>
                </div>
              </div>

              {/* Clear All */}
              {activeFilterCount > 0 && (
                <button
                  onClick={handleClearAll}
                  className="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
                >
                  Clear all filters
                </button>
              )}
            </div>
          </aside>
        )}

        {/* Activity Feed */}
        <div className="flex-1 min-w-0 flex flex-col">
          {/* Feed Header */}
          <div className="flex items-center justify-between px-6 py-3 border-b border-gray-200 bg-gray-50/50">
            <div className="flex items-center gap-3">
              {!sidebarOpen && (
                <button
                  onClick={() => setSidebarOpen(true)}
                  className="p-1 rounded text-gray-400 hover:text-gray-600 hover:bg-gray-200"
                  title="Show filters"
                >
                  <PanelLeft className="h-4 w-4" />
                </button>
              )}
              <h2 className="text-sm font-semibold text-gray-700">Audit Log</h2>
              {!isLoading && (
                <span className="text-xs text-gray-400">{totalElements} events</span>
              )}
            </div>
            <Button variant="outline" size="sm" onClick={handleExport}>
              <Download className="h-3.5 w-3.5" />
              Export CSV
            </Button>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto">
            {isLoading ? (
              <div>
                {Array.from({ length: 10 }).map((_, i) => (
                  <SkeletonRow key={i} columns={4} />
                ))}
              </div>
            ) : events.length === 0 ? (
              <EmptyState
                icon={<FileText className="h-10 w-10" />}
                title="No events found"
                description={
                  activeFilterCount > 0 || debouncedSearch
                    ? 'Try adjusting your filters or search terms.'
                    : 'Audit events will appear here as actions are performed.'
                }
                actionLabel={activeFilterCount > 0 ? 'Clear filters' : undefined}
                onAction={activeFilterCount > 0 ? handleClearAll : undefined}
              />
            ) : (
              <div className="divide-y divide-gray-100">
                {events.map((event) => {
                  const hasChanges = event.changes && Object.keys(event.changes).length > 0;
                  const isExpanded = expandedId === event.id;
                  const actorDisplay = event.actorName || event.actorId?.slice(0, 8) || 'System';
                  const resourceDisplay = event.resourceName || event.resourceId;
                  const verb = EVENT_VERBS[event.eventType] || event.eventType.toLowerCase().replace(/_/g, ' ');
                  const colorClass = EVENT_COLORS[event.eventType] || 'text-gray-500';
                  const absoluteTime = new Date(event.createdAt).toLocaleString();

                  return (
                    <div key={event.id} className="px-6 py-3 hover:bg-gray-50/50 transition-colors">
                      <div className="flex items-start gap-3">
                        {/* Icon */}
                        <div className={cn('mt-0.5 flex-shrink-0', colorClass)}>
                          {getEventIcon(event.eventType)}
                        </div>

                        {/* Actor Avatar */}
                        <div
                          className={cn(
                            'flex-shrink-0 h-7 w-7 rounded-full flex items-center justify-center text-white text-[10px] font-semibold',
                            hashColor(actorDisplay),
                          )}
                          title={actorDisplay}
                        >
                          {getInitials(actorDisplay)}
                        </div>

                        {/* Content */}
                        <div className="flex-1 min-w-0">
                          <p className="text-sm text-gray-800">
                            <span className="font-medium">{actorDisplay}</span>
                            {' '}
                            <span className="text-gray-600">{verb}</span>
                            {resourceDisplay && (
                              <>
                                {' '}
                                <span className="font-medium text-gray-800">{resourceDisplay}</span>
                              </>
                            )}
                          </p>
                          <div className="flex items-center gap-2 mt-1">
                            <Badge className="text-[10px] bg-gray-100 text-gray-600">
                              {event.resourceType}
                            </Badge>
                            <Badge className="text-[10px] bg-gray-100 text-gray-500">
                              {event.eventType.replace(/_/g, ' ')}
                            </Badge>
                            <span
                              className="text-[11px] text-gray-400"
                              title={absoluteTime}
                            >
                              {formatRelativeTime(event.createdAt)}
                            </span>
                          </div>
                        </div>

                        {/* Expand toggle */}
                        {hasChanges && (
                          <button
                            onClick={() => setExpandedId(isExpanded ? null : event.id)}
                            className="flex-shrink-0 p-1 rounded text-gray-400 hover:text-gray-600 hover:bg-gray-100"
                            title={isExpanded ? 'Hide changes' : 'Show changes'}
                          >
                            {isExpanded ? (
                              <ChevronUp className="h-4 w-4" />
                            ) : (
                              <ChevronDown className="h-4 w-4" />
                            )}
                          </button>
                        )}
                      </div>

                      {/* Expanded changes */}
                      {isExpanded && hasChanges && (
                        <div className="mt-2 ml-14 rounded-md bg-gray-50 border border-gray-200 p-3">
                          <pre className="text-xs text-gray-700 overflow-x-auto whitespace-pre-wrap">
                            {JSON.stringify(event.changes, null, 2)}
                          </pre>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Pagination */}
          {totalPages > 0 && events.length > 0 && (
            <div className="flex items-center justify-between px-6 py-3 border-t border-gray-200 bg-gray-50/50 text-sm">
              <span className="text-gray-500">
                Showing {startIndex}–{endIndex} of {totalElements} events
              </span>
              <div className="flex items-center gap-3">
                <select
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(Number(e.target.value));
                    setPage(0);
                  }}
                  className="h-8 rounded border border-gray-200 bg-white px-2 text-sm focus:outline-none"
                >
                  {PAGE_SIZE_OPTIONS.map((size) => (
                    <option key={size} value={size}>{size} per page</option>
                  ))}
                </select>
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="h-8 w-8 flex items-center justify-center rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                  <span className="text-sm text-gray-600 px-2">
                    {page + 1} / {totalPages}
                  </span>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="h-8 w-8 flex items-center justify-center rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
