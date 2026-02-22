import { create } from 'zustand';
import { FlowDefinition, FlowNodeData, FlowEdgeData, FlowNodeType } from '../features/flow-editor/types';

interface FlowEditorState {
  definition: FlowDefinition;
  selectedNodeId: string | null;
  isDirty: boolean;
  addNode: (type: FlowNodeType) => void;
  updateNode: (id: string, data: Partial<FlowNodeData>) => void;
  deleteNode: (id: string) => void;
  addEdge: (edge: FlowEdgeData) => void;
  deleteEdge: (from: string, to: string) => void;
  setStartNode: (id: string) => void;
  selectNode: (id: string | null) => void;
  setDefinition: (def: FlowDefinition) => void;
  reset: () => void;
}

const DEFAULT_DEFINITION: FlowDefinition = {
  nodes: [
    { id: 'start', type: 'CONDITION', label: 'Start Condition', expression: 'true' },
    { id: 'action1', type: 'ACTION', label: 'Default Action', outputs: { result: true } },
  ],
  edges: [
    { from: 'start', to: 'action1', condition: 'true' },
  ],
  startNodeId: 'start',
  maxIterations: 100,
};

export const useFlowEditorStore = create<FlowEditorState>((set) => ({
  definition: DEFAULT_DEFINITION,
  selectedNodeId: null,
  isDirty: false,
  addNode: (type) => set((state) => {
    const id = `node_${Date.now()}`;
    const newNode: FlowNodeData = { id, type, label: `${type} Node` };
    return {
      definition: { ...state.definition, nodes: [...state.definition.nodes, newNode] },
      isDirty: true,
    };
  }),
  updateNode: (id, data) => set((state) => ({
    definition: {
      ...state.definition,
      nodes: state.definition.nodes.map(n => n.id === id ? { ...n, ...data } : n),
    },
    isDirty: true,
  })),
  deleteNode: (id) => set((state) => ({
    definition: {
      ...state.definition,
      nodes: state.definition.nodes.filter(n => n.id !== id),
      edges: state.definition.edges.filter(e => e.from !== id && e.to !== id),
    },
    isDirty: true,
  })),
  addEdge: (edge) => set((state) => ({
    definition: { ...state.definition, edges: [...state.definition.edges, edge] },
    isDirty: true,
  })),
  deleteEdge: (from, to) => set((state) => ({
    definition: {
      ...state.definition,
      edges: state.definition.edges.filter(e => !(e.from === from && e.to === to)),
    },
    isDirty: true,
  })),
  setStartNode: (id) => set((state) => ({
    definition: { ...state.definition, startNodeId: id },
    isDirty: true,
  })),
  selectNode: (id) => set({ selectedNodeId: id }),
  setDefinition: (def) => set({
    definition: {
      nodes: def.nodes ?? [],
      edges: def.edges ?? [],
      startNodeId: def.startNodeId ?? '',
      maxIterations: def.maxIterations ?? 100,
    },
    isDirty: false,
  }),
  reset: () => set({ definition: DEFAULT_DEFINITION, selectedNodeId: null, isDirty: false }),
}));
