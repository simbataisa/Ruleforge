import { useCallback } from 'react';
import { Plus, Trash2, ChevronDown, ChevronRight } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { useScorecardEditorStore } from '@/stores/scorecard-editor.store';
import { useEditorStore } from '@/stores/editor.store';
import { useState } from 'react';

const OPERATORS = [
  { value: 'EQ', label: '=' },
  { value: 'NEQ', label: '!=' },
  { value: 'GT', label: '>' },
  { value: 'GTE', label: '>=' },
  { value: 'LT', label: '<' },
  { value: 'LTE', label: '<=' },
  { value: 'BETWEEN', label: 'Between' },
  { value: 'IN', label: 'In' },
];

const DATA_TYPES = [
  { value: 'STRING', label: 'String' },
  { value: 'NUMBER', label: 'Number' },
  { value: 'BOOLEAN', label: 'Boolean' },
  { value: 'DATE', label: 'Date' },
];

export function ScorecardEditor() {
  const {
    baseScore,
    outputField,
    reasonCodes,
    characteristics,
    setBaseScore,
    setOutputField,
    setReasonCodes,
    addCharacteristic,
    removeCharacteristic,
    updateCharacteristic,
    addBin,
    removeBin,
    updateBin,
  } = useScorecardEditorStore();

  const setDirty = useEditorStore((s) => s.setDirty);
  const [expandedChars, setExpandedChars] = useState<Set<number>>(new Set());

  const markDirty = useCallback(() => {
    setDirty(true);
  }, [setDirty]);

  const toggleExpanded = (index: number) => {
    setExpandedChars((prev) => {
      const next = new Set(prev);
      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }
      return next;
    });
  };

  return (
    <div className="flex flex-col h-full bg-white">
      {/* Toolbar */}
      <div className="flex items-center gap-4 px-4 py-2 border-b border-gray-200 bg-gray-50 flex-shrink-0">
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-gray-600">Base Score:</span>
          <input
            type="number"
            value={baseScore}
            onChange={(e) => {
              setBaseScore(Number(e.target.value));
              markDirty();
            }}
            className="w-20 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-gray-600">Output Field:</span>
          <input
            type="text"
            value={outputField}
            onChange={(e) => {
              setOutputField(e.target.value);
              markDirty();
            }}
            className="w-28 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>
        <label className="flex items-center gap-1.5 text-xs text-gray-600 cursor-pointer">
          <input
            type="checkbox"
            checked={reasonCodes}
            onChange={(e) => {
              setReasonCodes(e.target.checked);
              markDirty();
            }}
            className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
          />
          Reason Codes
        </label>
        <div className="h-4 w-px bg-gray-300" />
        <Button
          size="sm"
          variant="outline"
          onClick={() => {
            addCharacteristic();
            markDirty();
          }}
          className="text-xs"
        >
          <Plus className="h-3 w-3" />
          Characteristic
        </Button>
      </div>

      {/* Characteristics */}
      <div className="flex-1 overflow-auto p-4">
        {characteristics.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
            <p className="text-sm">
              No characteristics defined yet. Add characteristics and their scoring bins to build your scorecard.
            </p>
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                addCharacteristic();
                markDirty();
              }}
            >
              <Plus className="h-3 w-3" />
              Add Characteristic
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            {characteristics.map((char, charIndex) => (
              <div
                key={char.id}
                className="border border-gray-200 rounded-lg overflow-hidden"
              >
                {/* Characteristic header */}
                <div className="flex items-center gap-3 px-4 py-2 bg-gray-50 border-b border-gray-200">
                  <button
                    onClick={() => toggleExpanded(charIndex)}
                    className="p-0.5 rounded hover:bg-gray-200"
                  >
                    {expandedChars.has(charIndex) ? (
                      <ChevronDown className="h-4 w-4 text-gray-500" />
                    ) : (
                      <ChevronRight className="h-4 w-4 text-gray-500" />
                    )}
                  </button>
                  <input
                    type="text"
                    value={char.name}
                    onChange={(e) => {
                      updateCharacteristic(charIndex, { name: e.target.value });
                      markDirty();
                    }}
                    className="text-sm font-medium bg-transparent border-none outline-none flex-1 min-w-0 focus:ring-1 focus:ring-indigo-400 rounded px-1 text-gray-900"
                  />
                  <div className="flex items-center gap-2">
                    <input
                      type="text"
                      value={char.field}
                      onChange={(e) => {
                        updateCharacteristic(charIndex, { field: e.target.value });
                        markDirty();
                      }}
                      placeholder="Field path"
                      className="w-32 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                    <select
                      value={char.dataType}
                      onChange={(e) => {
                        updateCharacteristic(charIndex, { dataType: e.target.value as 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE' });
                        markDirty();
                      }}
                      className="text-xs h-7 rounded border border-gray-300 px-1 text-gray-600 bg-white"
                    >
                      {DATA_TYPES.map((dt) => (
                        <option key={dt.value} value={dt.value}>{dt.label}</option>
                      ))}
                    </select>
                    <div className="flex items-center gap-1">
                      <span className="text-xs text-gray-500">W:</span>
                      <input
                        type="number"
                        value={char.weight}
                        onChange={(e) => {
                          updateCharacteristic(charIndex, { weight: Number(e.target.value) });
                          markDirty();
                        }}
                        className="w-14 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 text-center focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                        min={0}
                        step={0.1}
                      />
                    </div>
                    <button
                      onClick={() => {
                        removeCharacteristic(charIndex);
                        markDirty();
                      }}
                      className="p-1 rounded hover:bg-red-100 text-gray-400 hover:text-red-500"
                      title="Remove characteristic"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>

                {/* Bins table (expanded) */}
                {expandedChars.has(charIndex) && (
                  <div className="p-3">
                    <table className="w-full border-collapse text-sm">
                      <thead>
                        <tr>
                          <th className="text-left text-xs font-medium text-gray-500 px-2 py-1 w-40">Label</th>
                          <th className="text-left text-xs font-medium text-gray-500 px-2 py-1 w-24">Operator</th>
                          <th className="text-left text-xs font-medium text-gray-500 px-2 py-1">Value</th>
                          <th className="text-left text-xs font-medium text-gray-500 px-2 py-1 w-20">Score</th>
                          <th className="w-8" />
                        </tr>
                      </thead>
                      <tbody>
                        {char.bins.length === 0 ? (
                          <tr>
                            <td colSpan={5} className="text-center text-xs text-gray-400 py-4">
                              No bins. Click "Add Bin" to define scoring ranges.
                            </td>
                          </tr>
                        ) : (
                          char.bins.map((bin, binIndex) => (
                            <tr key={bin.id} className="group">
                              <td className="px-1 py-1">
                                <input
                                  type="text"
                                  value={bin.label}
                                  onChange={(e) => {
                                    updateBin(charIndex, binIndex, { label: e.target.value });
                                    markDirty();
                                  }}
                                  placeholder="Bin label"
                                  className="w-full text-xs h-7 rounded border border-gray-200 px-2 text-gray-900 placeholder:text-gray-300 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                                />
                              </td>
                              <td className="px-1 py-1">
                                <select
                                  value={bin.operator}
                                  onChange={(e) => {
                                    updateBin(charIndex, binIndex, { operator: e.target.value });
                                    markDirty();
                                  }}
                                  className="w-full text-xs h-7 rounded border border-gray-200 px-1 text-gray-600 bg-white"
                                >
                                  {OPERATORS.map((op) => (
                                    <option key={op.value} value={op.value}>{op.label}</option>
                                  ))}
                                </select>
                              </td>
                              <td className="px-1 py-1">
                                <input
                                  type="text"
                                  value={bin.value}
                                  onChange={(e) => {
                                    updateBin(charIndex, binIndex, { value: e.target.value });
                                    markDirty();
                                  }}
                                  placeholder="Value or range"
                                  className="w-full text-xs h-7 rounded border border-gray-200 px-2 text-gray-900 placeholder:text-gray-300 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                                />
                              </td>
                              <td className="px-1 py-1">
                                <input
                                  type="number"
                                  value={bin.score}
                                  onChange={(e) => {
                                    updateBin(charIndex, binIndex, { score: Number(e.target.value) });
                                    markDirty();
                                  }}
                                  className="w-full text-xs h-7 rounded border border-gray-200 px-2 text-gray-900 text-center focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                                />
                              </td>
                              <td className="px-1 py-1">
                                <button
                                  onClick={() => {
                                    removeBin(charIndex, binIndex);
                                    markDirty();
                                  }}
                                  className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                                  title="Remove bin"
                                >
                                  <Trash2 className="h-3 w-3" />
                                </button>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => {
                        addBin(charIndex);
                        markDirty();
                      }}
                      className="text-xs mt-2 text-indigo-600 hover:text-indigo-700"
                    >
                      <Plus className="h-3 w-3" />
                      Add Bin
                    </Button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
