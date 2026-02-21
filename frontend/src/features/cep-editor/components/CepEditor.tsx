import { useCallback } from 'react';
import { Plus, Trash2, Zap, Clock, Activity } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { useCepEditorStore } from '@/stores/cep-editor.store';
import { useEditorStore } from '@/stores/editor.store';
import type { CepPatternType, CepWindowType, CepActionType } from '@/stores/cep-editor.store';

const PATTERN_TYPES: { value: CepPatternType; label: string; description: string }[] = [
  { value: 'SIMPLE', label: 'Simple', description: 'Match a single event type' },
  { value: 'SEQUENCE', label: 'Sequence', description: 'Match ordered sequence of events' },
  { value: 'ABSENCE', label: 'Absence', description: 'Detect missing expected events' },
  { value: 'COUNT', label: 'Count', description: 'Count events exceeding threshold' },
  { value: 'AGGREGATION', label: 'Aggregation', description: 'Aggregate values over window' },
];

const WINDOW_TYPES: { value: CepWindowType; label: string }[] = [
  { value: 'TUMBLING', label: 'Tumbling' },
  { value: 'SLIDING', label: 'Sliding' },
  { value: 'SESSION', label: 'Session' },
];

const TIME_UNITS = [
  { value: 'SECONDS', label: 'Seconds' },
  { value: 'MINUTES', label: 'Minutes' },
  { value: 'HOURS', label: 'Hours' },
];

const ACTION_TYPES: { value: CepActionType; label: string }[] = [
  { value: 'EMIT', label: 'Emit Event' },
  { value: 'ALERT', label: 'Alert' },
  { value: 'INVOKE_RULE', label: 'Invoke Rule' },
];

