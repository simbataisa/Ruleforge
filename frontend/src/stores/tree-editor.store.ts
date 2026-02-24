import { create } from 'zustand';
import type {
  Node,
  Edge,
  NodeChange,
  EdgeChange,
  Connection,
} from '@xyflow/react';
import { applyNodeChanges, applyEdgeChanges, addEdge } from '@xyflow/react';

export interface ConditionNodeData {
  label: string;
  field: string;
  operator: string;
  value: string;
  secondValue?: string;
  [key: string]: unknown;
}

export type CallableActionProtocol = 'http' | 'rule' | 'script' | 'log';

export interface CallableAction {
  id: string;
  protocol: CallableActionProtocol;
  target: string;
  parameters: Record<string, unknown>;
  outputMapping?: string;
}

export interface ActionNodeData {
  label: string;
  outputs: Record<string, string>;
  callableActions?: CallableAction[];
  [key: string]: unknown;
}

export interface GateNodeData {
  label: string;
  expression: string;
  [key: string]: unknown;
}

interface TreeEditorState {
  nodes: Node[];
  edges: Edge[];
  selectedNodeId: string | null;

  addConditionNode: (position: { x: number; y: number }) => void;
  addActionNode: (position: { x: number; y: number }) => void;
  addGateNode: (position: { x: number; y: number }) => void;
  updateNode: (id: string, data: Record<string, unknown>) => void;
  deleteNode: (id: string) => void;
  updateEdgeLabel: (id: string, label: string) => void;
  onNodesChange: (changes: NodeChange[]) => void;
  onEdgesChange: (changes: EdgeChange[]) => void;
  onConnect: (connection: Connection) => void;
  setSelectedNodeId: (id: string | null) => void;
  loadFromDefinition: (definition: Record<string, unknown>) => void;
  toDefinition: () => Record<string, unknown>;
  reset: () => void;
}

let nodeCounter = 0;

function generateNodeId(prefix: string): string {
  return `${prefix}_${++nodeCounter}_${Date.now()}`;
}

const initialState = {
  nodes: [] as Node[],
  edges: [] as Edge[],
  selectedNodeId: null as string | null,
};

