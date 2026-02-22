import { useMemo } from 'react';
import { X } from 'lucide-react';
import { Button } from '@/shared/components/Button';

interface VersionInfo {
  version: number;
  definition: Record<string, unknown>;
}

interface VersionDiffModalProps {
  isOpen: boolean;
  onClose: () => void;
  versionA: VersionInfo;
  versionB: VersionInfo;
}

type DiffType = 'added' | 'removed' | 'changed' | 'unchanged';

interface DiffEntry {
  path: string;
  type: DiffType;
  oldValue?: unknown;
  newValue?: unknown;
}

function flattenObject(obj: unknown, prefix = ''): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  if (obj === null || obj === undefined || typeof obj !== 'object') {
    result[prefix || '(root)'] = obj;
    return result;
  }
  if (Array.isArray(obj)) {
    if (obj.length === 0) {
      result[prefix || '(root)'] = '[]';
    }
    obj.forEach((item, index) => {
      const key = prefix ? `${prefix}[${index}]` : `[${index}]`;
      Object.assign(result, flattenObject(item, key));
    });
  } else {
    const entries = Object.entries(obj as Record<string, unknown>);
    if (entries.length === 0) {
      result[prefix || '(root)'] = '{}';
    }
    for (const [key, value] of entries) {
      const path = prefix ? `${prefix}.${key}` : key;
      if (value !== null && typeof value === 'object') {
        Object.assign(result, flattenObject(value, path));
      } else {
        result[path] = value;
      }
    }
  }
  return result;
}

function computeDiff(a: Record<string, unknown>, b: Record<string, unknown>): DiffEntry[] {
  const flatA = flattenObject(a);
  const flatB = flattenObject(b);
  const allKeys = new Set([...Object.keys(flatA), ...Object.keys(flatB)]);
  const entries: DiffEntry[] = [];

  for (const key of Array.from(allKeys).sort()) {
    const inA = key in flatA;
    const inB = key in flatB;
    if (inA && inB) {
      const valA = JSON.stringify(flatA[key]);
      const valB = JSON.stringify(flatB[key]);
      if (valA === valB) {
        entries.push({ path: key, type: 'unchanged', oldValue: flatA[key], newValue: flatB[key] });
      } else {
        entries.push({ path: key, type: 'changed', oldValue: flatA[key], newValue: flatB[key] });
      }
    } else if (inA) {
      entries.push({ path: key, type: 'removed', oldValue: flatA[key] });
    } else {
      entries.push({ path: key, type: 'added', newValue: flatB[key] });
    }
  }
  return entries;
}

function formatValue(value: unknown): string {
  if (value === undefined) return '';
  if (typeof value === 'string') return `"${value}"`;
  return String(value);
}

const diffColors: Record<DiffType, { bg: string; text: string; indicator: string }> = {
  added: { bg: 'bg-green-50', text: 'text-green-800', indicator: '+' },
  removed: { bg: 'bg-red-50', text: 'text-red-800', indicator: '-' },
  changed: { bg: 'bg-yellow-50', text: 'text-yellow-800', indicator: '~' },
  unchanged: { bg: '', text: 'text-gray-600', indicator: ' ' },
};

export function VersionDiffModal({ isOpen, onClose, versionA, versionB }: VersionDiffModalProps) {
  const diff = useMemo(
    () => computeDiff(versionA.definition, versionB.definition),
    [versionA.definition, versionB.definition],
  );

  const stats = useMemo(() => {
    const added = diff.filter((d) => d.type === 'added').length;
    const removed = diff.filter((d) => d.type === 'removed').length;
    const changed = diff.filter((d) => d.type === 'changed').length;
    return { added, removed, changed };
  }, [diff]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div className="relative bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[80vh] flex flex-col mx-4">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">
              Compare Versions
            </h2>
            <p className="text-sm text-gray-500 mt-0.5">
              v{versionA.version} vs v{versionB.version}
            </p>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3 text-xs">
              {stats.added > 0 && (
                <span className="text-green-700 font-medium">+{stats.added} added</span>
              )}
              {stats.removed > 0 && (
                <span className="text-red-700 font-medium">-{stats.removed} removed</span>
              )}
              {stats.changed > 0 && (
                <span className="text-yellow-700 font-medium">~{stats.changed} changed</span>
              )}
            </div>
            <button
              onClick={onClose}
              className="p-1 rounded hover:bg-gray-100 transition-colors"
            >
              <X className="h-5 w-5 text-gray-400" />
            </button>
          </div>
        </div>

        {/* Diff content */}
        <div className="flex-1 overflow-auto">
          {diff.length === 0 ? (
            <div className="flex items-center justify-center h-48 text-gray-500 text-sm">
              No differences found between versions.
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {/* Column headers */}
              <div className="grid grid-cols-[auto_1fr_1fr_1fr] gap-2 px-4 py-2 bg-gray-50 text-xs font-semibold text-gray-500 uppercase tracking-wide sticky top-0">
                <span className="w-6" />
                <span>Path</span>
                <span>v{versionA.version}</span>
                <span>v{versionB.version}</span>
              </div>
              {diff.map((entry) => {
                const colors = diffColors[entry.type];
                return (
                  <div
                    key={entry.path}
                    className={`grid grid-cols-[auto_1fr_1fr_1fr] gap-2 px-4 py-2 text-xs font-mono ${colors.bg}`}
                  >
                    <span className={`w-6 font-bold ${colors.text}`}>
                      {colors.indicator}
                    </span>
                    <span className={`font-medium ${colors.text} truncate`} title={entry.path}>
                      {entry.path}
                    </span>
                    <span className="text-gray-600 truncate" title={formatValue(entry.oldValue)}>
                      {formatValue(entry.oldValue)}
                    </span>
                    <span className="text-gray-600 truncate" title={formatValue(entry.newValue)}>
                      {formatValue(entry.newValue)}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end px-6 py-3 border-t border-gray-200">
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}
