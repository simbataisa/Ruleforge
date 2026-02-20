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
    const def = definition as {
      nodes?: Node[];
      edges?: Edge[];
    };
    set({
      nodes: (def.nodes ?? []).map((n) => ({
        ...n,
        type: n.type || 'conditionNode',
      })),
      edges: (def.edges ?? []).map((e) => ({
        ...e,
        type: e.type || 'labeledEdge',
      })),
      selectedNodeId: null,
    });
  },

  toDefinition: () => {
    const { nodes, edges } = get();
    return {
      nodes: nodes.map((n) => ({
        id: n.id,
        type: n.type,
        position: n.position,
        data: n.data,
      })),
      edges: edges.map((e) => ({
        id: e.id,
        source: e.source,
        target: e.target,
        sourceHandle: e.sourceHandle,
        targetHandle: e.targetHandle,
        type: e.type,
        label: e.label,
      })),
    };
  },

  reset: () => {
    set(initialState);
  },
}));
