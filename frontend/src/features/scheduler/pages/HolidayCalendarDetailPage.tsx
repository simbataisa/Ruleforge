import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Trash2, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { Input } from '@/shared/components/Input';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import { cn } from '@/shared/utils/cn';
import {
  useHolidayCalendar,
  useHolidayEntries,
  useCreateHolidayEntry,
  useDeleteHolidayEntry,
  type HolidayEntry,
  type EntryType,
} from '@/api/holiday-calendars.api';

const MARKET_FLAGS: Record<string, string> = {
  US: '\u{1F1FA}\u{1F1F8}',
  UK: '\u{1F1EC}\u{1F1E7}',
  GB: '\u{1F1EC}\u{1F1E7}',
  SG: '\u{1F1F8}\u{1F1EC}',
  HK: '\u{1F1ED}\u{1F1F0}',
  JP: '\u{1F1EF}\u{1F1F5}',
  AU: '\u{1F1E6}\u{1F1FA}',
  DE: '\u{1F1E9}\u{1F1EA}',
  FR: '\u{1F1EB}\u{1F1F7}',
  CN: '\u{1F1E8}\u{1F1F3}',
  IN: '\u{1F1EE}\u{1F1F3}',
  KR: '\u{1F1F0}\u{1F1F7}',
  CA: '\u{1F1E8}\u{1F1E6}',
  BR: '\u{1F1E7}\u{1F1F7}',
};

const ENTRY_TYPE_COLORS: Record<EntryType, { bg: string; text: string; dot: string }> = {
  PUBLIC_HOLIDAY: { bg: 'bg-red-100', text: 'text-red-700', dot: 'bg-red-500' },
  AD_HOC: { bg: 'bg-orange-100', text: 'text-orange-700', dot: 'bg-orange-500' },
  DAY_IN_LIEU: { bg: 'bg-blue-100', text: 'text-blue-700', dot: 'bg-blue-500' },
  COMPANY_HOLIDAY: { bg: 'bg-purple-100', text: 'text-purple-700', dot: 'bg-purple-500' },
};

const ENTRY_TYPES: EntryType[] = ['PUBLIC_HOLIDAY', 'AD_HOC', 'DAY_IN_LIEU', 'COMPANY_HOLIDAY'];

const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

function getDaysInMonth(year: number, month: number) {
  return new Date(year, month + 1, 0).getDate();
}

function getFirstDayOfWeek(year: number, month: number) {
  const day = new Date(year, month, 1).getDay();
  // Convert Sunday=0 to Monday-based: Mon=0, Tue=1, ..., Sun=6
  return day === 0 ? 6 : day - 1;
}

