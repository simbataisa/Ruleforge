import { useCallback } from 'react';
import { Plus, Trash2, Table2 } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { useDmnEditorStore } from '@/stores/dmn-editor.store';
import { useEditorStore } from '@/stores/editor.store';
import type { DmnHitPolicy } from '@/stores/dmn-editor.store';

const HIT_POLICIES: { value: DmnHitPolicy; label: string }[] = [
  { value: 'UNIQUE', label: 'U (Unique)' },
  { value: 'ANY', label: 'A (Any)' },
  { value: 'PRIORITY', label: 'P (Priority)' },
  { value: 'FIRST', label: 'F (First)' },
  { value: 'OUTPUT_ORDER', label: 'O (Output Order)' },
  { value: 'RULE_ORDER', label: 'R (Rule Order)' },
  { value: 'COLLECT', label: 'C (Collect)' },
];

const TYPE_REFS = [
  { value: 'string', label: 'string' },
  { value: 'number', label: 'number' },
  { value: 'boolean', label: 'boolean' },
  { value: 'date', label: 'date' },
];

export function DmnEditor() {
  const {
    decisions,
    activeDecisionIndex,
    addDecision,
    removeDecision,
    setActiveDecision,
    updateDecision,
    addInputColumn,
    removeInputColumn,
    updateInputColumn,
    addOutputColumn,
    removeOutputColumn,
    updateOutputColumn,
    addRule,
    removeRule,
    updateRuleEntry,
    updateRuleAnnotation,
  } = useDmnEditorStore();

  const setDirty = useEditorStore((s) => s.setDirty);

  const markDirty = useCallback(() => {
    setDirty(true);
  }, [setDirty]);

  const activeDecision = decisions[activeDecisionIndex];

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Decision tabs */}
      <div className="flex items-center gap-1 px-4 py-2 border-b border-gray-200 bg-gray-50 flex-shrink-0 overflow-x-auto">
        {decisions.map((dec, index) => (
          <button
            key={dec.id}
            onClick={() => setActiveDecision(index)}
            className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
              index === activeDecisionIndex
                ? 'bg-indigo-100 text-indigo-700 border border-indigo-200'
                : 'text-gray-600 hover:bg-gray-100 border border-transparent'
            }`}
          >
            <Table2 className="h-3 w-3" />
            {dec.name}
            {decisions.length > 1 && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  removeDecision(index);
                  markDirty();
                }}
                className="ml-1 p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500"
              >
                <Trash2 className="h-2.5 w-2.5" />
              </button>
            )}
          </button>
        ))}
        <Button
          size="sm"
          variant="ghost"
          onClick={() => {
            addDecision();
            markDirty();
          }}
          className="text-xs text-indigo-600"
        >
          <Plus className="h-3 w-3" />
          Decision
        </Button>
      </div>

      {!activeDecision ? (
        <div className="flex flex-col items-center justify-center flex-1 text-gray-400 gap-3">
          <p className="text-sm">No decisions defined. Add a decision table to get started.</p>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              addDecision();
              markDirty();
            }}
          >
            <Plus className="h-3 w-3" />
            Add Decision
          </Button>
        </div>
      ) : (
        <>
          {/* Decision toolbar */}
          <div className="flex items-center gap-3 px-4 py-2 border-b border-gray-200 flex-shrink-0">
            <div className="flex items-center gap-2">
              <span className="text-xs font-medium text-gray-600">Name:</span>
              <input
                type="text"
                value={activeDecision.name}
                onChange={(e) => {
                  updateDecision(activeDecisionIndex, { name: e.target.value });
                  markDirty();
                }}
                className="w-40 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-medium text-gray-600">Hit Policy:</span>
              <select
                value={activeDecision.hitPolicy}
                onChange={(e) => {
                  updateDecision(activeDecisionIndex, { hitPolicy: e.target.value as DmnHitPolicy });
                  markDirty();
                }}
                className="text-xs h-7 rounded border border-gray-300 px-1 text-gray-900 bg-white focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {HIT_POLICIES.map((hp) => (
                  <option key={hp.value} value={hp.value}>{hp.label}</option>
                ))}
              </select>
            </div>
            <div className="h-4 w-px bg-gray-300" />
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                addInputColumn(activeDecisionIndex);
                markDirty();
              }}
              className="text-xs"
            >
              <Plus className="h-3 w-3" />
              Input
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                addOutputColumn(activeDecisionIndex);
                markDirty();
              }}
              className="text-xs"
            >
              <Plus className="h-3 w-3" />
              Output
            </Button>
            <div className="h-4 w-px bg-gray-300" />
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                addRule(activeDecisionIndex);
                markDirty();
              }}
              className="text-xs"
            >
              <Plus className="h-3 w-3" />
              Rule
            </Button>
          </div>

          {/* Decision table */}
          <div className="flex-1 overflow-auto">
            {activeDecision.inputColumns.length === 0 && activeDecision.outputColumns.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
                <p className="text-sm">
                  Add input and output columns to define your DMN decision table.
                </p>
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      addInputColumn(activeDecisionIndex);
                      markDirty();
                    }}
                  >
                    <Plus className="h-3 w-3" />
                    Add Input
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      addOutputColumn(activeDecisionIndex);
                      markDirty();
                    }}
                  >
                    <Plus className="h-3 w-3" />
                    Add Output
                  </Button>
                </div>
              </div>
            ) : (
              <table className="w-full border-collapse text-sm">
                <thead>
                  {/* Section headers */}
                  <tr>
                    <th className="bg-gray-50 border border-gray-200 px-2 py-1 text-xs text-gray-500 w-10">#</th>
                    {activeDecision.inputColumns.length > 0 && (
                      <th
                        colSpan={activeDecision.inputColumns.length}
                        className="bg-blue-50 border border-blue-200 px-2 py-1 text-xs font-semibold text-blue-700 text-center"
                      >
                        Input (FEEL)
                      </th>
                    )}
                    {activeDecision.outputColumns.length > 0 && (
                      <th
                        colSpan={activeDecision.outputColumns.length}
                        className="bg-green-50 border border-green-200 px-2 py-1 text-xs font-semibold text-green-700 text-center"
                      >
                        Output
                      </th>
                    )}
                    <th className="bg-gray-50 border border-gray-200 px-2 py-1 text-xs text-gray-500 min-w-[120px]">
                      Annotation
                    </th>
                    <th className="bg-gray-50 border border-gray-200 w-8" />
                  </tr>
                  {/* Column headers */}
                  <tr>
                    <th className="bg-gray-50 border border-gray-200" />
                    {activeDecision.inputColumns.map((col, colIndex) => (
                      <th
                        key={col.id}
                        className="bg-blue-50 border border-blue-200 px-2 py-1 min-w-[140px]"
                      >
                        <div className="space-y-1">
                          <div className="flex items-center justify-between gap-1">
                            <input
                              type="text"
                              value={col.label}
                              onChange={(e) => {
                                updateInputColumn(activeDecisionIndex, colIndex, { label: e.target.value });
                                markDirty();
                              }}
                              className="text-xs font-semibold text-blue-800 bg-transparent border-none outline-none flex-1 min-w-0 focus:ring-1 focus:ring-indigo-400 rounded px-1"
                            />
                            <button
                              onClick={() => {
                                removeInputColumn(activeDecisionIndex, colIndex);
                                markDirty();
                              }}
                              className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 flex-shrink-0"
                            >
                              <Trash2 className="h-2.5 w-2.5" />
                            </button>
                          </div>
                          <div className="flex gap-1">
                            <input
                              type="text"
                              value={col.expression}
                              onChange={(e) => {
                                updateInputColumn(activeDecisionIndex, colIndex, { expression: e.target.value });
                                markDirty();
                              }}
                              placeholder="FEEL expr"
                              className="text-[10px] bg-white border border-gray-200 rounded px-1 py-0.5 text-gray-600 flex-1 min-w-0"
                            />
                            <select
                              value={col.typeRef}
                              onChange={(e) => {
                                updateInputColumn(activeDecisionIndex, colIndex, { typeRef: e.target.value });
                                markDirty();
                              }}
                              className="text-[10px] bg-white border border-gray-200 rounded px-1 py-0.5 text-gray-600"
                            >
                              {TYPE_REFS.map((t) => (
                                <option key={t.value} value={t.value}>{t.label}</option>
                              ))}
                            </select>
                          </div>
                        </div>
                      </th>
                    ))}
                    {activeDecision.outputColumns.map((col, colIndex) => (
                      <th
                        key={col.id}
                        className="bg-green-50 border border-green-200 px-2 py-1 min-w-[140px]"
                      >
                        <div className="space-y-1">
                          <div className="flex items-center justify-between gap-1">
                            <input
                              type="text"
                              value={col.label}
                              onChange={(e) => {
                                updateOutputColumn(activeDecisionIndex, colIndex, { label: e.target.value });
                                markDirty();
                              }}
                              className="text-xs font-semibold text-green-800 bg-transparent border-none outline-none flex-1 min-w-0 focus:ring-1 focus:ring-indigo-400 rounded px-1"
                            />
                            <button
                              onClick={() => {
                                removeOutputColumn(activeDecisionIndex, colIndex);
                                markDirty();
                              }}
                              className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 flex-shrink-0"
                            >
                              <Trash2 className="h-2.5 w-2.5" />
                            </button>
                          </div>
                          <div className="flex gap-1">
                            <input
                              type="text"
                              value={col.name}
                              onChange={(e) => {
                                updateOutputColumn(activeDecisionIndex, colIndex, { name: e.target.value });
                                markDirty();
                              }}
                              placeholder="Output name"
                              className="text-[10px] bg-white border border-gray-200 rounded px-1 py-0.5 text-gray-600 flex-1 min-w-0"
                            />
                            <select
                              value={col.typeRef}
                              onChange={(e) => {
                                updateOutputColumn(activeDecisionIndex, colIndex, { typeRef: e.target.value });
                                markDirty();
                              }}
                              className="text-[10px] bg-white border border-gray-200 rounded px-1 py-0.5 text-gray-600"
                            >
                              {TYPE_REFS.map((t) => (
                                <option key={t.value} value={t.value}>{t.label}</option>
                              ))}
                            </select>
                          </div>
                        </div>
                      </th>
                    ))}
                    <th className="bg-gray-50 border border-gray-200" />
                    <th className="bg-gray-50 border border-gray-200" />
                  </tr>
                </thead>
                <tbody>
                  {activeDecision.rules.length === 0 ? (
                    <tr>
                      <td
                        colSpan={activeDecision.inputColumns.length + activeDecision.outputColumns.length + 3}
                        className="border border-gray-200 px-4 py-6 text-center text-gray-400 text-sm"
                      >
                        No rules yet. Click "Rule" to add a decision rule row.
                      </td>
                    </tr>
                  ) : (
                    activeDecision.rules.map((rule, ruleIndex) => (
                      <tr key={rule.id} className="group">
                        <td className="border border-gray-200 px-2 py-1.5 text-xs text-gray-400 text-center bg-gray-50 w-10">
                          {ruleIndex + 1}
                        </td>
                        {activeDecision.inputColumns.map((col) => (
                          <td key={col.id} className="border border-gray-200 px-0 py-0">
                            <input
                              type="text"
                              value={rule.inputEntries[col.id] ?? ''}
                              onChange={(e) => {
                                updateRuleEntry(activeDecisionIndex, ruleIndex, col.id, e.target.value, 'input');
                                markDirty();
                              }}
                              placeholder="-"
                              className="w-full px-2 py-1.5 text-sm border-none outline-none bg-transparent focus:ring-1 focus:ring-indigo-400 font-mono text-xs"
                            />
                          </td>
                        ))}
                        {activeDecision.outputColumns.map((col) => (
                          <td key={col.id} className="border border-gray-200 px-0 py-0">
                            <input
                              type="text"
                              value={rule.outputEntries[col.id] ?? ''}
                              onChange={(e) => {
                                updateRuleEntry(activeDecisionIndex, ruleIndex, col.id, e.target.value, 'output');
                                markDirty();
                              }}
                              placeholder="-"
                              className="w-full px-2 py-1.5 text-sm border-none outline-none bg-transparent focus:ring-1 focus:ring-indigo-400"
                            />
                          </td>
                        ))}
                        <td className="border border-gray-200 px-0 py-0">
                          <input
                            type="text"
                            value={rule.annotation}
                            onChange={(e) => {
                              updateRuleAnnotation(activeDecisionIndex, ruleIndex, e.target.value);
                              markDirty();
                            }}
                            placeholder="Note"
                            className="w-full px-2 py-1.5 text-xs border-none outline-none bg-transparent text-gray-500 focus:ring-1 focus:ring-indigo-400"
                          />
                        </td>
                        <td className="border border-gray-200 px-1 py-1 w-8 bg-gray-50">
                          <button
                            onClick={() => {
                              removeRule(activeDecisionIndex, ruleIndex);
                              markDirty();
                            }}
                            className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  );
}
