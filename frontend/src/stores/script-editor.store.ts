import { create } from 'zustand';

interface ScriptEditorState {
  scriptBody: string;
  language: string;
  timeout: number;
  maxMemoryMb: number;
  isDirty: boolean;
  setScriptBody: (body: string) => void;
  setLanguage: (lang: string) => void;
  setTimeout: (ms: number) => void;
  setMaxMemoryMb: (mb: number) => void;
  setDefinition: (def: { scriptBody: string; language: string; timeout: number; maxMemoryMb: number }) => void;
  reset: () => void;
}

const DEFAULT_SCRIPT = 'function evaluate(input) {\n  // Write your rule logic here\n  return {\n    result: true\n  };\n}';

export const useScriptEditorStore = create<ScriptEditorState>((set) => ({
  scriptBody: DEFAULT_SCRIPT,
  language: 'js',
  timeout: 5000,
  maxMemoryMb: 64,
  isDirty: false,
  setScriptBody: (body) => set({ scriptBody: body, isDirty: true }),
  setLanguage: (lang) => set({ language: lang, isDirty: true }),
  setTimeout: (ms) => set({ timeout: ms, isDirty: true }),
  setMaxMemoryMb: (mb) => set({ maxMemoryMb: mb, isDirty: true }),
  setDefinition: (def) => set({ ...def, isDirty: false }),
  reset: () => set({ scriptBody: DEFAULT_SCRIPT, language: 'js', timeout: 5000, maxMemoryMb: 64, isDirty: false }),
}));
