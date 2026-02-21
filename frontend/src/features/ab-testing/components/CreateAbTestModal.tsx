import { useState } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import type { RuleVersion } from '@/api/types/rule.types';

interface CreateAbTestModalProps {
  ruleId: string;
  versions: RuleVersion[];
  isOpen: boolean;
  onClose: () => void;
  onCreated?: () => void;
}

interface AbTestFormData {
  name: string;
  versionA: string;
  versionB: string;
  trafficSplit: number;
  startDate: string;
  endDate: string;
}

async function createAbTest(
  ruleId: string,
  data: AbTestFormData,
): Promise<void> {
  const token = localStorage.getItem('ruleforge_token');
  const response = await fetch(`/v1/rules/${ruleId}/ab-tests`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({
      name: data.name,
      versionA: Number(data.versionA),
      versionB: Number(data.versionB),
      trafficSplit: data.trafficSplit,
      startDate: data.startDate || undefined,
      endDate: data.endDate || undefined,
    }),
  });

  if (!response.ok) {
    throw new Error(`Failed to create A/B test: ${response.statusText}`);
  }
}

export function CreateAbTestModal({
  ruleId,
  versions,
  isOpen,
  onClose,
  onCreated,
}: CreateAbTestModalProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState<AbTestFormData>({
    name: '',
    versionA: versions[0]?.version.toString() ?? '',
    versionB: versions[1]?.version.toString() ?? versions[0]?.version.toString() ?? '',
    trafficSplit: 50,
    startDate: '',
    endDate: '',
  });

  const handleClose = () => {
    setForm({
      name: '',
      versionA: versions[0]?.version.toString() ?? '',
      versionB: versions[1]?.version.toString() ?? versions[0]?.version.toString() ?? '',
      trafficSplit: 50,
      startDate: '',
      endDate: '',
    });
    setError(null);
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim() || !form.versionA || !form.versionB) return;

    setLoading(true);
    setError(null);

    try {
      await createAbTest(ruleId, form);
      handleClose();
      onCreated?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create A/B test');
    } finally {
      setLoading(false);
    }
  };

  const splitA = form.trafficSplit;
  const splitB = 100 - form.trafficSplit;

  return (
    <Modal open={isOpen} onClose={handleClose} title="Create A/B Test">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <div>
          <label
            htmlFor="ab-name"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Test Name
          </label>
          <Input
            id="ab-name"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            placeholder="e.g. Pricing rule v2 vs v3"
            autoFocus
            required
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label
              htmlFor="ab-version-a"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Version A (Control)
            </label>
            <Select
              id="ab-version-a"
              value={form.versionA}
              onChange={(e) => setForm((f) => ({ ...f, versionA: e.target.value }))}
              required
            >
              {versions.map((v) => (
                <option key={v.id} value={v.version.toString()}>
                  v{v.version} ({v.status})
                </option>
              ))}
            </Select>
          </div>

          <div>
            <label
              htmlFor="ab-version-b"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Version B (Challenger)
            </label>
            <Select
              id="ab-version-b"
              value={form.versionB}
              onChange={(e) => setForm((f) => ({ ...f, versionB: e.target.value }))}
              required
            >
              {versions.map((v) => (
                <option key={v.id} value={v.version.toString()}>
                  v{v.version} ({v.status})
                </option>
              ))}
            </Select>
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-1">
            <label
              htmlFor="ab-split"
              className="block text-sm font-medium text-gray-700"
            >
              Traffic Split
            </label>
            <span className="text-sm text-gray-500">
              A: {splitA}% / B: {splitB}%
            </span>
          </div>
          <input
            id="ab-split"
            type="range"
            min={0}
            max={100}
            step={5}
            value={form.trafficSplit}
            onChange={(e) =>
              setForm((f) => ({
                ...f,
                trafficSplit: Number(e.target.value),
              }))
            }
            className="w-full accent-indigo-600"
          />
          <div className="mt-2 flex h-3 overflow-hidden rounded-full">
            <div
              className="bg-indigo-500 transition-all"
              style={{ width: `${splitA}%` }}
            />
            <div
              className="bg-indigo-200 transition-all"
              style={{ width: `${splitB}%` }}
            />
          </div>
          <div className="mt-1 flex justify-between text-xs text-gray-500">
            <span>Version A</span>
            <span>Version B</span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label
              htmlFor="ab-start"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Start Date (optional)
            </label>
            <Input
              id="ab-start"
              type="date"
              value={form.startDate}
              onChange={(e) => setForm((f) => ({ ...f, startDate: e.target.value }))}
            />
          </div>

          <div>
            <label
              htmlFor="ab-end"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              End Date (optional)
            </label>
            <Input
              id="ab-end"
              type="date"
              value={form.endDate}
              onChange={(e) => setForm((f) => ({ ...f, endDate: e.target.value }))}
              min={form.startDate || undefined}
            />
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            loading={loading}
            disabled={!form.name.trim() || !form.versionA || !form.versionB}
          >
            Create A/B Test
          </Button>
        </div>
      </form>
    </Modal>
  );
}
