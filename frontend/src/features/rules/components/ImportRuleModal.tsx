import { useState, useRef } from 'react';

interface ImportRuleModalProps {
  spaceId: string;
  createdBy: string;
  onClose: () => void;
  onSuccess: (result: unknown) => void;
}

export function ImportRuleModal({ spaceId, createdBy, onClose, onSuccess }: ImportRuleModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleImport = async () => {
    if (!file) return;
    setLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('spaceId', spaceId);
      formData.append('createdBy', createdBy);
      const token = localStorage.getItem('ruleforge_token');
      const headers: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};
      const response = await fetch('/v1/rules/import', { method: 'POST', headers, body: formData });
      if (!response.ok) throw new Error('Import failed');
      const result = await response.json();
      onSuccess(result);
      onClose();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Import failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-lg p-6 w-96">
        <h2 className="text-lg font-semibold mb-4">Import Rules</h2>
        <div
          className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center cursor-pointer hover:border-blue-400"
          onClick={() => fileRef.current?.click()}
        >
          {file ? (
            <div className="text-sm text-gray-700">{file.name}</div>
          ) : (
            <div className="text-sm text-gray-500">Click to select a JSON file</div>
          )}
          <input
            ref={fileRef}
            type="file"
            accept=".json"
            className="hidden"
            onChange={e => setFile(e.target.files?.[0] || null)}
          />
        </div>
        {error && <div className="mt-2 text-sm text-red-600">{error}</div>}
        <div className="flex gap-2 mt-4">
          <button onClick={onClose} className="flex-1 px-4 py-2 border rounded-lg text-sm">Cancel</button>
          <button
            onClick={handleImport}
            disabled={!file || loading}
            className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm disabled:opacity-50"
          >
            {loading ? 'Importing...' : 'Import'}
          </button>
        </div>
      </div>
    </div>
  );
}
