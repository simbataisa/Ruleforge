function getAuthHeaders(): Record<string, string> {
  const token = localStorage.getItem('ruleforge_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export const importExportApi = {
  exportRule: async (ruleId: string): Promise<Blob> => {
    const response = await fetch(
      `/v1/rules/${ruleId}/export?format=JSON`,
      { headers: getAuthHeaders() },
    );
    if (!response.ok) throw new Error('Export failed');
    return response.blob();
  },

  exportBulk: async (ruleIds: string[]): Promise<Blob> => {
    const response = await fetch(
      `/v1/rules/export-bulk`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(ruleIds),
      },
    );
    if (!response.ok) throw new Error('Bulk export failed');
    return response.blob();
  },

  importRules: async (file: File, spaceId: string, createdBy: string): Promise<unknown> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('spaceId', spaceId);
    formData.append('createdBy', createdBy);
    const response = await fetch('/v1/rules/import', {
      method: 'POST',
      headers: getAuthHeaders(),
      body: formData,
    });
    if (!response.ok) throw new Error('Import failed');
    return response.json();
  },
};
