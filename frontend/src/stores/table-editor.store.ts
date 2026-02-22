import { create } from 'zustand';

export type ColumnType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE';
export type ColumnRole = 'condition' | 'action';
export type Operator =
  | 'EQ'
  | 'NEQ'
  | 'GT'
  | 'GTE'
  | 'LT'
  | 'LTE'
  | 'BETWEEN'
  | 'IN'
  | 'CONTAINS'
  | 'MATCHES'
  | 'IS_NULL';

export type HitPolicy = 'FIRST' | 'ALL' | 'PRIORITY' | 'AGGREGATE' | 'RANKED';

export interface ColumnDef {
  id: string;
  name: string;
  role: ColumnRole;
  type: ColumnType;
  operator: Operator;
}

export interface RowData {
  id: string;
  priority: number;
  cells: Record<string, string>;
}

interface TableEditorState {
  columns: ColumnDef[];
  rows: RowData[];
  hitPolicy: HitPolicy;
  selectedCell: { row: number; col: number } | null;

  addColumn: (role: ColumnRole) => void;
  removeColumn: (index: number) => void;
  updateColumn: (index: number, updates: Partial<ColumnDef>) => void;
  addRow: () => void;
  removeRow: (index: number) => void;
  updateCell: (row: number, col: number, value: string) => void;
  selectCell: (row: number, col: number) => void;
  clearSelection: () => void;
  setHitPolicy: (policy: HitPolicy) => void;
  updateRowPriority: (index: number, priority: number) => void;
  loadFromDefinition: (definition: Record<string, unknown>) => void;
  toDefinition: () => Record<string, unknown>;
  reset: () => void;
}

let colCounter = 0;
let rowCounter = 0;

function generateColId(): string {
  return `col_${++colCounter}_${Date.now()}`;
}

function generateRowId(): string {
  return `row_${++rowCounter}_${Date.now()}`;
}

const initialState = {
  columns: [] as ColumnDef[],
  rows: [] as RowData[],
  hitPolicy: 'FIRST' as HitPolicy,
  selectedCell: null as { row: number; col: number } | null,
};

