import { useState, useRef, useCallback } from 'react';
import { Upload, Download, X, FileText, CheckCircle, XCircle, AlertTriangle } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { useToastStore } from '@/stores/toast.store';
import { cn } from '@/shared/utils/cn';
import {
  useBulkImportEntries,
  type CreateHolidayEntryRequest,
  type EntryType,
  type BulkImportResult,
} from '@/api/holiday-calendars.api';

interface BulkImportPanelProps {
  calendarId: string;
  onClose: () => void;
}

interface ParsedRow {
  date: string;
  name: string;
  entryType: EntryType;
  recurring: boolean;
  valid: boolean;
  error?: string;
}

const VALID_ENTRY_TYPES = new Set(['PUBLIC_HOLIDAY', 'AD_HOC', 'DAY_IN_LIEU', 'COMPANY_HOLIDAY']);

const CSV_TEMPLATE = `date,name,entryType,recurring
2026-01-01,New Year's Day,PUBLIC_HOLIDAY,true
2026-03-17,St Patrick's Day,PUBLIC_HOLIDAY,true
2026-07-04,Independence Day,PUBLIC_HOLIDAY,true
2026-12-25,Christmas Day,PUBLIC_HOLIDAY,true`;

const JSON_TEMPLATE = JSON.stringify([
  { date: '2026-01-01', name: "New Year's Day", entryType: 'PUBLIC_HOLIDAY', recurring: true },
  { date: '2026-03-17', name: "St Patrick's Day", entryType: 'PUBLIC_HOLIDAY', recurring: true },
  { date: '2026-07-04', name: 'Independence Day', entryType: 'PUBLIC_HOLIDAY', recurring: true },
  { date: '2026-12-25', name: 'Christmas Day', entryType: 'PUBLIC_HOLIDAY', recurring: true },
], null, 2);

function isValidDate(str: string): boolean {
  const match = /^\d{4}-\d{2}-\d{2}$/.test(str);
  if (!match) return false;
  const d = new Date(str + 'T00:00:00');
  return !isNaN(d.getTime());
}

function validateRow(row: { date?: string; name?: string; entryType?: string; recurring?: unknown }): ParsedRow {
  const date = (row.date ?? '').trim();
  const name = (row.name ?? '').trim();
  const entryType = ((row.entryType ?? 'PUBLIC_HOLIDAY') as string).trim().toUpperCase() as EntryType;
  const recurring = row.recurring === true || row.recurring === 'true';

  const errors: string[] = [];
  if (!isValidDate(date)) errors.push('Invalid date');
  if (!name) errors.push('Name is empty');
  if (!VALID_ENTRY_TYPES.has(entryType)) errors.push(`Invalid type: ${entryType}`);

  return {
    date,
    name,
    entryType: VALID_ENTRY_TYPES.has(entryType) ? entryType : 'PUBLIC_HOLIDAY',
    recurring,
    valid: errors.length === 0,
    error: errors.length > 0 ? errors.join(', ') : undefined,
  };
}

function parseCsv(text: string): ParsedRow[] {
  const lines = text.trim().split('\n').filter((l) => l.trim());
  if (lines.length === 0) return [];

  // Skip header if it looks like one
  const firstLine = lines[0].toLowerCase();
  const startIdx = firstLine.includes('date') && firstLine.includes('name') ? 1 : 0;

  return lines.slice(startIdx).map((line) => {
    const parts = line.split(',').map((p) => p.trim());
    return validateRow({
      date: parts[0],
      name: parts[1],
      entryType: parts[2],
      recurring: parts[3],
    });
  });
}

function parseJson(text: string): ParsedRow[] {
  const arr = JSON.parse(text);
  if (!Array.isArray(arr)) throw new Error('Expected a JSON array');
  return arr.map((item) => validateRow(item));
}

function parseContent(text: string): ParsedRow[] {
  const trimmed = text.trim();
  if (trimmed.startsWith('[')) {
    return parseJson(trimmed);
  }
  return parseCsv(trimmed);
}

