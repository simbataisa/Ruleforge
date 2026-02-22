import { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, ChevronDown, ChevronUp, Search } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Badge } from '@/shared/components/Badge';
import { DataTable } from '@/shared/components/DataTable';
import { SkeletonRow } from '@/components/Skeleton';
import { EmptyState } from '@/shared/components/EmptyState';
import { useAuditLogs, type AuditEvent } from '@/api/audit.api';

const PAGE_SIZE = 20;

const eventTypeColors: Record<string, string> = {
  RULE_CREATED: 'bg-green-100 text-green-800',
  RULE_UPDATED: 'bg-blue-100 text-blue-800',
  RULE_DELETED: 'bg-red-100 text-red-800',
  RULE_PUBLISHED: 'bg-indigo-100 text-indigo-800',
  USER_LOGIN: 'bg-yellow-100 text-yellow-800',
  USER_LOGOUT: 'bg-gray-100 text-gray-600',
};

const EVENT_TYPES = [
  'RULE_CREATED',
  'RULE_UPDATED',
  'RULE_DELETED',
  'RULE_PUBLISHED',
  'USER_LOGIN',
  'USER_LOGOUT',
];

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function AuditLogPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [eventTypeFilter, setEventTypeFilter] = useState<string>('');
  const [expandedRow, setExpandedRow] = useState<string | null>(null);

  // Debounce search input by 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const { data, isLoading } = useAuditLogs(page, PAGE_SIZE, eventTypeFilter || undefined, debouncedSearch || undefined);

  const events = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const columns = [
    {
      key: 'eventType',
      header: 'Event Type',
      render: (event: AuditEvent) => (
        <Badge className={eventTypeColors[event.eventType] ?? 'bg-gray-100 text-gray-800'}>
          {event.eventType}
        </Badge>
      ),
    },
    {
      key: 'resourceType',
      header: 'Resource',
      render: (event: AuditEvent) => (
        <span className="text-gray-700">
          {event.resourceType}
          {event.resourceId && (
            <span className="ml-1 text-gray-400 text-xs">({event.resourceId.slice(0, 8)}...)</span>
          )}
        </span>
      ),
    },
    {
      key: 'actorId',
      header: 'Actor',
      render: (event: AuditEvent) => (
        <span className="text-gray-600 text-xs font-mono">{event.actorId?.slice(0, 8)}...</span>
      ),
    },
    {
      key: 'createdAt',
      header: 'Timestamp',
      render: (event: AuditEvent) => (
        <span className="text-gray-500">{formatDate(event.createdAt)}</span>
      ),
    },
    {
      key: 'changes',
      header: 'Changes',
      render: (event: AuditEvent) => {
        const hasChanges = event.changes && Object.keys(event.changes).length > 0;
        if (!hasChanges) return <span className="text-gray-400">-</span>;
        return (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setExpandedRow(expandedRow === event.id ? null : event.id);
            }}
            className="flex items-center gap-1 text-indigo-600 hover:text-indigo-800 text-xs"
          >
            {expandedRow === event.id ? (
              <>
                <ChevronUp className="h-3 w-3" /> Hide
              </>
            ) : (
              <>
                <ChevronDown className="h-3 w-3" /> Show
              </>
            )}
          </button>
        );
      },
    },
  ];

  if (isLoading) {
    return (
      <div>
        <PageHeader
          title="Audit Log"
          description="Track all changes and activities across your organization."
        />
        <div className="rounded-lg border border-gray-200 bg-white">
          <div className="flex items-center gap-4 px-6 py-3 border-b border-gray-200">
            {['20%', '18%', '15%', '22%', '10%'].map((w, i) => (
              <div key={i} className="h-3 animate-pulse rounded bg-gray-300" style={{ width: w }} />
            ))}
          </div>
          {Array.from({ length: 10 }).map((_, i) => (
            <SkeletonRow key={i} columns={5} />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="Audit Log"
        description="Track all changes and activities across your organization."
      />

      <div className="flex items-center gap-3 mb-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search audit events..."
            className="pl-9"
          />
        </div>
        <Select
          value={eventTypeFilter}
          onChange={(e) => {
            setEventTypeFilter(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All Event Types</option>
          {EVENT_TYPES.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </Select>
      </div>

      {events.length === 0 ? (
        <EmptyState
          title="No audit events found"
          description={
            eventTypeFilter
              ? 'Try adjusting your filter.'
              : 'Audit events will appear here as actions are performed.'
          }
        />
      ) : (
        <>
          <DataTable data={events} columns={columns} />

          {/* Expanded changes row */}
          {expandedRow && (
            <div className="mt-2 mb-4 rounded-lg border border-gray-200 bg-gray-50 p-4">
              <h4 className="text-xs font-medium text-gray-500 mb-2">Changes Detail</h4>
              <pre className="text-xs text-gray-700 overflow-x-auto">
                {JSON.stringify(
                  events.find((e) => e.id === expandedRow)?.changes,
                  null,
                  2,
                )}
              </pre>
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-4 text-sm text-gray-600">
              <span>
                {totalElements} events &middot; Page {page + 1} of {totalPages}
              </span>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="p-1 rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="p-1 rounded hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