export const useTableEditorStore = create<TableEditorState>((set, get) => ({
  ...initialState,

  addColumn: (role) => {
    const id = generateColId();
    const existingOfRole = get().columns.filter((c) => c.role === role);
    const name =
      role === 'condition'
        ? `Condition ${existingOfRole.length + 1}`
        : `Action ${existingOfRole.length + 1}`;

    const newCol: ColumnDef = {
      id,
      name,
      role,
      type: 'STRING',
      operator: 'EQ',
    };

    set((state) => ({
      columns: [...state.columns, newCol],
      rows: state.rows.map((row) => ({
        ...row,
        cells: { ...row.cells, [id]: '' },
      })),
    }));
  },

  removeColumn: (index) => {
    const col = get().columns[index];
    if (!col) return;
    set((state) => ({
      columns: state.columns.filter((_, i) => i !== index),
      rows: state.rows.map((row) => {
        const cells = { ...row.cells };
        delete cells[col.id];
        return { ...row, cells };
      }),
      selectedCell: null,
    }));
  },

  updateColumn: (index, updates) => {
    set((state) => ({
      columns: state.columns.map((col, i) =>
        i === index ? { ...col, ...updates } : col,
      ),
    }));
  },

  addRow: () => {
    const id = generateRowId();
    const cells: Record<string, string> = {};
    get().columns.forEach((col) => {
      cells[col.id] = '';
    });
    set((state) => ({
      rows: [
        ...state.rows,
        { id, priority: state.rows.length + 1, cells },
      ],
    }));
  },

  removeRow: (index) => {
    set((state) => ({
      rows: state.rows.filter((_, i) => i !== index),
      selectedCell: null,
    }));
  },

  updateCell: (row, col, value) => {
    const colDef = get().columns[col];
    if (!colDef) return;
    set((state) => ({
      rows: state.rows.map((r, i) =>
        i === row
          ? { ...r, cells: { ...r.cells, [colDef.id]: value } }
          : r,
      ),
    }));
  },

  selectCell: (row, col) => {
    set({ selectedCell: { row, col } });
  },

  clearSelection: () => {
    set({ selectedCell: null });
  },

  setHitPolicy: (policy) => {
    set({ hitPolicy: policy });
  },

  updateRowPriority: (index, priority) => {
    set((state) => ({
      rows: state.rows.map((r, i) =>
        i === index ? { ...r, priority } : r,
      ),
    }));
  },

  loadFromDefinition: (definition) => {
    const def = definition as {
      hitPolicy?: HitPolicy;
      columns?: Array<{
        name?: string;
        columnType?: string;
        operator?: string;
        dataType?: string;
        index?: number;
        // Also support UI format for round-trip
        id?: string;
        role?: ColumnRole;
        type?: ColumnType;
      }>;
      rows?: Array<{
        rowNumber?: number;
        priority?: number;
        conditions?: Array<{ value?: string } | string>;
        actions?: Array<{ value?: string } | string>;
        // Also support UI format
        cells?: Record<string, string>;
      }>;
    };

    // Build columns, handling both engine format (columnType) and UI format (role)
    const columns: ColumnDef[] = (def.columns ?? []).map((c, i) => {
      const role: ColumnRole = c.role
        ?? (c.columnType ? c.columnType.toLowerCase() as ColumnRole : 'condition');
      const type: ColumnType = c.type
        ?? (c.dataType as ColumnType | undefined)
        ?? 'STRING';
      return {
        id: c.id || generateColId(),
        name: c.name || `Column ${i + 1}`,
        role,
        type: (['STRING', 'NUMBER', 'BOOLEAN', 'DATE'].includes(type) ? type : 'STRING') as ColumnType,
        operator: (c.operator || 'EQ') as Operator,
      };
    });

    const conditionCols = columns.filter((c) => c.role === 'condition');
    const actionCols = columns.filter((c) => c.role === 'action');

    // Build rows, handling both engine format (conditions/actions arrays) and UI format (cells map)
    const rows: RowData[] = (def.rows ?? []).map((r, i) => {
      if (r.cells) {
        // UI format - cells map keyed by column id
        return {
          id: generateRowId(),
          priority: r.priority ?? i + 1,
          cells: r.cells,
        };
      }
      // Engine format - positional conditions/actions arrays
      const cells: Record<string, string> = {};
      (r.conditions ?? []).forEach((cell, ci) => {
        if (ci < conditionCols.length) {
          const val = typeof cell === 'string' ? cell : (cell?.value ?? '');
          cells[conditionCols[ci].id] = val;
        }
      });
      (r.actions ?? []).forEach((cell, ai) => {
        if (ai < actionCols.length) {
          const val = typeof cell === 'string' ? cell : (cell?.value ?? '');
          cells[actionCols[ai].id] = val;
        }
      });
      return {
        id: generateRowId(),
        priority: r.priority ?? i + 1,
        cells,
      };
    });

    set({
      hitPolicy: def.hitPolicy ?? 'FIRST',
      columns,
      rows,
      selectedCell: null,
    });
  },

  toDefinition: () => {
    const { columns, rows, hitPolicy } = get();
    const conditionCols = columns.filter((c) => c.role === 'condition');
    const actionCols = columns.filter((c) => c.role === 'action');

    return {
      hitPolicy,
      columns: columns.map((c, i) => ({
        name: c.name,
        columnType: c.role.toUpperCase(),
        operator: c.operator,
        dataType: c.type,
        index: i,
      })),
      rows: rows.map((r, i) => ({
        rowNumber: i + 1,
        priority: r.priority,
        conditions: conditionCols.map((col) => ({
          value: r.cells[col.id] ?? '',
        })),
        actions: actionCols.map((col) => ({
          value: r.cells[col.id] ?? '',
        })),
      })),
    };
  },

  reset: () => {
    set(initialState);
  },
}));