function formatDate(dateStr: string) {
  return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

export function HolidayCalendarDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);

  const { data: calendar, isLoading } = useHolidayCalendar(id ?? '');
  const { data: entries } = useHolidayEntries(id ?? '');
  const createEntry = useCreateHolidayEntry();
  const deleteEntry = useDeleteHolidayEntry();

  const now = new Date();
  const [viewYear, setViewYear] = useState(now.getFullYear());
  const [viewMonth, setViewMonth] = useState(now.getMonth());

  // Add entry form
  const [newDate, setNewDate] = useState('');
  const [newName, setNewName] = useState('');
  const [newType, setNewType] = useState<EntryType>('PUBLIC_HOLIDAY');
  const [newRecurring, setNewRecurring] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);

  const entriesList = entries ?? [];

  // Map entries by date for the calendar grid
  const entriesByDate = useMemo(() => {
    const map = new Map<string, HolidayEntry[]>();
    for (const entry of entriesList) {
      const existing = map.get(entry.date) ?? [];
      existing.push(entry);
      map.set(entry.date, existing);
    }
    return map;
  }, [entriesList]);

  // Sorted entries for the table
  const sortedEntries = useMemo(
    () => [...entriesList].sort((a, b) => a.date.localeCompare(b.date)),
    [entriesList],
  );

  const weekendDaysSet = useMemo(
    () => new Set((calendar?.weekendDays ?? ['SATURDAY', 'SUNDAY']).map((d) => d.toUpperCase())),
    [calendar],
  );

  const dayNameToWeekendIndex: Record<string, number> = {
    MONDAY: 0,
    TUESDAY: 1,
    WEDNESDAY: 2,
    THURSDAY: 3,
    FRIDAY: 4,
    SATURDAY: 5,
    SUNDAY: 6,
  };

  const isWeekend = (dayOfWeek: number) => {
    for (const [name, idx] of Object.entries(dayNameToWeekendIndex)) {
      if (idx === dayOfWeek && weekendDaysSet.has(name)) return true;
    }
    return false;
  };

  const handlePrevMonth = () => {
    if (viewMonth === 0) {
      setViewMonth(11);
      setViewYear(viewYear - 1);
    } else {
      setViewMonth(viewMonth - 1);
    }
  };

  const handleNextMonth = () => {
    if (viewMonth === 11) {
      setViewMonth(0);
      setViewYear(viewYear + 1);
    } else {
      setViewMonth(viewMonth + 1);
    }
  };

  const handleAddEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !newDate || !newName.trim()) return;

    try {
      await createEntry.mutateAsync({
        calendarId: id,
        data: {
          date: newDate,
          name: newName.trim(),
          entryType: newType,
          recurring: newRecurring,
        },
      });
      addToast({ type: 'success', title: 'Holiday entry added' });
      setNewDate('');
      setNewName('');
      setNewType('PUBLIC_HOLIDAY');
      setNewRecurring(false);
      setShowAddForm(false);
    } catch {
      addToast({ type: 'error', title: 'Failed to add entry' });
    }
  };

  const handleDeleteEntry = async (entryId: string) => {
    if (!id) return;
    try {
      await deleteEntry.mutateAsync({ calendarId: id, entryId });
      addToast({ type: 'success', title: 'Entry deleted' });
    } catch {
      addToast({ type: 'error', title: 'Failed to delete entry' });
    }
  };

  if (isLoading || !calendar) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  const flag = MARKET_FLAGS[calendar.market.toUpperCase()] ?? '';
  const daysInMonth = getDaysInMonth(viewYear, viewMonth);
  const firstDay = getFirstDayOfWeek(viewYear, viewMonth);
  const monthLabel = new Date(viewYear, viewMonth).toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric',
  });

  // Build calendar grid cells
  const cells: { day: number; dateStr: string; entries: HolidayEntry[]; isWeekend: boolean }[] = [];
  for (let d = 1; d <= daysInMonth; d++) {
    const mm = String(viewMonth + 1).padStart(2, '0');
    const dd = String(d).padStart(2, '0');
    const dateStr = `${viewYear}-${mm}-${dd}`;
    const dayOfWeek = (firstDay + d - 1) % 7;
    cells.push({
      day: d,
      dateStr,
      entries: entriesByDate.get(dateStr) ?? [],
      isWeekend: isWeekend(dayOfWeek),
    });
  }

  return (
    <div>
      {/* Back */}
      <button
        onClick={() => navigate('/schedules')}
        className="mb-4 flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to Scheduler
      </button>

      {/* Header */}
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-1">
          <h1 className="text-2xl font-bold text-gray-900">
            {flag && <span className="mr-2">{flag}</span>}
            {calendar.name}
          </h1>
          <Badge color={calendar.enabled ? 'green' : 'gray'}>
            {calendar.enabled ? 'Enabled' : 'Disabled'}
          </Badge>
        </div>
        <div className="flex items-center gap-3 text-sm text-gray-500">
          <span>Market: {calendar.market}</span>
          {calendar.region && <span>Region: {calendar.region}</span>}
          {calendar.description && (
            <span className="text-gray-400">{calendar.description}</span>
          )}
        </div>
      </div>

      {/* Month Calendar Grid */}
      <div className="rounded-lg border border-gray-200 bg-white p-4 mb-6">
        <div className="flex items-center justify-between mb-4">
          <button
            onClick={handlePrevMonth}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          <h2 className="text-lg font-semibold text-gray-900">{monthLabel}</h2>
          <button
            onClick={handleNextMonth}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
          >
            <ChevronRight className="h-5 w-5" />
          </button>
        </div>

        {/* Weekday headers */}
        <div className="grid grid-cols-7 gap-px mb-1">
          {WEEKDAYS.map((wd) => (
            <div key={wd} className="text-center text-xs font-medium text-gray-500 py-1">
              {wd}
            </div>
          ))}
        </div>

        {/* Day cells */}
        <div className="grid grid-cols-7 gap-px">
          {/* Empty cells before first day */}
          {Array.from({ length: firstDay }).map((_, i) => (
            <div key={`empty-${i}`} className="h-16" />
          ))}
          {cells.map((cell) => (
            <div
              key={cell.day}
              className={cn(
                'h-16 rounded p-1 text-sm',
                cell.isWeekend ? 'bg-gray-50' : 'bg-white',
                cell.entries.length > 0 && 'ring-1 ring-inset ring-gray-200',
              )}
            >
              <span className={cn(
                'text-xs font-medium',
                cell.isWeekend ? 'text-gray-400' : 'text-gray-700',
              )}>
                {cell.day}
              </span>
              {cell.entries.length > 0 && (
                <div className="flex flex-wrap gap-0.5 mt-0.5">
                  {cell.entries.map((entry) => {
                    const colors = ENTRY_TYPE_COLORS[entry.entryType];
                    return (
                      <span
                        key={entry.id}
                        className={cn('inline-block h-2 w-2 rounded-full', colors.dot)}
                        title={`${entry.name} (${entry.entryType})`}
                      />
                    );
                  })}
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Legend */}
        <div className="flex items-center gap-4 mt-3 pt-3 border-t border-gray-100">
          {ENTRY_TYPES.map((type) => {
            const colors = ENTRY_TYPE_COLORS[type];
            return (
              <div key={type} className="flex items-center gap-1.5">
                <span className={cn('inline-block h-2.5 w-2.5 rounded-full', colors.dot)} />
                <span className="text-[11px] text-gray-500">
                  {type.replace(/_/g, ' ')}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Entries table + add form */}
      <div className="rounded-lg border border-gray-200 bg-white">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
          <h3 className="text-sm font-semibold text-gray-900">
            Holiday Entries ({entriesList.length})
          </h3>
          <Button
            size="sm"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            <Plus className="h-3.5 w-3.5" />
            Add Entry
          </Button>
        </div>

        {/* Add entry form */}
        {showAddForm && (
          <form onSubmit={handleAddEntry} className="px-4 py-3 border-b border-gray-200 bg-gray-50">
            <div className="flex items-end gap-3 flex-wrap">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Date</label>
                <Input
                  type="date"
                  value={newDate}
                  onChange={(e) => setNewDate(e.target.value)}
                  required
                  className="w-40"
                />
              </div>
              <div className="flex-1 min-w-[160px]">
                <label className="block text-xs font-medium text-gray-600 mb-1">Name</label>
                <Input
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="e.g. Christmas Day"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Type</label>
                <select
                  value={newType}
                  onChange={(e) => setNewType(e.target.value as EntryType)}
                  className="rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  {ENTRY_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t.replace(/_/g, ' ')}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex items-center gap-2 pb-1">
                <input
                  type="checkbox"
                  id="entry-recurring"
                  checked={newRecurring}
                  onChange={(e) => setNewRecurring(e.target.checked)}
                  className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                />
                <label htmlFor="entry-recurring" className="text-sm text-gray-700">
                  Recurring
                </label>
              </div>
              <Button type="submit" size="sm" loading={createEntry.isPending}>
                Add
              </Button>
            </div>
          </form>
        )}

        {/* Entries table */}
        {sortedEntries.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-gray-500">
            No holiday entries yet. Add one above.
          </div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Date</th>
                <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Name</th>
                <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Type</th>
                <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Recurring</th>
                <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {sortedEntries.map((entry) => {
                const colors = ENTRY_TYPE_COLORS[entry.entryType];
                return (
                  <tr key={entry.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-900">{formatDate(entry.date)}</td>
                    <td className="px-4 py-3 text-gray-900">{entry.name}</td>
                    <td className="px-4 py-3">
                      <span className={cn(
                        'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
                        colors.bg,
                        colors.text,
                      )}>
                        {entry.entryType.replace(/_/g, ' ')}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {entry.recurring && (
                        <span className="inline-flex items-center gap-1 text-xs text-gray-500">
                          <RefreshCw className="h-3 w-3" />
                          Yearly
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleDeleteEntry(entry.id)}
                        className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-red-600"
                        title="Delete"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
