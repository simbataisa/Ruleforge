import { useCallback, useRef, useEffect } from 'react';
import { Plus, Trash2, GripVertical } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Select } from '@/shared/components/Select';
import { useTableEditorStore } from '@/stores/table-editor.store';
import { useEditorStore } from '@/stores/editor.store';
import type {
  ColumnType,
  Operator,
  HitPolicy,
  ColumnRole,
} from '@/stores/table-editor.store';

const COLUMN_TYPES: { value: ColumnType; label: string }[] = [
  { value: 'STRING', label: 'String' },
  { value: 'NUMBER', label: 'Number' },
  { value: 'BOOLEAN', label: 'Boolean' },
  { value: 'DATE', label: 'Date' },
];

const OPERATORS: { value: Operator; label: string }[] = [
  { value: 'EQ', label: '=' },
  { value: 'NEQ', label: '!=' },
  { value: 'GT', label: '>' },
  { value: 'GTE', label: '>=' },
  { value: 'LT', label: '<' },
  { value: 'LTE', label: '<=' },
  { value: 'BETWEEN', label: 'Between' },
  { value: 'IN', label: 'In' },
  { value: 'CONTAINS', label: 'Contains' },
  { value: 'MATCHES', label: 'Matches' },
  { value: 'IS_NULL', label: 'Is Null' },
];

const HIT_POLICIES: { value: HitPolicy; label: string }[] = [
  { value: 'FIRST', label: 'First' },
  { value: 'ALL', label: 'All' },
  { value: 'PRIORITY', label: 'Priority' },
  { value: 'AGGREGATE', label: 'Aggregate' },
  { value: 'RANKED', label: 'Ranked' },
];

interface EditableCellProps {
  value: string;
  isSelected: boolean;
  onSelect: () => void;
  onChange: (value: string) => void;
  onTab: (shift: boolean) => void;
}

function EditableCell({
  value,
  isSelected,
  onSelect,
  onChange,
  onTab,
}: EditableCellProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isSelected && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isSelected]);

  return (
    <td
      className={`border border-gray-200 px-0 py-0 relative ${
        isSelected ? 'ring-2 ring-indigo-500 ring-inset z-10' : ''
      }`}
      onClick={onSelect}
    >
      {isSelected ? (
        <input
          ref={inputRef}
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Tab') {
              e.preventDefault();
              onTab(e.shiftKey);
            }
            if (e.key === 'Escape') {
              inputRef.current?.blur();
            }
          }}
          className="w-full h-full px-2 py-1.5 text-sm border-none outline-none bg-white"
        />
      ) : (
        <div className="px-2 py-1.5 text-sm text-gray-700 min-h-[32px] cursor-text truncate">
          {value || <span className="text-gray-300">--</span>}
        </div>
      )}
    </td>
  );
}

interface ColumnHeaderProps {
  name: string;
  type: ColumnType;
  operator: Operator;
  role: ColumnRole;
  onUpdateName: (name: string) => void;
  onUpdateType: (type: ColumnType) => void;
  onUpdateOperator: (operator: Operator) => void;
  onRemove: () => void;
}

function ColumnHeader({
  name,
  type,
  operator,
  role,
  onUpdateName,
  onUpdateType,
  onUpdateOperator,
  onRemove,
}: ColumnHeaderProps) {
  const bgColor =
    role === 'condition' ? 'bg-blue-50' : 'bg-green-50';
  const borderColor =
    role === 'condition' ? 'border-blue-200' : 'border-green-200';

  return (
    <th
      className={`${bgColor} border ${borderColor} px-2 py-1 text-left min-w-[160px] max-w-[200px]`}
    >
      <div className="space-y-1">
        <div className="flex items-center justify-between gap-1">
          <input
            type="text"
            value={name}
            onChange={(e) => onUpdateName(e.target.value)}
            className={`text-xs font-semibold bg-transparent border-none outline-none flex-1 min-w-0 focus:ring-1 focus:ring-indigo-400 rounded px-1 ${
              role === 'condition' ? 'text-blue-800' : 'text-green-800'
            }`}
          />
          <button
            onClick={onRemove}
            className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 flex-shrink-0"
            title="Remove column"
          >
            <Trash2 className="h-3 w-3" />
          </button>
        </div>
        <div className="flex gap-1">
          <select
            value={type}
            onChange={(e) => onUpdateType(e.target.value as ColumnType)}
            className="text-[10px] bg-white border border-gray-200 rounded px-1 py-0.5 text-gray-600 flex-1 min-w-0"
          >
            {COLUMN_TYPES.map((ct) => (
              <option key={ct.value} value={ct.value}>
                {ct.label}
              </option>
            ))}
          </select>
          {role === 'condition' && (
            <select
              value={operator}
              onChange={(e) => onUpdateOperator(e.target.value as Operator)}
              className="text-[10px] bg-white border border-gray-200 rounded px-1 py-0.5 text-gray-600 flex-1 min-w-0"
            >
              {OPERATORS.map((op) => (
                <option key={op.value} value={op.value}>
                  {op.label}
                </option>
              ))}
            </select>
          )}
        </div>
      </div>
    </th>
  );
}