function downloadFile(content: string, filename: string, type: string) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export function BulkImportPanel({ calendarId, onClose }: BulkImportPanelProps) {
  const addToast = useToastStore((s) => s.addToast);
  const bulkImport = useBulkImportEntries();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [mode, setMode] = useState<'upload' | 'paste'>('upload');
  const [pasteText, setPasteText] = useState('');
  const [parsedRows, setParsedRows] = useState<ParsedRow[]>([]);
  const [importResult, setImportResult] = useState<BulkImportResult | null>(null);
  const [dragOver, setDragOver] = useState(false);

  const handleParse = useCallback((text: string) => {
    try {
      const rows = parseContent(text);
      setParsedRows(rows);
      setImportResult(null);
    } catch {
      addToast({ type: 'error', title: 'Failed to parse data. Check format.' });
      setParsedRows([]);
    }
  }, [addToast]);

  const handleFileRead = useCallback((file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target?.result as string;
      handleParse(text);
    };
    reader.readAsText(file);
  }, [handleParse]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFileRead(file);
  }, [handleFileRead]);

  const handleImport = async () => {
    const validEntries: CreateHolidayEntryRequest[] = parsedRows
      .filter((r) => r.valid)
      .map((r) => ({
        date: r.date,
        name: r.name,
        entryType: r.entryType,
        recurring: r.recurring,
      }));

    if (validEntries.length === 0) {
      addToast({ type: 'error', title: 'No valid entries to import' });
      return;
    }

    try {
      const result = await bulkImport.mutateAsync({ calendarId, entries: validEntries });
      setImportResult(result);
      addToast({ type: 'success', title: `Imported ${result.imported} entries` });
    } catch {
      addToast({ type: 'error', title: 'Bulk import failed' });
    }
  };

  const validCount = parsedRows.filter((r) => r.valid).length;
  const invalidCount = parsedRows.filter((r) => !r.valid).length;

  return (
    <div className="rounded-lg border border-gray-200 bg-white mb-6">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
        <h3 className="text-sm font-semibold text-gray-900">Bulk Import Entries</h3>
        <button onClick={onClose} className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600">
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="p-4 space-y-4">
        {/* Mode toggle */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setMode('upload')}
            className={cn(
              'rounded-lg px-3 py-1.5 text-sm font-medium transition-colors',
              mode === 'upload' ? 'bg-indigo-100 text-indigo-700' : 'text-gray-600 hover:bg-gray-100',
            )}
          >
            Upload File
          </button>
          <button
            onClick={() => setMode('paste')}
            className={cn(
              'rounded-lg px-3 py-1.5 text-sm font-medium transition-colors',
              mode === 'paste' ? 'bg-indigo-100 text-indigo-700' : 'text-gray-600 hover:bg-gray-100',
            )}
          >
            Paste Data
          </button>
          <div className="flex-1" />
          <button
            onClick={() => downloadFile(CSV_TEMPLATE, 'holiday-import-template.csv', 'text/csv')}
            className="flex items-center gap-1 text-xs text-indigo-600 hover:text-indigo-800"
          >
            <Download className="h-3.5 w-3.5" />
            CSV Template
          </button>
          <button
            onClick={() => downloadFile(JSON_TEMPLATE, 'holiday-import-template.json', 'application/json')}
            className="flex items-center gap-1 text-xs text-indigo-600 hover:text-indigo-800"
          >
            <Download className="h-3.5 w-3.5" />
            JSON Template
          </button>
        </div>

        {/* Upload mode */}
        {mode === 'upload' && (
          <div
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={cn(
              'flex flex-col items-center justify-center rounded-lg border-2 border-dashed p-8 cursor-pointer transition-colors',
              dragOver
                ? 'border-indigo-400 bg-indigo-50'
                : 'border-gray-300 hover:border-gray-400 hover:bg-gray-50',
            )}
          >
            <Upload className="h-8 w-8 text-gray-400 mb-2" />
            <p className="text-sm text-gray-600">
              Drop a <span className="font-medium">.csv</span> or <span className="font-medium">.json</span> file here, or click to browse
            </p>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,.json"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) handleFileRead(file);
                e.target.value = '';
              }}
            />
          </div>
        )}

        {/* Paste mode */}
        {mode === 'paste' && (
          <div className="space-y-2">
            <textarea
              value={pasteText}
              onChange={(e) => setPasteText(e.target.value)}
              placeholder="Paste CSV or JSON data here..."
              rows={6}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            <Button size="sm" variant="outline" onClick={() => handleParse(pasteText)} disabled={!pasteText.trim()}>
              <FileText className="h-3.5 w-3.5" />
              Parse
            </Button>
          </div>
        )}

        {/* Preview table */}
        {parsedRows.length > 0 && !importResult && (
          <div>
            <div className="flex items-center gap-3 mb-2">
              <span className="text-sm font-medium text-gray-700">
                Preview ({parsedRows.length} rows)
              </span>
              {validCount > 0 && (
                <span className="flex items-center gap-1 text-xs text-green-600">
                  <CheckCircle className="h-3.5 w-3.5" />
                  {validCount} valid
                </span>
              )}
              {invalidCount > 0 && (
                <span className="flex items-center gap-1 text-xs text-red-600">
                  <XCircle className="h-3.5 w-3.5" />
                  {invalidCount} invalid
                </span>
              )}
            </div>
            <div className="max-h-64 overflow-auto rounded-lg border border-gray-200">
              <table className="w-full text-left text-sm">
                <thead className="sticky top-0 bg-gray-50">
                  <tr className="border-b border-gray-200">
                    <th className="px-3 py-2 text-xs font-medium text-gray-500">Status</th>
                    <th className="px-3 py-2 text-xs font-medium text-gray-500">Date</th>
                    <th className="px-3 py-2 text-xs font-medium text-gray-500">Name</th>
                    <th className="px-3 py-2 text-xs font-medium text-gray-500">Type</th>
                    <th className="px-3 py-2 text-xs font-medium text-gray-500">Recurring</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {parsedRows.map((row, i) => (
                    <tr key={i} className={row.valid ? '' : 'bg-red-50'}>
                      <td className="px-3 py-2">
                        {row.valid ? (
                          <CheckCircle className="h-4 w-4 text-green-500" />
                        ) : (
                          <span title={row.error}>
                            <XCircle className="h-4 w-4 text-red-500" />
                          </span>
                        )}
                      </td>
                      <td className="px-3 py-2 text-gray-900">{row.date}</td>
                      <td className="px-3 py-2 text-gray-900">{row.name}</td>
                      <td className="px-3 py-2 text-gray-600">{row.entryType}</td>
                      <td className="px-3 py-2 text-gray-600">{row.recurring ? 'Yes' : 'No'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mt-3">
              <Button onClick={handleImport} loading={bulkImport.isPending} disabled={validCount === 0}>
                Import {validCount} Entries
              </Button>
            </div>
          </div>
        )}

        {/* Import result */}
        {importResult && (
          <div className="rounded-lg border border-gray-200 p-4 space-y-2">
            <h4 className="text-sm font-semibold text-gray-900">Import Complete</h4>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-1.5">
                <CheckCircle className="h-4 w-4 text-green-500" />
                <span className="text-sm font-medium text-green-700">{importResult.imported} imported</span>
              </div>
              {importResult.skipped > 0 && (
                <div className="flex items-center gap-1.5">
                  <AlertTriangle className="h-4 w-4 text-yellow-500" />
                  <span className="text-sm font-medium text-yellow-700">{importResult.skipped} skipped</span>
                </div>
              )}
              {importResult.errors.length > 0 && (
                <div className="flex items-center gap-1.5">
                  <XCircle className="h-4 w-4 text-red-500" />
                  <span className="text-sm font-medium text-red-700">{importResult.errors.length} errors</span>
                </div>
              )}
            </div>
            {importResult.errors.length > 0 && (
              <ul className="mt-2 space-y-1">
                {importResult.errors.map((err, i) => (
                  <li key={i} className="text-xs text-red-600">{err}</li>
                ))}
              </ul>
            )}
            <div className="pt-2">
              <Button size="sm" variant="outline" onClick={() => { setParsedRows([]); setImportResult(null); setPasteText(''); }}>
                Import More
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
