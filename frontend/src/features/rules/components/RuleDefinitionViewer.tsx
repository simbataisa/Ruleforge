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
  return (
    <JsonModelEditor
      readOnly
      value={JSON.stringify(definition, null, 2)}
      height={height}
    />
  );
}
