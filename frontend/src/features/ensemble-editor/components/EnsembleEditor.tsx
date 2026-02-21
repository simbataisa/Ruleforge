import { useCallback } from 'react';
import { Plus, Trash2, GripVertical, ToggleLeft, ToggleRight } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { useEnsembleEditorStore } from '@/stores/ensemble-editor.store';
import { useEditorStore } from '@/stores/editor.store';
import type { EnsembleStrategy } from '@/stores/ensemble-editor.store';

const STRATEGIES: { value: EnsembleStrategy; label: string; description: string }[] = [
  { value: 'VOTING', label: 'Voting', description: 'Majority vote across members' },
  { value: 'WEIGHTED_AVERAGE', label: 'Weighted Average', description: 'Weighted average of member outputs' },
  { value: 'FIRST_WINS', label: 'First Wins', description: 'First member to produce a result wins' },
  { value: 'BEST_SCORE', label: 'Best Score', description: 'Member with the highest confidence score wins' },
  { value: 'UNANIMOUS', label: 'Unanimous', description: 'All members must agree' },
];

export function EnsembleEditor() {
  const {
    strategy,
    outputField,
    threshold,
    members,
    setStrategy,
    setOutputField,
    setThreshold,
    addMember,
    removeMember,
    updateMember,
  } = useEnsembleEditorStore();

  const setDirty = useEditorStore((s) => s.setDirty);

  const markDirty = useCallback(() => {
    setDirty(true);
  }, [setDirty]);

  const totalWeight = members.filter((m) => m.enabled).reduce((sum, m) => sum + m.weight, 0);

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Toolbar */}
      <div className="flex items-center gap-4 px-4 py-2 border-b border-gray-200 bg-gray-50 flex-shrink-0">
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-gray-600">Strategy:</span>
          <select
            value={strategy}
            onChange={(e) => {
              setStrategy(e.target.value as EnsembleStrategy);
              markDirty();
            }}
            className="text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 bg-white focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            {STRATEGIES.map((s) => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-gray-600">Output:</span>
          <input
            type="text"
            value={outputField}
            onChange={(e) => {
              setOutputField(e.target.value);
              markDirty();
            }}
            className="w-24 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>
        {(strategy === 'VOTING' || strategy === 'UNANIMOUS') && (
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-gray-600">Threshold:</span>
            <input
              type="number"
              value={threshold}
              onChange={(e) => {
                setThreshold(Number(e.target.value));
                markDirty();
              }}
              className="w-16 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 text-center focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              min={0}
              max={1}
              step={0.1}
            />
          </div>
        )}
        <div className="h-4 w-px bg-gray-300" />
        <Button
          size="sm"
          variant="outline"
          onClick={() => {
            addMember();
            markDirty();
          }}
          className="text-xs"
        >
          <Plus className="h-3 w-3" />
          Member
        </Button>
        <div className="ml-auto text-xs text-gray-500">
          {members.filter((m) => m.enabled).length} active / {members.length} total
        </div>
      </div>

      {/* Strategy description */}
      <div className="px-4 py-2 bg-blue-50 border-b border-blue-100 text-xs text-blue-700">
        {STRATEGIES.find((s) => s.value === strategy)?.description}
      </div>

      {/* Members list */}
      <div className="flex-1 overflow-auto p-4">
        {members.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
            <p className="text-sm">
              No ensemble members defined yet. Add member rules to combine their outputs.
            </p>
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                addMember();
                markDirty();
              }}
            >
              <Plus className="h-3 w-3" />
              Add Member
            </Button>
          </div>
        ) : (
          <div className="space-y-2">
            {/* Header */}
            <div className="flex items-center gap-3 px-3 py-1 text-xs font-medium text-gray-500">
              <div className="w-6" />
              <div className="w-8">On</div>
              <div className="flex-1">Alias</div>
              <div className="w-48">Rule ID</div>
              <div className="w-24 text-center">Weight</div>
              <div className="w-20 text-center">Pct</div>
              <div className="w-8" />
            </div>

            {members.map((member, index) => (
              <div
                key={member.id}
                className={`flex items-center gap-3 px-3 py-2 rounded-lg border transition-colors ${
                  member.enabled
                    ? 'border-gray-200 bg-white'
                    : 'border-gray-100 bg-gray-50 opacity-60'
                }`}
              >
                <GripVertical className="h-4 w-4 text-gray-300 flex-shrink-0 cursor-grab" />

                <button
                  onClick={() => {
                    updateMember(index, { enabled: !member.enabled });
                    markDirty();
                  }}
                  className="flex-shrink-0"
                  title={member.enabled ? 'Disable' : 'Enable'}
                >
                  {member.enabled ? (
                    <ToggleRight className="h-5 w-5 text-indigo-600" />
                  ) : (
                    <ToggleLeft className="h-5 w-5 text-gray-400" />
                  )}
                </button>

                <input
                  type="text"
                  value={member.alias}
                  onChange={(e) => {
                    updateMember(index, { alias: e.target.value });
                    markDirty();
                  }}
                  className="flex-1 text-sm h-8 rounded border border-gray-200 px-2 text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  placeholder="Member alias"
                />

                <input
                  type="text"
                  value={member.ruleId}
                  onChange={(e) => {
                    updateMember(index, { ruleId: e.target.value });
                    markDirty();
                  }}
                  className="w-48 text-sm h-8 rounded border border-gray-200 px-2 text-gray-900 font-mono text-xs focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  placeholder="Rule ID or alias"
                />

                <input
                  type="number"
                  value={member.weight}
                  onChange={(e) => {
                    updateMember(index, { weight: Number(e.target.value) });
                    markDirty();
                  }}
                  className="w-24 text-sm h-8 rounded border border-gray-200 px-2 text-gray-900 text-center focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  min={0}
                  step={0.1}
                />

                <div className="w-20 text-center text-xs text-gray-500">
                  {totalWeight > 0 && member.enabled
                    ? `${((member.weight / totalWeight) * 100).toFixed(1)}%`
                    : '--'}
                </div>

                <button
                  onClick={() => {
                    removeMember(index);
                    markDirty();
                  }}
                  className="p-1 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 flex-shrink-0"
                  title="Remove member"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
