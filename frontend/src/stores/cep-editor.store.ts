import { create } from 'zustand';

export type CepPatternType = 'SIMPLE' | 'SEQUENCE' | 'ABSENCE' | 'COUNT' | 'AGGREGATION';
export type CepWindowType = 'TUMBLING' | 'SLIDING' | 'SESSION';
export type CepActionType = 'EMIT' | 'ALERT' | 'INVOKE_RULE';

export interface CepEventSchema {
  id: string;
  name: string;
  sourceField: string;
  filterExpression: string;
}

export interface CepWindow {
  type: CepWindowType;
  duration: number;
  unit: 'SECONDS' | 'MINUTES' | 'HOURS';
}

export interface CepAction {
  id: string;
  type: CepActionType;
  target: string;
  payload: string;
}

export interface CepPattern {
  id: string;
  name: string;
  type: CepPatternType;
  events: CepEventSchema[];
  window: CepWindow;
  threshold: number;
  groupByField: string;
  actions: CepAction[];
}

interface CepEditorState {
  patterns: CepPattern[];
  activePatternIndex: number;

  addPattern: () => void;
  removePattern: (index: number) => void;
  setActivePattern: (index: number) => void;
  updatePattern: (index: number, updates: Partial<CepPattern>) => void;
  addEvent: (patIndex: number) => void;
  removeEvent: (patIndex: number, eventIndex: number) => void;
  updateEvent: (patIndex: number, eventIndex: number, updates: Partial<CepEventSchema>) => void;
  updateWindow: (patIndex: number, window: Partial<CepWindow>) => void;
  addAction: (patIndex: number) => void;
  removeAction: (patIndex: number, actionIndex: number) => void;
  updateAction: (patIndex: number, actionIndex: number, updates: Partial<CepAction>) => void;
  loadFromDefinition: (definition: Record<string, unknown>) => void;
  toDefinition: () => Record<string, unknown>;
  reset: () => void;
}

let cepCounter = 0;

function generateId(prefix: string): string {
  return `${prefix}_${++cepCounter}_${Date.now()}`;
}

const defaultWindow: CepWindow = { type: 'TUMBLING', duration: 60, unit: 'SECONDS' };

const initialState = {
  patterns: [] as CepPattern[],
  activePatternIndex: 0,
};

export const useCepEditorStore = create<CepEditorState>((set, get) => ({
  ...initialState,

  addPattern: () => {
    set((state) => ({
      patterns: [
        ...state.patterns,
        {
          id: generateId('pat'),
          name: `Pattern ${state.patterns.length + 1}`,
          type: 'SIMPLE',
          events: [],
          window: { ...defaultWindow },
          threshold: 1,
          groupByField: '',
          actions: [],
        },
      ],
    }));
  },

  removePattern: (index) => {
    set((state) => {
      const patterns = state.patterns.filter((_, i) => i !== index);
      return {
        patterns,
        activePatternIndex: Math.min(state.activePatternIndex, Math.max(0, patterns.length - 1)),
      };
    });
  },

  setActivePattern: (index) => set({ activePatternIndex: index }),

  updatePattern: (index, updates) => {
    set((state) => ({
      patterns: state.patterns.map((p, i) =>
        i === index ? { ...p, ...updates } : p,
      ),
    }));
  },

  addEvent: (patIndex) => {
    set((state) => ({
      patterns: state.patterns.map((p, i) =>
        i === patIndex
          ? {
              ...p,
              events: [
                ...p.events,
                { id: generateId('evt'), name: '', sourceField: '', filterExpression: '' },
              ],
            }
          : p,
      ),
    }));
  },

  removeEvent: (patIndex, eventIndex) => {
    set((state) => ({
      patterns: state.patterns.map((p, i) =>
        i === patIndex
          ? { ...p, events: p.events.filter((_, ei) => ei !== eventIndex) }
          : p,
      ),
    }));
  },

  updateEvent: (patIndex, eventIndex, updates) => {
    set((state) => ({
      patterns: state.patterns.map((p, i) =>
        i === patIndex
          ? {
              ...p,
              events: p.events.map((e, ei) =>
                ei === eventIndex ? { ...e, ...updates } : e,
              ),
            }
          : p,
      ),
    }));
  },

  updateWindow: (patIndex, windowUpdates) => {
    set((state) => ({
      patterns: state.patterns.map((p, i) =>
        i === patIndex
          ? { ...p, window: { ...p.window, ...windowUpdates } }
          : p,
      ),
    }));
  },

  addAction: (patIndex) => {
    set((state) => ({
      patterns: state.patterns.map((p, i) =>
        i === patIndex
          ? {
              ...p,
              actions: [
                ...p.actions,
                { id: generateId('act'), type: 'EMIT' as CepActionType, target: '', payload: '' },
              ],
            }
          : p,
      ),
    }));
  },

  removeAction: (patIndex, actionIndex) => {
    set((state) => ({
      patterns: state.patterns.map((p, i) =>
        i === patIndex
          ? { ...p, actions: p.actions.filter((_, ai) => ai !== actionIndex) }
          : p,
      ),
    }));
  },

  updateAction: (patIndex, actionIndex, updates) => {
    set((state) => ({
      patterns: state.patterns.map((p, i) =>
        i === patIndex
          ? {
              ...p,
              actions: p.actions.map((a, ai) =>
                ai === actionIndex ? { ...a, ...updates } : a,
              ),
            }
          : p,
      ),
    }));
  },

  loadFromDefinition: (definition) => {
    const def = definition as { patterns?: CepPattern[] };

    const patterns = (def.patterns ?? []).map((p) => ({
      id: p.id || generateId('pat'),
      name: p.name || 'Pattern',
      type: p.type || 'SIMPLE' as CepPatternType,
      events: (p.events ?? []).map((e) => ({
        id: e.id || generateId('evt'),
        name: e.name || '',
        sourceField: e.sourceField || '',
        filterExpression: e.filterExpression || '',
      })),
      window: {
        type: p.window?.type || 'TUMBLING' as CepWindowType,
        duration: p.window?.duration ?? 60,
        unit: p.window?.unit || 'SECONDS' as const,
      },
      threshold: p.threshold ?? 1,
      groupByField: p.groupByField || '',
      actions: (p.actions ?? []).map((a) => ({
        id: a.id || generateId('act'),
        type: a.type || 'EMIT' as CepActionType,
        target: a.target || '',
        payload: a.payload || '',
      })),
    }));

    set({
      patterns,
      activePatternIndex: 0,
    });
  },

  toDefinition: () => {
    const { patterns } = get();
    return {
      patterns: patterns.map((p) => ({
        id: p.id,
        name: p.name,
        type: p.type,
        events: p.events,
        window: p.window,
        threshold: p.threshold,
        groupByField: p.groupByField,
        actions: p.actions,
      })),
    };
  },

  reset: () => {
    set(initialState);
  },
}));