export function TableEditor() {
  const {
    columns,
    rows,
    hitPolicy,
    selectedCell,
    addColumn,
    removeColumn,
    updateColumn,
    addRow,
    removeRow,
    updateCell,
    selectCell,
    clearSelection,
    setHitPolicy,
    updateRowPriority,
  } = useTableEditorStore();

  const setDirty = useEditorStore((s) => s.setDirty);

  const markDirty = useCallback(() => {
    setDirty(true);
  }, [setDirty]);

  const handleCellChange = useCallback(
    (row: number, col: number, value: string) => {
      updateCell(row, col, value);
      markDirty();
    },
    [updateCell, markDirty],
  );

  const handleTab = useCallback(
    (row: number, col: number, shift: boolean) => {
      if (shift) {
        if (col > 0) {
          selectCell(row, col - 1);
        } else if (row > 0) {
          selectCell(row - 1, columns.length - 1);
        }
      } else {
        if (col < columns.length - 1) {
          selectCell(row, col + 1);
        } else if (row < rows.length - 1) {
          selectCell(row + 1, 0);
        }
      }
    },
    [columns.length, rows.length, selectCell],
  );

  const handleAddColumn = useCallback(
    (role: ColumnRole) => {
      addColumn(role);
      markDirty();
    },
    [addColumn, markDirty],
  );

  const handleRemoveColumn = useCallback(
    (index: number) => {
      removeColumn(index);
      markDirty();
    },
    [removeColumn, markDirty],
  );

  const handleAddRow = useCallback(() => {
    addRow();
    markDirty();
  }, [addRow, markDirty]);

  const handleRemoveRow = useCallback(
    (index: number) => {
      removeRow(index);
      markDirty();
    },
    [removeRow, markDirty],
  );

  const conditionCols = columns.filter((c) => c.role === 'condition');
  const actionCols = columns.filter((c) => c.role === 'action');

  return (
    <div
      className="flex flex-col h-full bg-white"
      onClick={(e) => {
        if ((e.target as HTMLElement).tagName === 'DIV') {
          clearSelection();
        }
      }}
    >
      {/* Toolbar */}
      <div className="flex items-center gap-3 px-4 py-2 border-b border-gray-200 bg-gray-50 flex-shrink-0">
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-gray-600">Hit Policy:</span>
          <Select
            value={hitPolicy}
            onChange={(e) => {
              setHitPolicy(e.target.value as HitPolicy);
              markDirty();
            }}
            className="text-xs h-7 w-28"
          >
            {HIT_POLICIES.map((hp) => (
              <option key={hp.value} value={hp.value}>
                {hp.label}
              </option>
            ))}
          </Select>
        </div>
        <div className="h-4 w-px bg-gray-300" />
        <Button
          size="sm"
          variant="outline"
          onClick={() => handleAddColumn('condition')}
          className="text-xs"
        >
          <Plus className="h-3 w-3" />
          Condition
        </Button>
        <Button
          size="sm"
          variant="outline"
          onClick={() => handleAddColumn('action')}
          className="text-xs"
        >
          <Plus className="h-3 w-3" />
          Action
        </Button>
        <div className="h-4 w-px bg-gray-300" />
        <Button
          size="sm"
          variant="outline"
          onClick={handleAddRow}
          className="text-xs"
        >
          <Plus className="h-3 w-3" />
          Row
        </Button>
      </div>

      {/* Table */}
      <div className="flex-1 overflow-auto">
        {columns.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
            <p className="text-sm">
              No columns defined yet. Add condition and action columns to start
              building your decision table.
            </p>
            <div className="flex gap-2">
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleAddColumn('condition')}
              >
                <Plus className="h-3 w-3" />
                Add Condition
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleAddColumn('action')}
              >
                <Plus className="h-3 w-3" />
                Add Action
              </Button>
            </div>
          </div>
        ) : (
          <table className="w-full border-collapse text-sm">
            <thead>
              {/* Role header row */}
              <tr>
                <th className="bg-gray-50 border border-gray-200 px-2 py-1 text-xs text-gray-500 w-10">
                  #
                </th>
                {hitPolicy === 'PRIORITY' && (
                  <th className="bg-gray-50 border border-gray-200 px-2 py-1 text-xs text-gray-500 w-16">
                    Priority
                  </th>
                )}
                {conditionCols.length > 0 && (
                  <th
                    colSpan={conditionCols.length}
                    className="bg-blue-50 border border-blue-200 px-2 py-1 text-xs font-semibold text-blue-700 text-center"
                  >
                    Conditions
                  </th>
                )}
                {actionCols.length > 0 && (
                  <th
                    colSpan={actionCols.length}
                    className="bg-green-50 border border-green-200 px-2 py-1 text-xs font-semibold text-green-700 text-center"
                  >
                    Actions
                  </th>
                )}
                <th className="bg-gray-50 border border-gray-200 w-10" />
              </tr>
              {/* Column definition row */}
              <tr>
                <th className="bg-gray-50 border border-gray-200" />
                {hitPolicy === 'PRIORITY' && (
                  <th className="bg-gray-50 border border-gray-200" />
                )}
                {columns.map((col, colIndex) => (
                  <ColumnHeader
                    key={col.id}
                    name={col.name}
                    type={col.type}
                    operator={col.operator}
                    role={col.role}
                    onUpdateName={(name) => {
                      updateColumn(colIndex, { name });
                      markDirty();
                    }}
                    onUpdateType={(type) => {
                      updateColumn(colIndex, { type });
                      markDirty();
                    }}
                    onUpdateOperator={(operator) => {
                      updateColumn(colIndex, { operator });
                      markDirty();
                    }}
                    onRemove={() => handleRemoveColumn(colIndex)}
                  />
                ))}
                <th className="bg-gray-50 border border-gray-200" />
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td
                    colSpan={
                      columns.length +
                      2 +
                      (hitPolicy === 'PRIORITY' ? 1 : 0)
                    }
                    className="border border-gray-200 px-4 py-6 text-center text-gray-400 text-sm"
                  >
                    No rows yet. Click "Row" to add a rule row.
                  </td>
                </tr>
              ) : (
                rows.map((row, rowIndex) => (
                  <tr key={row.id} className="group">
                    <td className="border border-gray-200 px-2 py-1.5 text-xs text-gray-400 text-center bg-gray-50 w-10">
                      <div className="flex items-center gap-0.5 justify-center">
                        <GripVertical className="h-3 w-3 text-gray-300 opacity-0 group-hover:opacity-100" />
                        <span>{rowIndex + 1}</span>
                      </div>
                    </td>
                    {hitPolicy === 'PRIORITY' && (
                      <td className="border border-gray-200 px-0 py-0 w-16">
                        <input
                          type="number"
                          value={row.priority}
                          onChange={(e) => {
                            updateRowPriority(
                              rowIndex,
                              parseInt(e.target.value, 10) || 0,
                            );
                            markDirty();
                          }}
                          className="w-full px-2 py-1.5 text-sm border-none outline-none text-center bg-transparent focus:ring-1 focus:ring-indigo-400"
                          min={1}
                        />
                      </td>
                    )}
                    {columns.map((col, colIndex) => (
                      <EditableCell
                        key={col.id}
                        value={row.cells[col.id] ?? ''}
                        isSelected={
                          selectedCell?.row === rowIndex &&
                          selectedCell?.col === colIndex
                        }
                        onSelect={() => selectCell(rowIndex, colIndex)}
                        onChange={(value) =>
                          handleCellChange(rowIndex, colIndex, value)
                        }
                        onTab={(shift) => handleTab(rowIndex, colIndex, shift)}
                      />
                    ))}
                    <td className="border border-gray-200 px-1 py-1 w-10 bg-gray-50">
                      <button
                        onClick={() => handleRemoveRow(rowIndex)}
                        className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
                        title="Remove row"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
