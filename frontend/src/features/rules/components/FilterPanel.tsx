import { useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { TagBadge } from './TagBadge';
import { RuleTypeIcon, ruleTypeConfig } from './RuleTypeIcon';
import type { RuleType, RuleStatus, TagCount } from '@/api/types/rule.types';

const STATUS_OPTIONS: { value: RuleStatus | 'PENDING_APPROVAL'; label: string; dotColor: string }[] = [
  { value: 'DRAFT', label: 'Draft', dotColor: 'bg-gray-400' },
  { value: 'PUBLISHED', label: 'Published', dotColor: 'bg-green-500' },
  { value: 'ARCHIVED', label: 'Archived', dotColor: 'bg-gray-300' },
  { value: 'PENDING_APPROVAL', label: 'Pending Approval', dotColor: 'bg-amber-500' },
];

const TYPE_OPTIONS: RuleType[] = ['TABLE', 'TREE', 'SCRIPT', 'FLOW', 'SCORECARD', 'DMN', 'CEP', 'ENSEMBLE'];

const DATE_PRESETS = [
  { label: 'Today', days: 0 },
  { label: 'Last 7 days', days: 7 },
  { label: 'Last 30 days', days: 30 },
] as const;

interface FilterPanelProps {
  selectedStatuses: string[];
  onStatusChange: (statuses: string[]) => void;
  selectedTypes: string[];
  onTypeChange: (types: string[]) => void;
  selectedTags: string[];
  onTagChange: (tags: string[]) => void;
  tags: TagCount[];
  datePreset: number | null;
  onDatePresetChange: (days: number | null) => void;
}

function CollapsibleSection({
  title,
  defaultOpen = true,
  children,
}: {
  title: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
}) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div className="border-b border-gray-100 last:border-b-0">
      <button
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between px-4 py-2.5 text-xs font-semibold uppercase tracking-wider text-gray-500 hover:bg-gray-50"
      >
        {title}
        {open ? (
          <ChevronDown className="h-3.5 w-3.5" />
        ) : (
          <ChevronRight className="h-3.5 w-3.5" />
        )}
      </button>
      {open && <div className="px-4 pb-3">{children}</div>}
    </div>
  );
}

export function FilterPanel({
  selectedStatuses,
  onStatusChange,
  selectedTypes,
  onTypeChange,
  selectedTags,
  onTagChange,
  tags,
  datePreset,
  onDatePresetChange,
}: FilterPanelProps) {
  const toggleStatus = (value: string) => {
    if (selectedStatuses.includes(value)) {
      onStatusChange(selectedStatuses.filter((s) => s !== value));
    } else {
      onStatusChange([...selectedStatuses, value]);
    }
  };

  const toggleType = (value: string) => {
    if (selectedTypes.includes(value)) {
      onTypeChange(selectedTypes.filter((t) => t !== value));
    } else {
      onTypeChange([...selectedTypes, value]);
    }
  };

  const toggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      onTagChange(selectedTags.filter((t) => t !== tag));
    } else {
      onTagChange([...selectedTags, tag]);
    }
  };

  return (
    <div className="border-t border-gray-200 mt-2">
      <div className="px-4 py-2.5">
        <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">
          Filters
        </span>
      </div>

      <CollapsibleSection title="Status">
        <div className="space-y-1">
          {STATUS_OPTIONS.map((opt) => (
            <label
              key={opt.value}
              className="flex items-center gap-2 rounded px-1 py-1 text-sm text-gray-700 hover:bg-gray-50 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={selectedStatuses.includes(opt.value)}
                onChange={() => toggleStatus(opt.value)}
                className="h-3.5 w-3.5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
              />
              <span className={cn('h-2 w-2 rounded-full', opt.dotColor)} />
              <span className="flex-1 text-[13px]">{opt.label}</span>
            </label>
          ))}
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Type">
        <div className="space-y-1">
          {TYPE_OPTIONS.map((type) => (
            <label
              key={type}
              className="flex items-center gap-2 rounded px-1 py-1 text-sm text-gray-700 hover:bg-gray-50 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={selectedTypes.includes(type)}
                onChange={() => toggleType(type)}
                className="h-3.5 w-3.5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
              />
              <RuleTypeIcon type={type} size={14} />
              <span className="flex-1 text-[13px]">{ruleTypeConfig[type]?.label ?? type}</span>
            </label>
          ))}
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Tags" defaultOpen={tags.length > 0}>
        {tags.length === 0 ? (
          <p className="text-xs text-gray-400">No tags available</p>
        ) : (
          <div className="flex flex-wrap gap-1.5">
            {tags.slice(0, 10).map((t) => (
              <TagBadge
                key={t.tag}
                tag={t.tag}
                count={t.count}
                selected={selectedTags.includes(t.tag)}
                onClick={() => toggleTag(t.tag)}
                size="md"
              />
            ))}
          </div>
        )}
      </CollapsibleSection>

      <CollapsibleSection title="Last Updated" defaultOpen={false}>
        <div className="flex flex-wrap gap-1.5">
          {DATE_PRESETS.map((preset) => (
            <button
              key={preset.days}
              onClick={() =>
                onDatePresetChange(datePreset === preset.days ? null : preset.days)
              }
              className={cn(
                'rounded-full px-2.5 py-1 text-xs font-medium transition-colors',
                datePreset === preset.days
                  ? 'bg-indigo-50 text-indigo-700'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200',
              )}
            >
              {preset.label}
            </button>
          ))}
        </div>
      </CollapsibleSection>
    </div>
  );
}
