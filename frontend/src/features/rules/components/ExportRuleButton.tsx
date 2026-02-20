import { useState } from 'react';

interface ExportRuleButtonProps {
  ruleId: string;
  spaceId: string;
  ruleName: string;
}

export function ExportRuleButton({ ruleId, spaceId, ruleName }: ExportRuleButtonProps) {
  const [loading, setLoading] = useState(false);

  const handleExport = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('ruleforge_token');
      const headers: Record<string, string> = token ? { Authorization: `Bearer ${token}` } : {};
      const response = await fetch(
        `/v1/rules/${ruleId}/export?spaceId=${encodeURIComponent(spaceId)}&format=JSON`,
        { headers },
      );
      if (!response.ok) throw new Error('Export failed');
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `rule-${ruleName}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Export failed:', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleExport}
      disabled={loading}
      className="text-xs px-2 py-1 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded"
      title="Export rule as JSON"
    >
      {loading ? '...' : 'Export'}
    </button>
  );
}
