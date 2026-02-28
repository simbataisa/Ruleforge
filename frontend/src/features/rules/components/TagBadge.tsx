import { cn } from '@/shared/utils/cn';

interface TagBadgeProps {
  tag: string;
  selected?: boolean;
  removable?: boolean;
  count?: number;
  onClick?: () => void;
  onRemove?: () => void;
  size?: 'sm' | 'md';
}

export function TagBadge({
  tag,
  selected = false,
  removable = false,
  count,
  onClick,
  onRemove,
  size = 'sm',
}: TagBadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full border font-medium transition-colors',
        size === 'sm' && 'h-5 px-2 text-[11px]',
        size === 'md' && 'h-6 px-2.5 text-xs',
        selected
          ? 'border-indigo-300 bg-indigo-50 text-indigo-700'
          : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300',
        onClick && 'cursor-pointer',
      )}
      onClick={onClick}
    >
      <span className="truncate max-w-[120px]">{tag}</span>
      {count != null && (
        <span className="text-gray-400">({count})</span>
      )}
      {removable && onRemove && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          className="ml-0.5 rounded-full p-0.5 hover:bg-gray-200 text-gray-400 hover:text-gray-600"
        >
          <svg className="h-3 w-3" viewBox="0 0 12 12" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M3 3l6 6M9 3l-6 6" />
          </svg>
        </button>
      )}
    </span>
  );
}