export const useTreeEditorStore = create<TreeEditorState>((set, get) => ({
  ...initialState,

  addConditionNode: (position) => {
    const id = generateNodeId('condition');
    const newNode: Node = {
      id,
      type: 'conditionNode',
      position,
      data: {
        label: 'New Condition',
        field: '',
        operator: 'EQ',
        value: '',
        secondValue: '',
      } satisfies ConditionNodeData,
    };
    set((state) => ({ nodes: [...state.nodes, newNode] }));
  },

  addActionNode: (position) => {
    const id = generateNodeId('action');
    const newNode: Node = {
      id,
      type: 'actionNode',
      position,
      data: {
        label: 'New Action',
        outputs: {},
        callableActions: [],
      } satisfies ActionNodeData,
    };
    set((state) => ({ nodes: [...state.nodes, newNode] }));
  },

  addGateNode: (position) => {
    const id = generateNodeId('gate');
    const newNode: Node = {
      id,
      type: 'gateNode',
      position,
      data: {
        label: 'New Gate',
        expression: '',
      } satisfies GateNodeData,
    };
    set((state) => ({ nodes: [...state.nodes, newNode] }));
  },

  updateNode: (id, data) => {
    set((state) => ({
      nodes: state.nodes.map((node) =>
        node.id === id ? { ...node, data: { ...node.data, ...data } } : node,
      ),
    }));
  },

  deleteNode: (id) => {
    set((state) => ({
      nodes: state.nodes.filter((n) => n.id !== id),
      edges: state.edges.filter((e) => e.source !== id && e.target !== id),
      selectedNodeId:
        state.selectedNodeId === id ? null : state.selectedNodeId,
    }));
  },

  updateEdgeLabel: (id, label) => {
    set((state) => ({
      edges: state.edges.map((e) =>
        e.id === id ? { ...e, label } : e,
      ),
    }));
  },

  onNodesChange: (changes) => {
    set((state) => ({
      nodes: applyNodeChanges(changes, state.nodes),
    }));
  },

  onEdgesChange: (changes) => {
    set((state) => ({
      edges: applyEdgeChanges(changes, state.edges),
    }));
  },

  onConnect: (connection) => {
    const { nodes, edges: currentEdges } = get();
    const sourceNode = nodes.find((n) => n.id === connection.source);
    const existingOutEdges = currentEdges.filter((e) => e.source === connection.source);

    // Auto-label edges based on node type
    let label = '';
    if (sourceNode?.type === 'gateNode') {
      label = existingOutEdges.length === 0 ? 'true' : 'false';
    } else if (sourceNode?.type === 'conditionNode' && existingOutEdges.length >= 1) {
      // When adding a second+ edge from a condition, label it "default"
      label = 'default';
    }

    const edge: Edge = {
      ...connection,
      id: `edge_${connection.source}_${connection.target}`,
      type: 'labeledEdge',
      label,
    } as Edge;
    set((state) => ({
      edges: addEdge(edge, state.edges),
    }));
  },

  setSelectedNodeId: (id) => {
    set({ selectedNodeId: id });
  },

  loadFromDefinition: (definition) => {
    // Backend may return definition as JSON string — parse it
    let def = definition as Record<string, unknown>;
    if (typeof def === 'string') {
      try { def = JSON.parse(def); } catch { /* keep as-is */ }
    }

    if (def.nodes && Array.isArray(def.nodes)) {
      // UI format - React Flow nodes/edges
      const uiDef = def as { nodes: Node[]; edges?: Edge[] };
      set({
        nodes: uiDef.nodes.map((n) => ({
          ...n,
          type: n.type || 'conditionNode',
        })),
        edges: (uiDef.edges ?? []).map((e) => ({
          ...e,
          type: e.type || 'labeledEdge',
        })),
        selectedNodeId: null,
      });
      return;
    }

    // Engine format - recursive tree with root node
    if (def.root) {
      const nodes: Node[] = [];
      const edges: Edge[] = [];
      let yOffset = 0;

      function flattenNode(
        node: Record<string, unknown>,
        x: number,
        y: number,
      ): string {
        const nodeId = (node.nodeId as string) || generateNodeId('n');
        const nodeType = node.nodeType as string;

        if (nodeType === 'ACTION' || nodeType === 'SCORE') {
          nodes.push({
            id: nodeId,
            type: 'actionNode',
            position: { x, y },
            data: {
              label: (node.label as string) || 'Action',
              outputs: (node.outputs as Record<string, string>) ?? {},
              callableActions: Array.isArray(node.callableActions)
                ? (node.callableActions as Array<Record<string, unknown>>).map((a, i) => ({
                    id: (a.actionId as string) || (a.id as string) || `action_${i}`,
                    protocol: (a.protocol as CallableActionProtocol) || 'log',
                    target: (a.target as string) || '',
                    parameters: (a.parameters as Record<string, unknown>) ?? {},
                    outputMapping: (a.outputMapping as string) || undefined,
                  }))
                : [],
            },
          });
        } else if (nodeType === 'GATE') {
          nodes.push({
            id: nodeId,
            type: 'gateNode',
            position: { x, y },
            data: {
              label: (node.label as string) || ((node.expression as string) ? 'Gate' : 'New Gate'),
              expression: (node.expression as string) || '',
            },
          });

          // GATE has trueChild and falseChild
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
              type: 'labeledEdge',
              label: ci === 0 && trueChild ? 'true' : 'false',
            } as Edge);
          });
        } else {
          // CONDITION or other
          const branches = (node.branches as Array<Record<string, unknown>>) ?? [];
          // Always restore the first branch value to the condition node
          const nodeValue = branches.length > 0 ? String(branches[0]?.value ?? '') : '';
          const nodeSecondValue = branches.length > 0 ? String(branches[0]?.secondValue ?? '') : '';

          nodes.push({
            id: nodeId,
            type: 'conditionNode',
            position: { x, y },
            data: {
              label: (node.label as string) || (node.field as string) || 'Condition',
              field: (node.field as string) || '',
              operator: (node.operator as string) || 'EQ',
              value: nodeValue,
              secondValue: nodeSecondValue,
            },
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
                type: 'labeledEdge',
                label: '',
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
              type: 'labeledEdge',
              label: 'default',
            } as Edge);
          }
        }

        yOffset = Math.max(yOffset, y);
        return nodeId;
      }

      flattenNode(def.root as Record<string, unknown>, 400, 50);

      set({ nodes, edges, selectedNodeId: null });
      return;
    }

    // Empty definition
    set({ nodes: [], edges: [], selectedNodeId: null });
  },

  toDefinition: () => {
    const { nodes, edges } = get();

    if (nodes.length === 0) {
      return { root: null, scoringMode: false, earlyTermination: true };
    }

    // Find root node (no incoming edges)
    const targetIds = new Set(edges.map((e) => e.target));
    const rootNode = nodes.find((n) => !targetIds.has(n.id)) ?? nodes[0];

    function buildTreeNode(node: Node): Record<string, unknown> {
      if (node.type === 'actionNode') {
        const data = node.data as ActionNodeData;
        const result: Record<string, unknown> = {
          nodeType: 'ACTION',
          nodeId: node.id,
          label: data.label || '',
          outputs: data.outputs ?? {},
        };
        if (data.callableActions && data.callableActions.length > 0) {
          result.callableActions = data.callableActions.map((a) => ({
            actionId: a.id,
            protocol: a.protocol,
            target: a.target,
            parameters: a.parameters,
            ...(a.outputMapping ? { outputMapping: a.outputMapping } : {}),
          }));
        }
        return result;
      }

      if (node.type === 'gateNode') {
        const data = node.data as GateNodeData;
        const outEdges = edges.filter((e) => e.source === node.id);
        // First edge = trueChild, second edge = falseChild
        const trueEdge = outEdges.find((e) => e.label === 'true') ?? outEdges[0];
        const falseEdge = outEdges.find((e) => e.label === 'false') ?? outEdges[1];

        const trueTarget = trueEdge ? nodes.find((n) => n.id === trueEdge.target) : undefined;
        const falseTarget = falseEdge && falseEdge !== trueEdge ? nodes.find((n) => n.id === falseEdge.target) : undefined;

        return {
          nodeType: 'GATE',
          nodeId: node.id,
          label: data.label || '',
          expression: data.expression || '',
          ...(trueTarget ? { trueChild: buildTreeNode(trueTarget) } : {}),
          ...(falseTarget ? { falseChild: buildTreeNode(falseTarget) } : {}),
        };
      }

      // conditionNode — always use node's value for branch thresholds
      const data = node.data as ConditionNodeData;
      const outEdges = edges.filter((e) => e.source === node.id);

      const rawValue = data.value || '';
      const numericValue = rawValue !== '' && !isNaN(Number(rawValue)) ? Number(rawValue) : rawValue;
      const isBetween = data.operator === 'BETWEEN';
      const rawSecond = data.secondValue || '';
      const numericSecond = rawSecond !== '' && !isNaN(Number(rawSecond)) ? Number(rawSecond) : rawSecond;

      // Separate default edge (labeled "default") from match edges
      const defaultEdge = outEdges.find((e) => e.label === 'default');
      const matchEdges = outEdges.filter((e) => e.label !== 'default');

      // If no explicit default label but 2+ edges, treat last as default
      const effectiveDefaultEdge = defaultEdge ?? (outEdges.length >= 2 ? outEdges[outEdges.length - 1] : undefined);
      const effectiveMatchEdges = defaultEdge
        ? matchEdges
        : (outEdges.length >= 2 ? outEdges.slice(0, -1) : outEdges);

      const branches = effectiveMatchEdges.map((edge) => {
        const targetNode = nodes.find((n) => n.id === edge.target);
        return {
          value: numericValue,
          ...(isBetween && rawSecond ? { secondValue: numericSecond } : {}),
          child: targetNode ? buildTreeNode(targetNode) : { nodeType: 'ACTION', nodeId: edge.target, outputs: {} },
        };
      });

      let defaultBranch: Record<string, unknown> | undefined;
      if (effectiveDefaultEdge) {
        const defTarget = nodes.find((n) => n.id === effectiveDefaultEdge.target);
        defaultBranch = defTarget ? buildTreeNode(defTarget) : { nodeType: 'ACTION', nodeId: effectiveDefaultEdge.target, outputs: {} };
      }

      return {
        nodeType: 'CONDITION',
        nodeId: node.id,
        label: data.label || '',
        field: data.field || '',
        operator: data.operator || 'EQ',
        branches,
        ...(defaultBranch ? { defaultBranch } : {}),
      };
    }

    return {
      root: buildTreeNode(rootNode),
      scoringMode: false,
      earlyTermination: true,
    };
  },

  reset: () => {
    set(initialState);
  },
}));
