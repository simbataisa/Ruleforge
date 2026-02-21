import { describe, it, expect, beforeEach } from 'vitest';
import { useTableEditorStore } from './table-editor.store';

function getState() {
  return useTableEditorStore.getState();
}

describe('useTableEditorStore', () => {
  beforeEach(() => {
    getState().reset();
  });

  describe('initial state', () => {
    it('starts with empty columns, rows, FIRST hit policy', () => {
      const state = getState();
      expect(state.columns).toEqual([]);
      expect(state.rows).toEqual([]);
      expect(state.hitPolicy).toBe('FIRST');
      expect(state.selectedCell).toBeNull();
    });
  });

  describe('addColumn', () => {
    it('adds a condition column', () => {
      getState().addColumn('condition');
      const { columns } = getState();
      expect(columns).toHaveLength(1);
      expect(columns[0].role).toBe('condition');
      expect(columns[0].name).toBe('Condition 1');
      expect(columns[0].type).toBe('STRING');
      expect(columns[0].operator).toBe('EQ');
    });

    it('adds an action column', () => {
      getState().addColumn('action');
      const { columns } = getState();
      expect(columns).toHaveLength(1);
      expect(columns[0].role).toBe('action');
      expect(columns[0].name).toBe('Action 1');
    });

    it('increments column names per role', () => {
      getState().addColumn('condition');
      getState().addColumn('condition');
      getState().addColumn('action');
      const { columns } = getState();
      expect(columns[0].name).toBe('Condition 1');
      expect(columns[1].name).toBe('Condition 2');
      expect(columns[2].name).toBe('Action 1');
    });

    it('adds empty cell entry to existing rows', () => {
      getState().addColumn('condition');
      getState().addRow();
      getState().addColumn('action');
      const { rows, columns } = getState();
      const actionColId = columns[1].id;
      expect(rows[0].cells[actionColId]).toBe('');
    });
  });

  describe('removeColumn', () => {
    it('removes a column and its cells from all rows', () => {
      getState().addColumn('condition');
      getState().addColumn('action');
      getState().addRow();
      const colId = getState().columns[0].id;
      getState().removeColumn(0);
      const { columns, rows } = getState();
      expect(columns).toHaveLength(1);
      expect(columns[0].role).toBe('action');
      expect(rows[0].cells[colId]).toBeUndefined();
    });

    it('clears selection on remove', () => {
      getState().addColumn('condition');
      getState().addRow();
      getState().selectCell(0, 0);
      getState().removeColumn(0);
      expect(getState().selectedCell).toBeNull();
    });
  });

  describe('updateColumn', () => {
    it('updates column properties', () => {
      getState().addColumn('condition');
      getState().updateColumn(0, { name: 'Age', type: 'NUMBER', operator: 'GT' });
      const col = getState().columns[0];
      expect(col.name).toBe('Age');
      expect(col.type).toBe('NUMBER');
      expect(col.operator).toBe('GT');
    });
  });

  describe('rows', () => {
    it('addRow creates a row with empty cells for all columns', () => {
      getState().addColumn('condition');
      getState().addColumn('action');
      getState().addRow();
      const { rows, columns } = getState();
      expect(rows).toHaveLength(1);
      expect(rows[0].priority).toBe(1);
      expect(Object.keys(rows[0].cells)).toHaveLength(2);
      columns.forEach((col) => {
        expect(rows[0].cells[col.id]).toBe('');
      });
    });

    it('removeRow removes the row at index', () => {
      getState().addColumn('condition');
      getState().addRow();
      getState().addRow();
      getState().removeRow(0);
      expect(getState().rows).toHaveLength(1);
    });

    it('updateCell updates a specific cell value', () => {
      getState().addColumn('condition');
      getState().addRow();
      getState().updateCell(0, 0, 'hello');
      const colId = getState().columns[0].id;
      expect(getState().rows[0].cells[colId]).toBe('hello');
    });

    it('updateRowPriority changes row priority', () => {
      getState().addColumn('condition');
      getState().addRow();
      getState().updateRowPriority(0, 5);
      expect(getState().rows[0].priority).toBe(5);
    });
  });

  describe('selection', () => {
    it('selectCell and clearSelection work', () => {
      getState().selectCell(1, 2);
      expect(getState().selectedCell).toEqual({ row: 1, col: 2 });
      getState().clearSelection();
      expect(getState().selectedCell).toBeNull();
    });
  });

  describe('hitPolicy', () => {
    it('setHitPolicy changes the policy', () => {
      getState().setHitPolicy('ALL');
      expect(getState().hitPolicy).toBe('ALL');
    });
  });

  describe('loadFromDefinition / toDefinition', () => {
    it('round-trips a definition', () => {
      getState().addColumn('condition');
      getState().addColumn('action');
      getState().addRow();
      getState().updateCell(0, 0, 'x');
      getState().updateCell(0, 1, 'y');
      getState().setHitPolicy('PRIORITY');

      const def = getState().toDefinition();

      getState().reset();
      expect(getState().columns).toHaveLength(0);

      getState().loadFromDefinition(def);

      expect(getState().hitPolicy).toBe('PRIORITY');
      expect(getState().columns).toHaveLength(2);
      expect(getState().rows).toHaveLength(1);
      expect(getState().selectedCell).toBeNull();
    });

    it('handles empty definition gracefully', () => {
      getState().loadFromDefinition({});
      expect(getState().columns).toEqual([]);
      expect(getState().rows).toEqual([]);
      expect(getState().hitPolicy).toBe('FIRST');
    });
  });

  describe('reset', () => {
    it('resets to initial state', () => {
      getState().addColumn('condition');
      getState().addRow();
      getState().setHitPolicy('ALL');
      getState().selectCell(0, 0);
      getState().reset();

      const state = getState();
      expect(state.columns).toEqual([]);
      expect(state.rows).toEqual([]);
      expect(state.hitPolicy).toBe('FIRST');
      expect(state.selectedCell).toBeNull();
    });
  });
});
