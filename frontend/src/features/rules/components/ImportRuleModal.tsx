import { useState, useRef } from 'react';
import { Upload, FileJson, AlertCircle } from 'lucide-react';

interface ImportRuleModalProps {
  spaceId?: string;
  createdBy?: string;
  onClose: () => void;
  onSuccess: (result: unknown) => void;
}

interface ImportResultData {
  totalRules?: number;
  importedCount?: number;
  skippedCount?: number;
  failedCount?: number;
  errors?: string[];
}

export function ImportRuleModal({ spaceId, createdBy, onClose, onSuccess }: ImportRuleModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ImportResultData | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleImport = async () => {
    if (!file) return;
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const formData = new FormData();
      formData.append('file', file);
      if (spaceId) formData.append('spaceId', spaceId);
      if (createdBy) formData.append('createdBy', createdBy);
      const token = localStorage.getItem('ruleforge_token');
      const headers: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};
      const response = await fetch('/v1/rules/import', { method: 'POST', headers, body: formData });
      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || `Import failed (HTTP ${response.status})`);
      }
      const data = await response.json();
      setResult(data);
      onSuccess(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Import failed');
    } finally {
      setLoading(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile && droppedFile.name.endsWith('.json')) {
      setFile(droppedFile);
      setError(null);
    } else {
      setError('Please drop a .json file');
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-lg p-6 w-[28rem]" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center gap-2 mb-1">
          <Upload className="h-5 w-5 text-indigo-600" />
          <h2 className="text-lg font-semibold text-gray-900">Import Rules</h2>
        </div>
        <p className="text-sm text-gray-500 mb-4">
          Import rules from a JSON file previously exported from RuleForge. The file can contain a single rule or an array of rules.
        </p>

        <div
          className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-colors ${
            file ? 'border-indigo-400 bg-indigo-50' : 'border-gray-300 hover:border-indigo-400 hover:bg-gray-50'
          }`}
          onClick={() => fileRef.current?.click()}
          onDragOver={(e) => e.preventDefault()}
          onDrop={handleDrop}
        >
          {file ? (
            <div className="flex items-center justify-center gap-2">
              <FileJson className="h-5 w-5 text-indigo-600" />
              <span className="text-sm font-medium text-gray-700">{file.name}</span>
              <span className="text-xs text-gray-400">({(file.size / 1024).toFixed(1)} KB)</span>
            </div>
          ) : (
            <div>
              <FileJson className="h-8 w-8 text-gray-400 mx-auto mb-2" />
              <p className="text-sm text-gray-600 font-medium">
                Click to select or drag &amp; drop a JSON file
              </p>
              <p className="text-xs text-gray-400 mt-1">
                Accepts .json files exported from RuleForge
              </p>
            </div>
          )}
          <input
            ref={fileRef}
            type="file"
            accept=".json"
            className="hidden"
            onChange={e => { setFile(e.target.files?.[0] || null); setError(null); }}
          />
        </div>

        {error && (
          <div className="mt-3 flex items-start gap-2 text-sm text-red-600 bg-red-50 rounded-lg p-3">
            <AlertCircle className="h-4 w-4 mt-0.5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {result && (
          <div className="mt-3 text-sm bg-green-50 rounded-lg p-3 text-green-800">
            <p className="font-medium">Import complete</p>
            <p className="mt-1">
              {result.importedCount ?? 0} imported, {result.skippedCount ?? 0} skipped, {result.failedCount ?? 0} failed
            </p>
            {result.errors && result.errors.length > 0 && (
              <ul className="mt-1 text-xs text-green-700 list-disc list-inside">
                {result.errors.map((err, i) => <li key={i}>{err}</li>)}
              </ul>
            )}
          </div>
        )}

        <div className="flex items-center justify-between mt-4 pt-3 border-t border-gray-100">
          <p className="text-xs text-gray-400">
            Imported rules are created as drafts
          </p>
          <div className="flex gap-2">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 transition-colors"
            >
              {result ? 'Close' : 'Cancel'}
            </button>
            {!result && (
              <button
                onClick={handleImport}
                disabled={!file || loading}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm disabled:opacity-50 hover:bg-indigo-700 transition-colors"
              >
                {loading ? 'Importing...' : 'Import'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
