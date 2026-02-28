import { useState, useMemo } from 'react';
import { DiffEditor } from '@monaco-editor/react';
import { X } from 'lucide-react';
import { Button } from '@/shared/components/Button';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  original: string;
  modified: string;
  originalLabel: string;
  modifiedLabel: string;
}

function flattenObject(obj: unknown, prefix = ''): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  if (obj === null || obj === undefined || typeof obj !== 'object') {
    result[prefix || '(root)'] = obj;
    return result;
  }
  if (Array.isArray(obj)) {
    if (obj.length === 0) result[prefix || '(root)'] = '[]';
    obj.forEach((item, index) => {
      const key = prefix ? `${prefix}[${index}]` : `[${index}]`;
      Object.assign(result, flattenObject(item, key));
    });
  } else {
    const entries = Object.entries(obj as Record<string, unknown>);
    if (entries.length === 0) result[prefix || '(root)'] = '{}';
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

function computeStats(original: string, modified: string) {
  try {
    const a = flattenObject(JSON.parse(original));
    const b = flattenObject(JSON.parse(modified));
    const allKeys = new Set([...Object.keys(a), ...Object.keys(b)]);
    let added = 0, removed = 0, changed = 0;
    for (const key of allKeys) {
      const inA = key in a, inB = key in b;
      if (inA && inB) {
        if (JSON.stringify(a[key]) !== JSON.stringify(b[key])) changed++;
      } else if (inA) {
        removed++;
      } else {
        added++;
      }
    }
    return { added, removed, changed };
  } catch {
    return { added: 0, removed: 0, changed: 0 };
  }
}

export function MonacoDiffModal({
  isOpen,
  onClose,
  original,
  modified,
  originalLabel,
  modifiedLabel,
}: Props) {
  const [sideBySide, setSideBySide] = useState(true);
  const stats = useMemo(() => computeStats(original, modified), [original, modified]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div
        className="relative bg-white rounded-lg shadow-xl w-full max-w-7xl mx-4 flex flex-col"
        style={{ height: '90vh' }}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
          <div className="flex items-center gap-4">
            <h2 className="text-lg font-semibold text-gray-900">Definition Diff</h2>
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
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 text-xs">
              <span className="px-2 py-1 bg-red-50 border border-red-200 rounded text-red-700">
                {originalLabel}
              </span>
              <span className="px-2 py-1 bg-green-50 border border-green-200 rounded text-green-700">
                {modifiedLabel}
              </span>
            </div>
            <Button size="sm" variant="outline" onClick={() => setSideBySide(!sideBySide)}>
              {sideBySide ? 'Unified' : 'Side by Side'}
            </Button>
            <button
              onClick={onClose}
              className="p-1 rounded hover:bg-gray-100 transition-colors"
            >
              <X className="h-5 w-5 text-gray-400" />
            </button>
          </div>
        </div>

        {/* Diff editor */}
        <div className="flex-1 overflow-hidden">
          <DiffEditor
            height="100%"
            language="json"
            theme="vs"
            original={original}
            modified={modified}
            options={{
              renderSideBySide: sideBySide,
              readOnly: true,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              automaticLayout: true,
              fontSize: 13,
              fontFamily:
                "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace",
              lineHeight: 1.6,
              wordWrap: 'on',
            }}
          />
        </div>
      </div>
    </div>
  );
}
