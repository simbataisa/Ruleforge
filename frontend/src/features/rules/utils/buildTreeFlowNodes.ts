import type { Node, Edge } from '@xyflow/react';

/**
 * Pure helper that converts a rule tree definition (engine or UI format)
 * into React Flow nodes and edges for read-only display.
 */
export function buildTreeFlowNodes(definition: Record<string, unknown>): {
  nodes: Node[];
  edges: Edge[];
} {
  let def = definition as Record<string, unknown>;
  if (typeof def === 'string') {
    try { def = JSON.parse(def); } catch { return { nodes: [], edges: [] }; }
  }

  // UI format — React Flow nodes/edges already
  if (def.nodes && Array.isArray(def.nodes)) {
    const uiDef = def as { nodes: Node[]; edges?: Edge[] };
    return {
      nodes: uiDef.nodes.map((n) => ({ ...n, type: n.type || 'conditionNode' })),
      edges: (uiDef.edges ?? []).map((e) => ({ ...e, type: e.type || 'labeledEdge' })),
    };
  }

  // Engine format — recursive tree with root node
  if (def.root) {
    const nodes: Node[] = [];
    const edges: Edge[] = [];
    let counter = 0;

    function genId(prefix: string): string {
      return `${prefix}_${++counter}_view`;
    }

    function flattenNode(node: Record<string, unknown>, x: number, y: number): string {
      const nodeId = (node.nodeId as string) || genId('n');
      const nodeType = node.nodeType as string;

      if (nodeType === 'ACTION' || nodeType === 'SCORE') {
        nodes.push({
          id: nodeId,
          type: 'default',
          position: { x, y },
          data: {
            label: (node.label as string) || 'Action',
          },
          style: { background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 6 },
        });
      } else if (nodeType === 'GATE') {
        nodes.push({
          id: nodeId,
          type: 'default',
          position: { x, y },
          data: {
            label: (node.label as string) || (node.expression as string) || 'Gate',
          },
          style: { background: '#fff7ed', border: '1px solid #fdba74', borderRadius: 6 },
        });

        const trueChild = node.trueChild as Record<string, unknown> | undefined;
        const falseChild = node.falseChild as Record<string, unknown> | undefined;
        const children = [trueChild, falseChild].filter(Boolean) as Record<string, unknown>[];
        children.forEach((child, ci) => {
          const childX = x + (ci - (children.length - 1) / 2) * 250;
          const childY = y + 150;
          const childId = flattenNode(child, childX, childY);
          edges.push({
            id: `edge_${nodeId}_${childId}`,
            source: nodeId,
            target: childId,
            label: ci === 0 && trueChild ? 'true' : 'false',
          } as Edge);
        });
      } else {
        // CONDITION
        const branches = (node.branches as Array<Record<string, unknown>>) ?? [];
        nodes.push({
          id: nodeId,
          type: 'default',
          position: { x, y },
          data: {
            label: (node.label as string) || (node.field as string) || 'Condition',
          },
          style: { background: '#eff6ff', border: '1px solid #93c5fd', borderRadius: 6 },
        });

        branches.forEach((branch, bi) => {
          const totalChildren = branches.length + (node.defaultBranch ? 1 : 0);
          const childX = x + (bi - (totalChildren - 1) / 2) * 250;
          const childY = y + 150;
          const child = branch.child as Record<string, unknown>;
          if (child) {
            const childId = flattenNode(child, childX, childY);
            edges.push({
              id: `edge_${nodeId}_${childId}`,
              source: nodeId,
              target: childId,
            } as Edge);
          }
        });

        if (node.defaultBranch) {
          const defIdx = branches.length;
          const totalChildren = branches.length + 1;
          const defX = x + (defIdx - (totalChildren - 1) / 2) * 250;
          const defY = y + 150;
          const defId = flattenNode(node.defaultBranch as Record<string, unknown>, defX, defY);
          edges.push({
            id: `edge_${nodeId}_${defId}`,
            source: nodeId,
            target: defId,
            label: 'default',
          } as Edge);
        }
      }

      return nodeId;
    }

    flattenNode(def.root as Record<string, unknown>, 400, 50);
    return { nodes, edges };
  }

  return { nodes: [], edges: [] };
}
