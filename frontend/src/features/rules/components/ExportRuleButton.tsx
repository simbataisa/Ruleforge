import { useState } from 'react';
import { Download } from 'lucide-react';
import { useToastStore } from '@/stores/toast.store';

interface ExportRuleButtonProps {
  ruleId: string;
  ruleName: string;
  variant?: 'icon' | 'button' | 'menu';
}

export function ExportRuleButton({ ruleId, ruleName, variant = 'button' }: ExportRuleButtonProps) {
  const [loading, setLoading] = useState(false);
  const addToast = useToastStore((s) => s.addToast);

  const handleExport = async (e: React.MouseEvent) => {
    e.stopPropagation();
    setLoading(true);
    try {
      const token = localStorage.getItem('ruleforge_token');
      const headers: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};
      const response = await fetch(
        `/v1/rules/${ruleId}/export?format=JSON`,
        { headers },
      );
      if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(errorBody || `Export failed (HTTP ${response.status})`);
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const safeName = ruleName.replace(/[^a-zA-Z0-9_-]/g, '_');
      a.download = `${safeName}.json`;
      a.click();
      URL.revokeObjectURL(url);
      addToast({ type: 'success', title: `Exported "${ruleName}" as JSON` });
    } catch (e) {
      console.error('Export failed:', e);
      addToast({ type: 'error', title: 'Export failed. Please try again.' });
    } finally {
      setLoading(false);
    }
  };

  if (variant === 'icon') {
    return (
      <button
        onClick={handleExport}
        disabled={loading}
        className="p-1.5 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-600 disabled:opacity-50"
        title="Export rule as JSON"
      >
        <Download className={`h-4 w-4 ${loading ? 'animate-pulse' : ''}`} />
      </button>
    );
  }

  if (variant === 'menu') {
    return (
      <button
        onClick={handleExport}
        disabled={loading}
        className="flex w-full items-center gap-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50"
      >
        <Download className={`h-3.5 w-3.5 ${loading ? 'animate-pulse' : ''}`} />
        {loading ? 'Exporting...' : 'Export'}
      </button>
    );
  }

  return (
    <button
      onClick={handleExport}
      disabled={loading}
      className="inline-flex items-center gap-1.5 text-sm px-3 py-1.5 text-gray-700 hover:text-gray-900 hover:bg-gray-100 border border-gray-300 rounded-lg transition-colors disabled:opacity-50"
      title="Export rule as JSON"
    >
      <Download className={`h-4 w-4 ${loading ? 'animate-pulse' : ''}`} />
      {loading ? 'Exporting...' : 'Export'}
    </button>
  );
}
