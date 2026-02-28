import { useState, useEffect } from 'react';
import { ReactFlow, Background, Controls } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { buildTreeFlowNodes } from '../utils/buildTreeFlowNodes';
import { JsonModelEditor } from './JsonModelEditor';
import type { Node, Edge } from '@xyflow/react';

interface Props {
  ruleType: string;
  definition: Record<string, unknown>;
  height?: string;
}

// ─── TABLE read-only view ────────────────────────────────────────────────────

function TableReadOnlyView({ definition }: { definition: Record<string, unknown> }) {
  const columns = (definition.columns as Array<Record<string, unknown>>) ?? [];
  const rows = (definition.rows as Array<Record<string, unknown>>) ?? [];
  const hitPolicy = (definition.hitPolicy as string) ?? 'FIRST';

  const conditionCols = columns.filter(
    (c) => (c.columnType ?? c.role) === 'CONDITION',
  );
  const actionCols = columns.filter(
    (c) => (c.columnType ?? c.role) === 'ACTION',
  );

  if (columns.length === 0) {
    return (
      <div className="flex items-center justify-center h-full text-gray-400 text-sm">
        No columns defined
      </div>
    );
  }

  return (
    <div className="h-full overflow-auto p-3">
      <div className="mb-2 text-xs text-gray-500">
        Hit Policy: <span className="font-medium">{hitPolicy}</span>
      </div>
      <table className="min-w-full text-xs border-collapse">
        <thead className="sticky top-0 z-10">
          <tr>
            <th className="border border-gray-200 bg-gray-100 px-2 py-1 text-gray-500 w-8">#</th>
            {conditionCols.map((col, i) => (
              <th
                key={i}
                className="border border-blue-200 bg-blue-50 px-2 py-1 text-blue-800 font-medium text-left whitespace-nowrap"
              >
                {String(col.header ?? col.name ?? col.fieldPath ?? '')}
              </th>
            ))}
            {actionCols.map((col, i) => (
              <th
                key={i}
                className="border border-green-200 bg-green-50 px-2 py-1 text-green-800 font-medium text-left whitespace-nowrap"
              >
                {String(col.header ?? col.name ?? col.fieldPath ?? '')}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + 1}
                className="border border-gray-200 px-2 py-4 text-center text-gray-400"
              >
                No rows defined
              </td>
            </tr>
          ) : (
            rows.map((row, rowIdx) => {
              // Engine format: rows have conditions[] and actions[] arrays (positional)
              // UI/cells format: rows have cells[] array
              const conditions = (row.conditions as Array<Record<string, unknown>>) ?? [];
              const actions = (row.actions as Array<Record<string, unknown>>) ?? [];
              // Interleave into column order: all condition cols first, then action cols
              const allValues = [
                ...conditions.map((c) => String(c.value ?? c.operator ?? '')),
                ...actions.map((a) => String(a.value ?? '')),
              ];
              // Fallback: legacy cells[] format
              const cells = (row.cells as Array<Record<string, unknown>>) ?? [];
              const values = allValues.length > 0 ? allValues
                : cells.map((c) => String(c.value ?? c.operator ?? ''));
              return (
                <tr key={rowIdx} className="hover:bg-gray-50">
                  <td className="border border-gray-200 px-2 py-1 text-gray-400 text-center">
                    {rowIdx + 1}
                  </td>
                  {values.map((val, ci) => (
                    <td
                      key={ci}
                      className="border border-gray-200 px-2 py-1 text-gray-700 font-mono"
                    >
                      {val}
                    </td>
                  ))}
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}

// ─── TREE read-only view ─────────────────────────────────────────────────────

function TreeReadOnlyView({
  definition,
  height,
}: {
  definition: Record<string, unknown>;
  height?: string;
}) {
  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);

  useEffect(() => {
    const { nodes: n, edges: e } = buildTreeFlowNodes(definition);
    setNodes(n);
    setEdges(e);
  }, [definition]);

  return (
    <div style={{ height: height ?? '100%', width: '100%' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodesDraggable={false}
        nodesConnectable={false}
        elementsSelectable={false}
        panOnDrag={true}
        zoomOnScroll={true}
        fitView
      >
        <Background />
        <Controls />
      </ReactFlow>
    </div>
  );
}

// ─── ENSEMBLE read-only view ─────────────────────────────────────────────────

const STRATEGY_LABELS: Record<string, string> = {
  MAJORITY_VOTE: 'Majority Vote',
  WEIGHTED_AVERAGE: 'Weighted Average',
  WEIGHTED_VOTE: 'Weighted Vote',
  AVERAGE: 'Average',
  FIRST_MATCH: 'First Match',
  MAX: 'Maximum',
  MIN: 'Minimum',
};

function EnsembleReadOnlyView({ definition }: { definition: Record<string, unknown> }) {
  const strategy = (definition.strategy as string) ?? 'MAJORITY_VOTE';
  const outputField = (definition.outputField as string) ?? '';
  const threshold = definition.threshold as number | undefined;
  const members = (definition.members as Array<Record<string, unknown>>) ?? [];
  const enabledMembers = members.filter((m) => m.enabled !== false);
  const totalWeight = enabledMembers.reduce((sum, m) => sum + ((m.weight as number) ?? 1), 0);

  return (
    <div className="h-full overflow-auto p-3 space-y-3">
      {/* Config summary */}
      <div className="flex flex-wrap gap-3 p-3 rounded-md border border-gray-200 bg-gray-50 text-xs">
        <div>
          <span className="text-gray-500 font-medium mr-1">Strategy:</span>
          <span className="text-indigo-700 font-semibold">{STRATEGY_LABELS[strategy] ?? strategy}</span>
        </div>
        {outputField && (
          <div>
            <span className="text-gray-500 font-medium mr-1">Output field:</span>
            <span className="font-mono text-gray-800">{outputField}</span>
          </div>
        )}
        {threshold !== undefined && (
          <div>
            <span className="text-gray-500 font-medium mr-1">Threshold:</span>
            <span className="text-gray-800">{threshold}</span>
          </div>
        )}
        <div>
          <span className="text-gray-500 font-medium mr-1">Members:</span>
          <span className="text-gray-800">{enabledMembers.length} active / {members.length} total</span>
        </div>
      </div>

      {/* Members table */}
      {members.length === 0 ? (
        <div className="flex items-center justify-center h-24 text-gray-400 text-sm">
          No ensemble members defined
        </div>
      ) : (
        <table className="min-w-full text-xs border-collapse">
          <thead>
            <tr className="bg-gray-50">
              <th className="border border-gray-200 px-2 py-1.5 text-left text-gray-600 font-medium">Status</th>
              <th className="border border-gray-200 px-2 py-1.5 text-left text-gray-600 font-medium">Alias</th>
              <th className="border border-gray-200 px-2 py-1.5 text-left text-gray-600 font-medium">Rule ID</th>
              <th className="border border-gray-200 px-2 py-1.5 text-right text-gray-600 font-medium">Weight</th>
              <th className="border border-gray-200 px-2 py-1.5 text-right text-gray-600 font-medium">%</th>
            </tr>
          </thead>
          <tbody>
            {members.map((m, i) => {
              const enabled = m.enabled !== false;
              const weight = (m.weight as number) ?? 1;
              const pct = totalWeight > 0 ? ((weight / totalWeight) * 100).toFixed(1) : '—';
              return (
                <tr key={i} className={enabled ? 'bg-white' : 'bg-gray-50 opacity-60'}>
                  <td className="border border-gray-200 px-2 py-1.5 text-center">
                    <span className={`inline-block w-2 h-2 rounded-full ${enabled ? 'bg-green-500' : 'bg-gray-300'}`} />
                  </td>
                  <td className="border border-gray-200 px-2 py-1.5 text-gray-700 font-medium">
                    {String(m.alias ?? '—')}
                  </td>
                  <td className="border border-gray-200 px-2 py-1.5 font-mono text-gray-500 text-[11px]">
                    {String(m.ruleId ?? '—')}
                  </td>
                  <td className="border border-gray-200 px-2 py-1.5 text-right text-gray-700">{weight}</td>
                  <td className="border border-gray-200 px-2 py-1.5 text-right text-gray-500">{enabled ? `${pct}%` : '—'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}

// ─── SCORECARD read-only view ────────────────────────────────────────────────

function ScorecardReadOnlyView({ definition }: { definition: Record<string, unknown> }) {
  // Support engine format (categories/attributes) and UI format (characteristics/bins)
  const baseScore = (definition.baseScore as number) ?? 0;
  const outputField = (definition.outputField as string) ?? 'score';
  const reasonCodes = definition.reasonCodes === true;

  type Bin = { label?: string; operator?: string; value?: string | number; score?: number };
  type Characteristic = { name?: string; field?: string; factPath?: string; weight?: number; bins?: Bin[]; attributes?: Array<Record<string, unknown>> };

  let characteristics: Characteristic[] = [];

  const engineCats = definition.categories as Array<Record<string, unknown>> | undefined;
  const uiChars = definition.characteristics as Characteristic[] | undefined;

  if (engineCats && engineCats.length > 0) {
    characteristics = engineCats.map((cat) => {
      const attrs = (cat.attributes as Array<Record<string, unknown>>) ?? [];
      const bins: Bin[] = attrs.flatMap((attr) => {
        const ranges = (attr.ranges as Array<Record<string, unknown>>) ?? [];
        return ranges.map((r) => ({
          label: String(r.label ?? r.value ?? ''),
          operator: String(r.operator ?? ''),
          value: String(r.value ?? ''),
          score: Number(r.points ?? 0),
        }));
      });
      return {
        name: String(cat.name ?? cat.factPath ?? ''),
        field: String(cat.factPath ?? ''),
        weight: Number(cat.weight ?? 1),
        bins,
      };
    });
  } else if (uiChars) {
    characteristics = uiChars;
  }

  if (characteristics.length === 0) {
    return (
      <div className="h-full flex items-center justify-center text-sm text-gray-400 italic">
        No characteristics defined
      </div>
    );
  }

  return (
    <div className="h-full overflow-auto p-3 space-y-3">
      {/* Summary bar */}
      <div className="flex flex-wrap gap-3 p-3 rounded-md border border-gray-200 bg-gray-50 text-xs">
        <div>
          <span className="text-gray-500 font-medium mr-1">Base Score:</span>
          <span className="font-semibold text-indigo-700">{baseScore}</span>
        </div>
        <div>
          <span className="text-gray-500 font-medium mr-1">Output Field:</span>
          <span className="font-mono text-gray-700">{outputField}</span>
        </div>
        <div>
          <span className="text-gray-500 font-medium mr-1">Characteristics:</span>
          <span className="font-semibold text-gray-700">{characteristics.length}</span>
        </div>
        {reasonCodes && (
          <div className="text-emerald-700 font-medium">Reason Codes: On</div>
        )}
      </div>

      {/* Characteristics */}
      {characteristics.map((char, ci) => {
        const bins = char.bins ?? [];
        return (
          <div key={ci} className="border border-gray-200 rounded-md overflow-hidden">
            <div className="flex items-center gap-2 px-3 py-2 bg-gray-50 border-b border-gray-200">
              <span className="font-medium text-sm text-gray-800">{char.name ?? `Characteristic ${ci + 1}`}</span>
              {(char.field ?? char.factPath) && (
                <span className="font-mono text-xs text-gray-500">({char.field ?? char.factPath})</span>
              )}
              {char.weight != null && char.weight !== 1 && (
                <span className="ml-auto text-xs text-gray-500">weight: {char.weight}</span>
              )}
            </div>
            {bins.length === 0 ? (
              <div className="px-3 py-2 text-xs text-gray-400 italic">No bins defined</div>
            ) : (
              <table className="w-full text-xs">
                <thead>
                  <tr className="bg-gray-50 text-gray-500 font-medium">
                    <th className="border-b border-gray-200 px-3 py-1.5 text-left">Label</th>
                    <th className="border-b border-gray-200 px-3 py-1.5 text-left">Condition</th>
                    <th className="border-b border-gray-200 px-3 py-1.5 text-right">Score</th>
                  </tr>
                </thead>
                <tbody>
                  {bins.map((bin, bi) => (
                    <tr key={bi} className={bi % 2 === 0 ? 'bg-white' : 'bg-gray-50/50'}>
                      <td className="border-b border-gray-100 px-3 py-1.5 text-gray-700">{bin.label ?? '—'}</td>
                      <td className="border-b border-gray-100 px-3 py-1.5 font-mono text-gray-500">
                        {bin.operator ? `${bin.operator} ${bin.value ?? ''}` : String(bin.value ?? '—')}
                      </td>
                      <td className="border-b border-gray-100 px-3 py-1.5 text-right font-semibold text-indigo-700">
                        {bin.score ?? 0}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        );
      })}
    </div>
  );
}

// ─── DMN read-only view ───────────────────────────────────────────────────────

function DmnReadOnlyView({ definition }: { definition: Record<string, unknown> }) {
  const rawDecisions = (definition.decisions as Array<Record<string, unknown>>) ?? [];

  if (rawDecisions.length === 0) {
    return (
      <div className="flex items-center justify-center h-full text-gray-400 text-sm">
        No decisions defined
      </div>
    );
  }

  return (
    <div className="h-full overflow-auto p-3 space-y-4">
      {rawDecisions.map((d, di) => {
        // Support both engine format (decisionTable wrapper) and UI format (flat)
        const dt = (d.decisionTable as Record<string, unknown>) ?? d;
        const hitPolicy = String((dt.hitPolicy ?? d.hitPolicy) ?? 'UNIQUE');
        const decisionName = String(d.name ?? `Decision ${di + 1}`);
        const inputClauses = (dt.inputClauses ?? dt.inputColumns ?? []) as Array<Record<string, unknown> | string>;
        const outputClauses = (dt.outputClauses ?? dt.outputColumns ?? []) as Array<Record<string, unknown> | string>;
        const rules = (dt.rules ?? []) as Array<Record<string, unknown>>;

        const inputCols = inputClauses.map((c) =>
          typeof c === 'string' ? c : String((c.label ?? c.name ?? c.expression) || '')
        );
        const outputCols = outputClauses.map((c) =>
          typeof c === 'string' ? c : String((c.label ?? c.name) || '')
        );

        return (
          <div key={di} className="rounded-md border border-gray-200 overflow-hidden">
            <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-50 border-b border-gray-200 text-xs">
              <span className="font-semibold text-gray-700">{decisionName}</span>
              <span className="text-gray-400">·</span>
              <span className="text-gray-500">Hit Policy: <span className="font-medium text-indigo-700">{hitPolicy}</span></span>
              <span className="ml-auto text-gray-400">{rules.length} rule{rules.length !== 1 ? 's' : ''}</span>
            </div>
            {inputCols.length === 0 && outputCols.length === 0 ? (
              <div className="px-3 py-4 text-xs text-gray-400 text-center">No columns defined</div>
            ) : (
              <div className="overflow-auto">
                <table className="min-w-full text-xs border-collapse">
                  <thead>
                    <tr>
                      <th className="border border-gray-200 bg-gray-100 px-2 py-1 text-gray-500 w-8">#</th>
                      {inputCols.map((col, i) => (
                        <th key={i} className="border border-blue-200 bg-blue-50 px-2 py-1 text-blue-800 font-medium text-left whitespace-nowrap">
                          {col}
                        </th>
                      ))}
                      {outputCols.map((col, i) => (
                        <th key={i} className="border border-green-200 bg-green-50 px-2 py-1 text-green-800 font-medium text-left whitespace-nowrap">
                          {col}
                        </th>
                      ))}
                      <th className="border border-gray-200 bg-gray-50 px-2 py-1 text-gray-500 font-medium text-left whitespace-nowrap">Note</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rules.length === 0 ? (
                      <tr><td colSpan={inputCols.length + outputCols.length + 2} className="px-2 py-4 text-center text-gray-400">No rules</td></tr>
                    ) : rules.map((r, ri) => {
                      const inputEntries = Array.isArray(r.inputEntries)
                        ? r.inputEntries as string[]
                        : inputCols.map((_, i) => String((r.inputEntries as Record<string, string>)?.[String(i)] ?? ''));
                      const outputEntries = Array.isArray(r.outputEntries)
                        ? r.outputEntries as string[]
                        : outputCols.map((_, i) => String((r.outputEntries as Record<string, string>)?.[String(i)] ?? ''));
                      return (
                        <tr key={ri} className="hover:bg-gray-50">
                          <td className="border border-gray-200 px-2 py-1 text-gray-400 text-center">{ri + 1}</td>
                          {inputEntries.map((v, i) => (
                            <td key={i} className="border border-gray-200 px-2 py-1 text-gray-700 font-mono">{v || <span className="text-gray-300">—</span>}</td>
                          ))}
                          {outputEntries.map((v, i) => (
                            <td key={i} className="border border-gray-200 px-2 py-1 text-gray-700 font-mono">{v || <span className="text-gray-300">—</span>}</td>
                          ))}
                          <td className="border border-gray-200 px-2 py-1 text-gray-400 italic text-[11px]">{String(r.annotation ?? '')}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

// ─── CEP read-only view ───────────────────────────────────────────────────────

const CEP_PATTERN_LABELS: Record<string, string> = {
  SIMPLE: 'Simple', SEQUENCE: 'Sequence', ABSENCE: 'Absence',
  COUNT: 'Count / Frequency', AGGREGATION: 'Aggregation',
};
const CEP_ACTION_LABELS: Record<string, string> = {
  EMIT: 'Emit Event', EMIT_EVENT: 'Emit Event', ALERT: 'Alert', INVOKE_RULE: 'Invoke Rule',
};

function CepReadOnlyView({ definition }: { definition: Record<string, unknown> }) {
  // Support UI format (patterns[]) and engine format (patternType flat)
  const patterns = (definition.patterns as Array<Record<string, unknown>>) ?? [];

  // Engine flat format — synthesise a single pattern for display
  const displayPatterns: Array<Record<string, unknown>> = patterns.length > 0 ? patterns : [{
    name: 'Pattern',
    type: String(definition.patternType ?? 'SIMPLE'),
    events: (definition.eventSchemas as Array<Record<string, unknown>>) ?? [],
    window: definition.timeWindow as Record<string, unknown> ?? {},
    threshold: definition.threshold,
    groupByField: definition.groupByField,
    actions: (definition.actions as Array<Record<string, unknown>>) ?? [],
    _engineFormat: true,
  }];

  return (
    <div className="h-full overflow-auto p-3 space-y-3">
      {/* Topics (engine format) */}
      {(definition.inputTopic != null || definition.outputTopic != null) && (
        <div className="flex gap-4 px-3 py-2 rounded-md bg-gray-50 border border-gray-200 text-xs text-gray-600">
          {definition.inputTopic != null && <span>Input topic: <span className="font-mono text-gray-800">{String(definition.inputTopic)}</span></span>}
          {definition.outputTopic != null && <span>Output topic: <span className="font-mono text-gray-800">{String(definition.outputTopic)}</span></span>}
        </div>
      )}

      {displayPatterns.map((p, pi) => {
        const isEngine = Boolean(p._engineFormat);
        const patType = String(p.type ?? 'SIMPLE');
        const window = p.window as Record<string, unknown> ?? {};
        const windowDuration = Number(window.duration ?? 0);
        const windowUnit = isEngine
          ? (windowDuration >= 3600 ? `${windowDuration / 3600}h` : windowDuration >= 60 ? `${windowDuration / 60}m` : `${windowDuration}s`)
          : `${windowDuration} ${String(window.unit ?? 'SECONDS').toLowerCase()}`;
        const events = (p.events as Array<Record<string, unknown>>) ?? [];
        const actions = (p.actions as Array<Record<string, unknown>>) ?? [];
        const threshold = p.threshold as number | undefined;
        const groupBy = p.groupByField as string | undefined;

        return (
          <div key={pi} className="rounded-md border border-gray-200 overflow-hidden">
            {/* Pattern header */}
            <div className="flex flex-wrap items-center gap-3 px-3 py-1.5 bg-gray-50 border-b border-gray-200 text-xs">
              <span className="font-semibold text-gray-700">{String(p.name ?? `Pattern ${pi + 1}`)}</span>
              <span className="text-indigo-700 font-medium">{CEP_PATTERN_LABELS[patType] ?? patType}</span>
              <span className="text-gray-500">Window: <span className="font-medium">{windowUnit} ({String(window.type ?? 'TUMBLING')})</span></span>
              {threshold !== undefined && threshold > 1 && <span className="text-gray-500">Threshold: <span className="font-medium">{threshold}</span></span>}
              {groupBy && <span className="text-gray-500">Group by: <span className="font-mono">{groupBy}</span></span>}
            </div>

            <div className="grid grid-cols-2 divide-x divide-gray-200">
              {/* Events */}
              <div className="p-2">
                <p className="text-[11px] font-semibold uppercase tracking-wide text-blue-600 mb-1.5 px-1">Events ({events.length})</p>
                {events.length === 0
                  ? <p className="text-xs text-gray-400 px-1">No events</p>
                  : events.map((e, ei) => (
                    <div key={ei} className="text-xs px-1 py-0.5">
                      <span className="font-medium text-gray-700">{String(isEngine ? (e.eventType ?? e.name ?? '') : (e.name ?? ''))}</span>
                      {(e.filterExpression != null || e.filterConditions != null) && (
                        <span className="ml-2 font-mono text-gray-400 text-[11px] truncate">
                          {isEngine
                            ? (e.filterConditions as Array<Record<string, unknown>> ?? []).map(fc => `${fc.field} ${fc.operator} ${fc.value}`).join(' && ')
                            : String(e.filterExpression ?? '')}
                        </span>
                      )}
                    </div>
                  ))
                }
              </div>
              {/* Actions */}
              <div className="p-2">
                <p className="text-[11px] font-semibold uppercase tracking-wide text-green-600 mb-1.5 px-1">Actions ({actions.length})</p>
                {actions.length === 0
                  ? <p className="text-xs text-gray-400 px-1">No actions</p>
                  : actions.map((a, ai) => {
                    const aType = String(a.type ?? a.actionType ?? 'EMIT');
                    const target = String(a.target ?? '');
                    return (
                      <div key={ai} className="text-xs px-1 py-0.5">
                        <span className="font-medium text-gray-700">{CEP_ACTION_LABELS[aType] ?? aType}</span>
                        {target && <span className="ml-2 text-gray-400">→ {target}</span>}
                      </div>
                    );
                  })
                }
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ─── FLOW read-only view ──────────────────────────────────────────────────────

const FLOW_NODE_COLORS: Record<string, string> = {
  CONDITION: 'border-blue-300 bg-blue-50 text-blue-800',
  ACTION: 'border-green-300 bg-green-50 text-green-800',
  RULE_REFERENCE: 'border-purple-300 bg-purple-50 text-purple-800',
  PARALLEL_SPLIT: 'border-orange-300 bg-orange-50 text-orange-800',
  PARALLEL_JOIN: 'border-orange-300 bg-orange-50 text-orange-800',
};

function FlowReadOnlyView({ definition }: { definition: Record<string, unknown> }) {
  const nodes = (definition.nodes as Array<Record<string, unknown>>) ?? [];
  const edges = (definition.edges as Array<Record<string, unknown>>) ?? [];
  const startNodeId = definition.startNodeId as string | undefined;
  const maxIterations = definition.maxIterations as number | undefined;

  if (nodes.length === 0) {
    return <div className="flex items-center justify-center h-full text-gray-400 text-sm">No nodes defined</div>;
  }

  // Build adjacency for displaying outgoing edges per node
  const edgeMap = new Map<string, Array<Record<string, unknown>>>();
  for (const e of edges) {
    const from = String(e.from ?? '');
    if (!edgeMap.has(from)) edgeMap.set(from, []);
    edgeMap.get(from)!.push(e);
  }

  return (
    <div className="h-full overflow-auto p-3 space-y-2">
      {/* Summary */}
      <div className="flex gap-4 px-3 py-2 rounded-md bg-gray-50 border border-gray-200 text-xs text-gray-600">
        <span>{nodes.length} nodes · {edges.length} edges</span>
        {maxIterations !== undefined && <span>Max iterations: <span className="font-medium">{maxIterations}</span></span>}
        {startNodeId && <span>Start: <span className="font-mono text-indigo-700">{startNodeId}</span></span>}
      </div>

      {/* Node list — topological hint: start node first */}
      {[...nodes].sort((a) => (a.id === startNodeId ? -1 : 0)).map((n, ni) => {
        const nId = String(n.id ?? '');
        const nType = String(n.type ?? 'CONDITION');
        const label = String(n.label ?? nId);
        const expression = n.expression as string | undefined;
        const outgoing = edgeMap.get(nId) ?? [];
        const colorClass = FLOW_NODE_COLORS[nType] ?? 'border-gray-300 bg-gray-50 text-gray-700';
        const isStart = nId === startNodeId;

        return (
          <div key={ni} className={`rounded-md border px-3 py-2 text-xs ${colorClass}`}>
            <div className="flex items-center gap-2">
              {isStart && <span className="shrink-0 rounded px-1 bg-indigo-500 text-white text-[10px] font-bold">START</span>}
              <span className="font-semibold">{label}</span>
              <span className="opacity-60 text-[11px]">[{nType}]</span>
              <span className="font-mono opacity-50 text-[10px]">#{nId}</span>
            </div>
            {expression && (
              <div className="mt-1 font-mono text-[11px] opacity-70 truncate">if: {expression}</div>
            )}
            {outgoing.length > 0 && (
              <div className="mt-1 flex flex-wrap gap-1">
                {outgoing.map((e, ei) => (
                  <span key={ei} className="inline-flex items-center gap-0.5 text-[11px] opacity-70">
                    → <span className="font-mono">{String(e.to ?? '')}</span>
                    {e.condition != null && String(e.condition) !== 'true' && <span className="italic">({String(e.condition)})</span>}
                  </span>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

// ─── Dispatcher ──────────────────────────────────────────────────────────────

export function RuleDefinitionViewer({ ruleType, definition, height = '100%' }: Props) {
  if (ruleType === 'TABLE') {
    return <TableReadOnlyView definition={definition} />;
  }
  if (ruleType === 'TREE') {
    return <TreeReadOnlyView definition={definition} height={height} />;
  }
  if (ruleType === 'ENSEMBLE') {
    return <EnsembleReadOnlyView definition={definition} />;
  }
  if (ruleType === 'SCORECARD') {
    return <ScorecardReadOnlyView definition={definition} />;
  }
  if (ruleType === 'DMN') {
    return <DmnReadOnlyView definition={definition} />;
  }
  if (ruleType === 'CEP') {
    return <CepReadOnlyView definition={definition} />;
  }
  if (ruleType === 'FLOW') {
    return <FlowReadOnlyView definition={definition} />;
  }
  return (
    <JsonModelEditor
      readOnly
      value={JSON.stringify(definition, null, 2)}
      height={height}
    />
  );
}
