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
    const def = definition as Record<string, unknown>;

    // Support both engine format (flat patternType + eventSchemas) and UI format (patterns array)
    if (def.patterns && Array.isArray(def.patterns)) {
      // UI format
      const rawPatterns = def.patterns as CepPattern[];
      const patterns = rawPatterns.map((p) => ({
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
      set({ patterns, activePatternIndex: 0 });
      return;
    }

    // Engine format - flat structure → wrap in single pattern
    const enginePatternType = def.patternType as string | undefined;
    const eventSchemas = (def.eventSchemas as Array<Record<string, unknown>>) ?? [];
    const timeWindow = def.timeWindow as Record<string, unknown> | undefined;
    const engineActions = (def.actions as Array<Record<string, unknown>>) ?? [];

    // Map engine pattern types to UI types
    const patternTypeMap: Record<string, CepPatternType> = {
      'SEQUENCE': 'SEQUENCE',
      'FREQUENCY': 'COUNT',
      'AGGREGATION': 'AGGREGATION',
      'ABSENCE': 'ABSENCE',
      'CONJUNCTION': 'SIMPLE',
    };
    const uiPatternType = patternTypeMap[enginePatternType ?? ''] ?? (enginePatternType as CepPatternType) ?? 'SIMPLE';

    // Map engine action types to UI types
    const actionTypeMap: Record<string, CepActionType> = {
      'EMIT_EVENT': 'EMIT',
      'ALERT': 'ALERT',
      'INVOKE_RULE': 'INVOKE_RULE',
    };

    // Convert duration from seconds to best unit
    const durationSec = (timeWindow?.duration as number) ?? 300;
    let duration = durationSec;
    let unit: 'SECONDS' | 'MINUTES' | 'HOURS' = 'SECONDS';
    if (durationSec >= 3600 && durationSec % 3600 === 0) {
      duration = durationSec / 3600;
      unit = 'HOURS';
    } else if (durationSec >= 60 && durationSec % 60 === 0) {
      duration = durationSec / 60;
      unit = 'MINUTES';
    }

    const pattern: CepPattern = {
      id: generateId('pat'),
      name: 'Pattern 1',
      type: uiPatternType,
      events: eventSchemas.map((es) => ({
        id: generateId('evt'),
        name: (es.eventType as string) || '',
        sourceField: '',
        filterExpression: ((es.filterConditions as Array<Record<string, unknown>>) ?? [])
          .map((fc) => `${fc.field} ${fc.operator} ${fc.value}`)
          .join(' && '),
      })),
      window: {
        type: ((timeWindow?.type as string) || 'TUMBLING') as CepWindowType,
        duration,
        unit,
      },
      threshold: 1,
      groupByField: '',
      actions: engineActions.map((a) => ({
        id: generateId('act'),
        type: (actionTypeMap[(a.actionType as string) ?? ''] ?? 'EMIT') as CepActionType,
        target: '',
        payload: JSON.stringify(a.parameters ?? {}),
      })),
    };

    set({ patterns: [pattern], activePatternIndex: 0 });
  },

  toDefinition: () => {
    const { patterns } = get();
    if (patterns.length === 0) {
      return { patternType: 'SEQUENCE', eventSchemas: [], timeWindow: { type: 'TUMBLING', duration: 300, graceTime: 0 }, actions: [] };
    }

    const p = patterns[0];

    // Map UI pattern types to engine types
    const patternTypeMap: Record<string, string> = {
      'SIMPLE': 'SEQUENCE',
      'SEQUENCE': 'SEQUENCE',
      'COUNT': 'FREQUENCY',
      'AGGREGATION': 'AGGREGATION',
      'ABSENCE': 'ABSENCE',
    };

    // Map UI action types to engine types
    const actionTypeMap: Record<string, string> = {
      'EMIT': 'EMIT_EVENT',
      'ALERT': 'ALERT',
      'INVOKE_RULE': 'INVOKE_RULE',
    };

    // Convert duration to seconds
    const multiplier = p.window.unit === 'HOURS' ? 3600 : p.window.unit === 'MINUTES' ? 60 : 1;
    const durationSec = p.window.duration * multiplier;

    return {
      patternType: patternTypeMap[p.type] ?? p.type,
      eventSchemas: p.events.map((e) => ({
        eventType: e.name || 'unknown',
        fields: {} as Record<string, string>,
        filterConditions: e.filterExpression
          ? e.filterExpression.split('&&').map((expr) => {
              const parts = expr.trim().split(/\s+/);
              return { field: parts[0] ?? '', operator: parts[1] ?? 'EQ', value: parts[2] ?? '' };
            })
          : [],
      })),
      timeWindow: {
        type: p.window.type,
        duration: durationSec,
        graceTime: 0,
      },
      actions: p.actions.map((a) => ({
        actionType: actionTypeMap[a.type] ?? a.type,
        parameters: a.payload ? (() => { try { return JSON.parse(a.payload); } catch { return {}; } })() : {},
      })),
      inputTopic: 'rule.execution',
      outputTopic: 'cep.results',
    };
  },

  reset: () => {
    set(initialState);
  },
}));
