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
  [key: string]: unknown;
}

export interface ActionNodeData {
  label: string;
  outputs: Record<string, string>;
  [key: string]: unknown;
}

interface TreeEditorState {
  nodes: Node[];
  edges: Edge[];
  selectedNodeId: string | null;

  addConditionNode: (position: { x: number; y: number }) => void;
  addActionNode: (position: { x: number; y: number }) => void;
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
      } satisfies ActionNodeData,
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
    const edge: Edge = {
      ...connection,
      id: `edge_${connection.source}_${connection.target}`,
      type: 'labeledEdge',
      label: '',
    } as Edge;
    set((state) => ({
      edges: addEdge(edge, state.edges),
    }));
  },

  setSelectedNodeId: (id) => {
    set({ selectedNodeId: id });
  },

  loadFromDefinition: (definition) => {
    // Support both engine format (root tree) and UI format (nodes/edges)
    const def = definition as Record<string, unknown>;

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
              label: 'Action',
              outputs: (node.outputs as Record<string, string>) ?? {},
            },
          });
        } else {
          // CONDITION or other
          nodes.push({
            id: nodeId,
            type: 'conditionNode',
            position: { x, y },
            data: {
              label: (node.field as string) || 'Condition',
              field: (node.field as string) || '',
              operator: (node.operator as string) || 'EQ',
              value: '',
            },
          });

          const branches = (node.branches as Array<Record<string, unknown>>) ?? [];
          branches.forEach((branch, bi) => {
            const childX = x + (bi - (branches.length - 1) / 2) * 250;
            const childY = y + 150;
            const child = branch.child as Record<string, unknown>;
            if (child) {
              const childId = flattenNode(child, childX, childY);
              edges.push({
                id: `edge_${nodeId}_${childId}`,
                source: nodeId,
                target: childId,
                type: 'labeledEdge',
                label: String(branch.value ?? ''),
              } as Edge);
            }
          });

          if (node.defaultBranch) {
            const defX = x + branches.length * 250;
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
        return {
          nodeType: 'ACTION',
          nodeId: node.id,
          outputs: data.outputs ?? {},
        };
      }

      // conditionNode
      const data = node.data as ConditionNodeData;
      const outEdges = edges.filter((e) => e.source === node.id);
      const branches = outEdges.map((edge) => {
        const targetNode = nodes.find((n) => n.id === edge.target);
        return {
          value: edge.label ? String(edge.label) : '',
          child: targetNode ? buildTreeNode(targetNode) : { nodeType: 'ACTION', nodeId: edge.target, outputs: {} },
        };
      });

      return {
        nodeType: 'CONDITION',
        nodeId: node.id,
        field: data.field || '',
        operator: data.operator || 'EQ',
        branches,
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
