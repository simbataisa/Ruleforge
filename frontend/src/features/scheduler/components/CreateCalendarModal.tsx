import { useState } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { useToastStore } from '@/stores/toast.store';
import { useCreateHolidayCalendar } from '@/api/holiday-calendars.api';

interface CreateCalendarModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const WEEKEND_OPTIONS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export function CreateCalendarModal({ isOpen, onClose }: CreateCalendarModalProps) {
  const addToast = useToastStore((s) => s.addToast);
  const createCalendar = useCreateHolidayCalendar();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [market, setMarket] = useState('');
  const [region, setRegion] = useState('');
  const [weekendDays, setWeekendDays] = useState<string[]>(['SATURDAY', 'SUNDAY']);

  const handleClose = () => {
    setName('');
    setDescription('');
    setMarket('');
    setRegion('');
    setWeekendDays(['SATURDAY', 'SUNDAY']);
    onClose();
  };

  const toggleWeekendDay = (day: string) => {
    setWeekendDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day],
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !market.trim()) return;

    try {
      await createCalendar.mutateAsync({
        name: name.trim(),
        description: description.trim() || undefined,
        market: market.trim().toUpperCase(),
        region: region.trim() || undefined,
        weekendDays,
        enabled: true,
      });
      addToast({ type: 'success', title: 'Holiday calendar created' });
      handleClose();
    } catch {
      addToast({ type: 'error', title: 'Failed to create calendar' });
    }
  };

  return (
    <Modal open={isOpen} onClose={handleClose} title="Create Holiday Calendar">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="cal-name" className="block text-sm font-medium text-gray-700 mb-1">
            Name
          </label>
          <Input
            id="cal-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. US Federal Holidays 2026"
            autoFocus
            required
          />
        </div>

        <div>
          <label htmlFor="cal-desc" className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            id="cal-desc"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description..."
            rows={2}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="cal-market" className="block text-sm font-medium text-gray-700 mb-1">
              Market
            </label>
            <Input
              id="cal-market"
              value={market}
              onChange={(e) => setMarket(e.target.value)}
              placeholder="e.g. US, UK, SG"
              required
            />
          </div>
          <div>
            <label htmlFor="cal-region" className="block text-sm font-medium text-gray-700 mb-1">
              Region
            </label>
            <Input
              id="cal-region"
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

        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            loading={createCalendar.isPending}
            disabled={!name.trim() || !market.trim()}
          >
            Create Calendar
          </Button>
        </div>
      </form>
    </Modal>
  );
}
