import { X } from 'lucide-react';

interface FilterChip {
  key: string;
  label: string;
  value: string;
}

interface FilterChipsProps {
  chips: FilterChip[];
  onRemove: (key: string, value: string) => void;
  onClearAll: () => void;
}

export function FilterChips({ chips, onRemove, onClearAll }: FilterChipsProps) {
  if (chips.length === 0) return null;

  return (
    <div className="flex items-center gap-1.5 flex-wrap">
      {chips.map((chip) => (
        <span
          key={`${chip.key}:${chip.value}`}
          className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-1 text-xs font-medium text-indigo-700"
        >
          {chip.label}: {chip.value}
          <button
            onClick={() => onRemove(chip.key, chip.value)}
            className="ml-0.5 rounded p-0.5 text-indigo-400 hover:text-indigo-600 hover:bg-indigo-100 transition-colors"
          >
            <X className="h-3 w-3" />
          </button>
        </span>
      ))}
      <button
        onClick={onClearAll}
        className="text-xs text-indigo-600 hover:underline ml-1"
      >
        Clear all
      </button>
    </div>
  );
}