export function CepEditor() {
  const {
    patterns,
    activePatternIndex,
    addPattern,
    removePattern,
    setActivePattern,
    updatePattern,
    addEvent,
    removeEvent,
    updateEvent,
    updateWindow,
    addAction,
    removeAction,
    updateAction,
  } = useCepEditorStore();

  const setDirty = useEditorStore((s) => s.setDirty);

  const markDirty = useCallback(() => {
    setDirty(true);
  }, [setDirty]);

  const activePattern = patterns[activePatternIndex];

  return (
    <div className="flex h-full bg-white">
      {/* Pattern sidebar */}
      <div className="w-56 flex-shrink-0 border-r border-gray-200 bg-gray-50 flex flex-col">
        <div className="flex items-center justify-between px-3 py-2 border-b border-gray-200">
          <span className="text-xs font-semibold text-gray-600 uppercase tracking-wider">Patterns</span>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              addPattern();
              markDirty();
            }}
            className="text-xs h-6 px-1.5"
          >
            <Plus className="h-3 w-3" />
          </Button>
        </div>
        <div className="flex-1 overflow-auto p-2 space-y-1">
          {patterns.length === 0 ? (
            <p className="text-xs text-gray-400 text-center py-4">No patterns</p>
          ) : (
            patterns.map((pat, index) => (
              <button
                key={pat.id}
                onClick={() => setActivePattern(index)}
                className={`w-full text-left px-3 py-2 rounded-md text-xs transition-colors ${
                  index === activePatternIndex
                    ? 'bg-indigo-100 text-indigo-700 font-medium'
                    : 'text-gray-700 hover:bg-gray-100'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Activity className="h-3 w-3 flex-shrink-0" />
                  <span className="truncate">{pat.name}</span>
                </div>
                <span className="text-[10px] text-gray-500 ml-5">{pat.type}</span>
              </button>
            ))
          )}
        </div>
      </div>

      {/* Pattern editor */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {!activePattern ? (
          <div className="flex flex-col items-center justify-center flex-1 text-gray-400 gap-3">
            <p className="text-sm">No CEP patterns defined. Add a pattern to start detecting complex events.</p>
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                addPattern();
                markDirty();
              }}
            >
              <Plus className="h-3 w-3" />
              Add Pattern
            </Button>
          </div>
        ) : (
          <>
            {/* Pattern config toolbar */}
            <div className="flex items-center gap-3 px-4 py-2 border-b border-gray-200 bg-gray-50 flex-shrink-0">
              <input
                type="text"
                value={activePattern.name}
                onChange={(e) => {
                  updatePattern(activePatternIndex, { name: e.target.value });
                  markDirty();
                }}
                className="w-40 text-sm font-medium h-7 rounded border border-gray-300 px-2 text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
              <select
                value={activePattern.type}
                onChange={(e) => {
                  updatePattern(activePatternIndex, { type: e.target.value as CepPatternType });
                  markDirty();
                }}
                className="text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 bg-white"
              >
                {PATTERN_TYPES.map((pt) => (
                  <option key={pt.value} value={pt.value}>{pt.label}</option>
                ))}
              </select>
              {(activePattern.type === 'COUNT' || activePattern.type === 'AGGREGATION') && (
                <div className="flex items-center gap-1">
                  <span className="text-xs text-gray-600">Threshold:</span>
                  <input
                    type="number"
                    value={activePattern.threshold}
                    onChange={(e) => {
                      updatePattern(activePatternIndex, { threshold: Number(e.target.value) });
                      markDirty();
                    }}
                    className="w-16 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 text-center"
                    min={1}
                  />
                </div>
              )}
              <div className="flex items-center gap-1">
                <span className="text-xs text-gray-600">Group by:</span>
                <input
                  type="text"
                  value={activePattern.groupByField}
                  onChange={(e) => {
                    updatePattern(activePatternIndex, { groupByField: e.target.value });
                    markDirty();
                  }}
                  placeholder="field"
                  className="w-24 text-xs h-7 rounded border border-gray-300 px-2 text-gray-900 placeholder:text-gray-400"
                />
              </div>
              <div className="ml-auto">
                <button
                  onClick={() => {
                    removePattern(activePatternIndex);
                    markDirty();
                  }}
                  className="p-1 rounded hover:bg-red-100 text-gray-400 hover:text-red-500"
                  title="Delete pattern"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>

            {/* Pattern description */}
            <div className="px-4 py-1.5 bg-blue-50 border-b border-blue-100 text-xs text-blue-700">
              {PATTERN_TYPES.find((p) => p.value === activePattern.type)?.description}
            </div>

            <div className="flex-1 overflow-auto p-4 space-y-6">
              {/* Time Window */}
              <section>
                <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-700 mb-3">
                  <Clock className="h-4 w-4" />
                  Time Window
                </h3>
                <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200">
                  <select
                    value={activePattern.window.type}
                    onChange={(e) => {
                      updateWindow(activePatternIndex, { type: e.target.value as CepWindowType });
                      markDirty();
                    }}
                    className="text-xs h-8 rounded border border-gray-300 px-2 text-gray-900 bg-white"
                  >
                    {WINDOW_TYPES.map((wt) => (
                      <option key={wt.value} value={wt.value}>{wt.label}</option>
                    ))}
                  </select>
                  <input
                    type="number"
                    value={activePattern.window.duration}
                    onChange={(e) => {
                      updateWindow(activePatternIndex, { duration: Number(e.target.value) });
                      markDirty();
                    }}
                    className="w-20 text-xs h-8 rounded border border-gray-300 px-2 text-gray-900 text-center"
                    min={1}
                  />
                  <select
                    value={activePattern.window.unit}
                    onChange={(e) => {
                      updateWindow(activePatternIndex, { unit: e.target.value as 'SECONDS' | 'MINUTES' | 'HOURS' });
                      markDirty();
                    }}
                    className="text-xs h-8 rounded border border-gray-300 px-2 text-gray-900 bg-white"
                  >
                    {TIME_UNITS.map((tu) => (
                      <option key={tu.value} value={tu.value}>{tu.label}</option>
                    ))}
                  </select>
                </div>
              </section>

              {/* Event Sources */}
              <section>
                <div className="flex items-center justify-between mb-3">
                  <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-700">
                    <Zap className="h-4 w-4" />
                    Event Sources
                  </h3>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      addEvent(activePatternIndex);
                      markDirty();
                    }}
                    className="text-xs"
                  >
                    <Plus className="h-3 w-3" />
                    Event
                  </Button>
                </div>
                {activePattern.events.length === 0 ? (
                  <p className="text-xs text-gray-400 text-center py-4 border border-dashed border-gray-200 rounded-lg">
                    No event sources. Add events to match against.
                  </p>
                ) : (
                  <div className="space-y-2">
                    {activePattern.events.map((evt, evtIndex) => (
                      <div
                        key={evt.id}
                        className="flex items-center gap-2 p-2 border border-gray-200 rounded-lg bg-white group"
                      >
                        <span className="text-xs text-gray-400 w-5 text-center">{evtIndex + 1}</span>
                        <input
                          type="text"
                          value={evt.name}
                          onChange={(e) => {
                            updateEvent(activePatternIndex, evtIndex, { name: e.target.value });
                            markDirty();
                          }}
                          placeholder="Event name"
                          className="w-32 text-xs h-7 rounded border border-gray-200 px-2 text-gray-900 placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                        />
                        <input
                          type="text"
                          value={evt.sourceField}
                          onChange={(e) => {
                            updateEvent(activePatternIndex, evtIndex, { sourceField: e.target.value });
                            markDirty();
                          }}
                          placeholder="Source field"
                          className="w-32 text-xs h-7 rounded border border-gray-200 px-2 text-gray-900 placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                        />
                        <input
                          type="text"
                          value={evt.filterExpression}
                          onChange={(e) => {
                            updateEvent(activePatternIndex, evtIndex, { filterExpression: e.target.value });
                            markDirty();
                          }}
                          placeholder="Filter expression"
                          className="flex-1 text-xs h-7 rounded border border-gray-200 px-2 text-gray-900 font-mono placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                        />
                        <button
                          onClick={() => {
                            removeEvent(activePatternIndex, evtIndex);
                            markDirty();
                          }}
                          className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          <Trash2 className="h-3 w-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </section>

              {/* Actions */}
              <section>
                <div className="flex items-center justify-between mb-3">
                  <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-700">
                    <Zap className="h-4 w-4" />
                    Actions
                  </h3>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      addAction(activePatternIndex);
                      markDirty();
                    }}
                    className="text-xs"
                  >
                    <Plus className="h-3 w-3" />
                    Action
                  </Button>
                </div>
                {activePattern.actions.length === 0 ? (
                  <p className="text-xs text-gray-400 text-center py-4 border border-dashed border-gray-200 rounded-lg">
                    No actions. Add actions to trigger when the pattern matches.
                  </p>
                ) : (
                  <div className="space-y-2">
                    {activePattern.actions.map((action, actIndex) => (
                      <div
                        key={action.id}
                        className="flex items-center gap-2 p-2 border border-gray-200 rounded-lg bg-white group"
                      >
                        <select
                          value={action.type}
                          onChange={(e) => {
                            updateAction(activePatternIndex, actIndex, { type: e.target.value as CepActionType });
                            markDirty();
                          }}
                          className="text-xs h-7 rounded border border-gray-200 px-1 text-gray-900 bg-white"
                        >
                          {ACTION_TYPES.map((at) => (
                            <option key={at.value} value={at.value}>{at.label}</option>
                          ))}
                        </select>
                        <input
                          type="text"
                          value={action.target}
                          onChange={(e) => {
                            updateAction(activePatternIndex, actIndex, { target: e.target.value });
                            markDirty();
                          }}
                          placeholder="Target"
                          className="w-40 text-xs h-7 rounded border border-gray-200 px-2 text-gray-900 placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                        />
                        <input
                          type="text"
                          value={action.payload}
                          onChange={(e) => {
                            updateAction(activePatternIndex, actIndex, { payload: e.target.value });
                            markDirty();
                          }}
                          placeholder="Payload expression"
                          className="flex-1 text-xs h-7 rounded border border-gray-200 px-2 text-gray-900 font-mono placeholder:text-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                        />
                        <button
                          onClick={() => {
                            removeAction(activePatternIndex, actIndex);
                            markDirty();
                          }}
                          className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          <Trash2 className="h-3 w-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
