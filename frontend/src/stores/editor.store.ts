import { create } from 'zustand';

interface EditorState {
  isDirty: boolean;
  selectedCellId: string | null;
  setDirty: (dirty: boolean) => void;
  selectCell: (cellId: string | null) => void;
}

export const useEditorStore = create<EditorState>((set) => ({
  isDirty: false,
  selectedCellId: null,
  setDirty: (dirty) => set({ isDirty: dirty }),
  selectCell: (cellId) => set({ selectedCellId: cellId }),
}));
