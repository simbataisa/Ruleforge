import { MoreVertical, Lock } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { RuleTypeIcon } from './RuleTypeIcon';
import { TagBadge } from './TagBadge';
import { StatusBadge } from './StatusBadge';
import { formatRelativeTime } from '../utils/formatRelativeTime';
import type { Rule } from '@/api/types/rule.types';

interface RuleCardProps {
  rule: Rule;
  selected: boolean;
  onSelect: (e: React.MouseEvent) => void;
  onClick: () => void;
  onMenuOpen: (e: React.MouseEvent) => void;
  onEdit: () => void;
}

export function RuleCard({
  rule,
  selected,
  onSelect,
  onClick,
  onMenuOpen,
  onEdit,
}: RuleCardProps) {
  return (
    <div
      className={cn(
        'group relative flex flex-col rounded-xl border bg-white p-4 transition-all duration-200',
        selected
          ? 'border-indigo-400 bg-indigo-50 shadow-sm'
          : 'border-gray-200 hover:border-gray-300 hover:shadow-md',
      )}
    >
      {/* Checkbox */}
      <div
        className={cn(
          'absolute top-3 left-3 z-10',
          selected ? 'opacity-100' : 'opacity-0 group-hover:opacity-100',
        )}
      >
        <input
          type="checkbox"
          checked={selected}
          onChange={() => {}}
          onClick={(e) => {
            e.stopPropagation();
            onSelect(e);
          }}
          className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
        />
      </div>

      {/* Type badge top-right */}
      <div className="flex justify-end mb-2">
        <RuleTypeIcon type={rule.type} showLabel />
      </div>

      {/* Content - clickable area */}
      <div className="flex-1 cursor-pointer" onClick={onClick}>
        <h3 className="text-base font-semibold text-gray-900 line-clamp-2 mb-1">
          {rule.name}
        </h3>
        {rule.description && (
          <p className="text-[13px] text-gray-500 line-clamp-2 mb-3">
            {rule.description}
          </p>
        )}
      </div>

      {/* Version + Status row */}
      <div className="flex items-center gap-2 mb-2 mt-auto pt-2 border-t border-gray-100">
        <span className="text-[13px] text-gray-600 font-medium">v{rule.latestVersion}</span>
        <StatusBadge status={rule.status} />
        {rule.locked && <Lock className="h-3.5 w-3.5 text-gray-400" />}
      </div>

      {/* Tags */}
      {rule.tags && rule.tags.length > 0 && (
        <div className="flex flex-wrap gap-1 mb-2">
          {rule.tags.slice(0, 3).map((tag) => (
            <TagBadge key={tag} tag={tag} />
          ))}
          {rule.tags.length > 3 && (
            <span className="inline-flex items-center text-[11px] text-gray-400 font-medium">
              +{rule.tags.length - 3}
            </span>
          )}
        </div>
      )}

      {/* Footer */}
      <div className="flex items-center justify-between pt-2 border-t border-gray-100">
        <span className="text-xs text-gray-500" title={new Date(rule.updatedAt).toLocaleString()}>
          Updated {formatRelativeTime(rule.updatedAt)}
        </span>
        <div className="flex items-center gap-1">
          <button
            onClick={(e) => {
              e.stopPropagation();
              onEdit();
            }}
            className="text-[13px] font-medium text-indigo-600 hover:text-indigo-700 px-2 py-0.5 rounded hover:bg-indigo-50 transition-colors"
          >
            Edit
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onMenuOpen(e);
            }}
            className="rounded p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
          >
            <MoreVertical className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
