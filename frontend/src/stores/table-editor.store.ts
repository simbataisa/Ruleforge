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
      columns?: ColumnDef[];
      rows?: Array<{ priority?: number; cells?: Record<string, string> }>;
    };

    const columns = (def.columns ?? []).map((c, i) => ({
      id: c.id || generateColId(),
      name: c.name || `Column ${i + 1}`,
      role: c.role || 'condition' as ColumnRole,
      type: c.type || 'STRING' as ColumnType,
      operator: c.operator || 'EQ' as Operator,
    }));

    const rows = (def.rows ?? []).map((r, i) => ({
      id: generateRowId(),
      priority: r.priority ?? i + 1,
      cells: r.cells ?? {},
    }));

    set({
      hitPolicy: def.hitPolicy ?? 'FIRST',
      columns,
      rows,
      selectedCell: null,
    });
  },

  toDefinition: () => {
    const { columns, rows, hitPolicy } = get();
    return {
      hitPolicy,
      columns: columns.map((c) => ({
        id: c.id,
        name: c.name,
        role: c.role,
        type: c.type,
        operator: c.operator,
      })),
      rows: rows.map((r) => ({
        priority: r.priority,
        cells: r.cells,
      })),
    };
  },

  reset: () => {
    set(initialState);
  },
}));
