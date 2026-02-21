import { describe, it, expect, beforeEach } from 'vitest';
import { useTreeEditorStore } from './tree-editor.store';

function getState() {
  return useTreeEditorStore.getState();
}

describe('useTreeEditorStore', () => {
  beforeEach(() => {
    getState().reset();
  });

  describe('initial state', () => {
    it('starts with empty nodes, edges, no selection', () => {
      const state = getState();
      expect(state.nodes).toEqual([]);
      expect(state.edges).toEqual([]);
      expect(state.selectedNodeId).toBeNull();
    });
  });

  describe('addConditionNode', () => {
    it('adds a condition node at given position', () => {
      getState().addConditionNode({ x: 100, y: 200 });
      const { nodes } = getState();
      expect(nodes).toHaveLength(1);
      expect(nodes[0].type).toBe('conditionNode');
      expect(nodes[0].position).toEqual({ x: 100, y: 200 });
      expect(nodes[0].data.label).toBe('New Condition');
      expect(nodes[0].data.field).toBe('');
      expect(nodes[0].data.operator).toBe('EQ');
    });
  });

  describe('addActionNode', () => {
    it('adds an action node at given position', () => {
      getState().addActionNode({ x: 50, y: 300 });
      const { nodes } = getState();
      expect(nodes).toHaveLength(1);
      expect(nodes[0].type).toBe('actionNode');
      expect(nodes[0].position).toEqual({ x: 50, y: 300 });
      expect(nodes[0].data.label).toBe('New Action');
      expect(nodes[0].data.outputs).toEqual({});
    });
  });

  describe('updateNode', () => {
    it('updates node data by id', () => {
      getState().addConditionNode({ x: 0, y: 0 });
      const nodeId = getState().nodes[0].id;
      getState().updateNode(nodeId, { label: 'Age Check', field: 'age' });
      const node = getState().nodes[0];
      expect(node.data.label).toBe('Age Check');
      expect(node.data.field).toBe('age');
    });

    it('does not affect other nodes', () => {
      getState().addConditionNode({ x: 0, y: 0 });
      getState().addActionNode({ x: 100, y: 100 });
      const conditionId = getState().nodes[0].id;
      getState().updateNode(conditionId, { label: 'Updated' });
      expect(getState().nodes[1].data.label).toBe('New Action');
    });
  });

  describe('deleteNode', () => {
    it('removes the node and its connected edges', () => {
      getState().addConditionNode({ x: 0, y: 0 });
      getState().addActionNode({ x: 100, y: 100 });
      const conditionId = getState().nodes[0].id;
      const actionId = getState().nodes[1].id;

      getState().onConnect({
        source: conditionId,
        target: actionId,
        sourceHandle: null,
        targetHandle: null,
      });
      expect(getState().edges).toHaveLength(1);

      getState().deleteNode(conditionId);
      expect(getState().nodes).toHaveLength(1);
      expect(getState().nodes[0].id).toBe(actionId);
      expect(getState().edges).toHaveLength(0);
    });

    it('clears selection if deleted node was selected', () => {
      getState().addConditionNode({ x: 0, y: 0 });
      const nodeId = getState().nodes[0].id;
      getState().setSelectedNodeId(nodeId);
      getState().deleteNode(nodeId);
      expect(getState().selectedNodeId).toBeNull();
    });
  });

  describe('onConnect', () => {
    it('creates an edge between two nodes', () => {
      getState().addConditionNode({ x: 0, y: 0 });
      getState().addActionNode({ x: 100, y: 100 });
      const src = getState().nodes[0].id;
      const tgt = getState().nodes[1].id;

      getState().onConnect({
        source: src,
        target: tgt,
        sourceHandle: null,
        targetHandle: null,
      });

      const { edges } = getState();
      expect(edges).toHaveLength(1);
      expect(edges[0].source).toBe(src);
      expect(edges[0].target).toBe(tgt);
      expect(edges[0].type).toBe('labeledEdge');
    });
  });

  describe('updateEdgeLabel', () => {
    it('updates an edge label', () => {
      getState().addConditionNode({ x: 0, y: 0 });
      getState().addActionNode({ x: 100, y: 100 });
      const src = getState().nodes[0].id;
      const tgt = getState().nodes[1].id;
      getState().onConnect({ source: src, target: tgt, sourceHandle: null, targetHandle: null });

      const edgeId = getState().edges[0].id;
      getState().updateEdgeLabel(edgeId, 'Yes');
      expect(getState().edges[0].label).toBe('Yes');
    });
  });

  describe('selection', () => {
    it('setSelectedNodeId selects and deselects', () => {
      getState().setSelectedNodeId('abc');
      expect(getState().selectedNodeId).toBe('abc');
      getState().setSelectedNodeId(null);
      expect(getState().selectedNodeId).toBeNull();
    });
  });

  describe('loadFromDefinition / toDefinition', () => {
    it('round-trips a definition', () => {
      getState().addConditionNode({ x: 10, y: 20 });
      getState().addActionNode({ x: 30, y: 40 });
      const src = getState().nodes[0].id;
      const tgt = getState().nodes[1].id;
      getState().onConnect({ source: src, target: tgt, sourceHandle: null, targetHandle: null });

      const def = getState().toDefinition();

      getState().reset();
      expect(getState().nodes).toHaveLength(0);

      getState().loadFromDefinition(def);
      expect(getState().nodes).toHaveLength(2);
      expect(getState().edges).toHaveLength(1);
      expect(getState().selectedNodeId).toBeNull();
    });

    it('handles empty definition', () => {
      getState().loadFromDefinition({});
      expect(getState().nodes).toEqual([]);
      expect(getState().edges).toEqual([]);
    });
  });

  describe('reset', () => {
    it('clears all state', () => {
      getState().addConditionNode({ x: 0, y: 0 });
      getState().setSelectedNodeId(getState().nodes[0].id);
      getState().reset();
      expect(getState().nodes).toEqual([]);
      expect(getState().edges).toEqual([]);
      expect(getState().selectedNodeId).toBeNull();
    });
  });
});
