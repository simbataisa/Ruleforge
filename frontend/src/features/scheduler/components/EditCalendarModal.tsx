import { useState, useEffect } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { useToastStore } from '@/stores/toast.store';
import { useUpdateHolidayCalendar, type HolidayCalendar } from '@/api/holiday-calendars.api';

interface EditCalendarModalProps {
  isOpen: boolean;
  onClose: () => void;
  calendar: HolidayCalendar;
}

const WEEKEND_OPTIONS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export function EditCalendarModal({ isOpen, onClose, calendar }: EditCalendarModalProps) {
  const addToast = useToastStore((s) => s.addToast);
  const updateCalendar = useUpdateHolidayCalendar();

  const [name, setName] = useState(calendar.name);
  const [description, setDescription] = useState(calendar.description ?? '');
  const [market, setMarket] = useState(calendar.market);
  const [region, setRegion] = useState(calendar.region ?? '');
  const [weekendDays, setWeekendDays] = useState<string[]>(calendar.weekendDays ?? ['SATURDAY', 'SUNDAY']);
  const [enabled, setEnabled] = useState(calendar.enabled);

  useEffect(() => {
    setName(calendar.name);
    setDescription(calendar.description ?? '');
    setMarket(calendar.market);
    setRegion(calendar.region ?? '');
    setWeekendDays(calendar.weekendDays ?? ['SATURDAY', 'SUNDAY']);
    setEnabled(calendar.enabled);
  }, [calendar]);

  const toggleWeekendDay = (day: string) => {
    setWeekendDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day],
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !market.trim()) return;

    try {
      await updateCalendar.mutateAsync({
        id: calendar.id,
        data: {
          name: name.trim(),
          description: description.trim() || undefined,
          market: market.trim().toUpperCase(),
          region: region.trim() || undefined,
          weekendDays,
          enabled,
        },
      });
      addToast({ type: 'success', title: 'Holiday calendar updated' });
      onClose();
    } catch {
      addToast({ type: 'error', title: 'Failed to update calendar' });
    }
  };

  return (
    <Modal open={isOpen} onClose={onClose} title="Edit Holiday Calendar">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="edit-cal-name" className="block text-sm font-medium text-gray-700 mb-1">
            Name
          </label>
          <Input
            id="edit-cal-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. US Federal Holidays 2026"
            autoFocus
            required
          />
        </div>

        <div>
          <label htmlFor="edit-cal-desc" className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            id="edit-cal-desc"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description..."
            rows={2}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="edit-cal-market" className="block text-sm font-medium text-gray-700 mb-1">
              Market
            </label>
            <Input
              id="edit-cal-market"
              value={market}
              onChange={(e) => setMarket(e.target.value)}
              placeholder="e.g. US, UK, SG"
              required
            />
          </div>
          <div>
            <label htmlFor="edit-cal-region" className="block text-sm font-medium text-gray-700 mb-1">
              Region
            </label>
            <Input
              id="edit-cal-region"
              value={region}
              onChange={(e) => setRegion(e.target.value)}
              placeholder="e.g. North America"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Weekend Days</label>
          <div className="flex flex-wrap gap-2">
            {WEEKEND_OPTIONS.map((day) => (
              <button
                key={day}
                type="button"
                onClick={() => toggleWeekendDay(day)}
                className={`rounded-full px-3 py-1 text-xs font-medium border transition-colors ${
                  weekendDays.includes(day)
                    ? 'bg-indigo-100 text-indigo-700 border-indigo-300'
                    : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'
                }`}
              >
                {day.slice(0, 3)}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-3">
          <label className="text-sm font-medium text-gray-700">Enabled</label>
          <button
            type="button"
            onClick={() => setEnabled(!enabled)}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
              enabled ? 'bg-indigo-600' : 'bg-gray-200'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                enabled ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
          <span className="text-sm text-gray-500">{enabled ? 'Active' : 'Disabled'}</span>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            loading={updateCalendar.isPending}
            disabled={!name.trim() || !market.trim()}
          >
            Save Changes
          </Button>
        </div>
      </form>
    </Modal>
  );
}
