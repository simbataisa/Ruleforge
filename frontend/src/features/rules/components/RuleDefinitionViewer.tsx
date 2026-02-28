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

// ─── Dispatcher ──────────────────────────────────────────────────────────────

export function RuleDefinitionViewer({ ruleType, definition, height = '100%' }: Props) {
  if (ruleType === 'TABLE') {
    return <TableReadOnlyView definition={definition} />;
  }
  if (ruleType === 'TREE') {
    return <TreeReadOnlyView definition={definition} height={height} />;
  }
  return (
    <JsonModelEditor
      readOnly
      value={JSON.stringify(definition, null, 2)}
      height={height}
    />
  );
}
