import { create } from 'zustand';

export type EnsembleStrategy = 'VOTING' | 'WEIGHTED_AVERAGE' | 'FIRST_WINS' | 'BEST_SCORE' | 'UNANIMOUS';

export interface EnsembleMember {
  id: string;
  ruleId: string;
  alias: string;
  weight: number;
  enabled: boolean;
}

interface EnsembleEditorState {
  strategy: EnsembleStrategy;
  outputField: string;
  threshold: number;
  members: EnsembleMember[];

  setStrategy: (strategy: EnsembleStrategy) => void;
  setOutputField: (field: string) => void;
  setThreshold: (threshold: number) => void;
  addMember: () => void;
  removeMember: (index: number) => void;
  updateMember: (index: number, updates: Partial<EnsembleMember>) => void;
  reorderMember: (fromIndex: number, toIndex: number) => void;
  loadFromDefinition: (definition: Record<string, unknown>) => void;
  toDefinition: () => Record<string, unknown>;
  reset: () => void;
}

let memberCounter = 0;

function generateMemberId(): string {
  return `member_${++memberCounter}_${Date.now()}`;
}

const initialState = {
  strategy: 'VOTING' as EnsembleStrategy,
  outputField: 'result',
  threshold: 0.5,
  members: [] as EnsembleMember[],
};

export const useEnsembleEditorStore = create<EnsembleEditorState>((set, get) => ({
  ...initialState,

  setStrategy: (strategy) => set({ strategy }),

  setOutputField: (field) => set({ outputField: field }),

  setThreshold: (threshold) => set({ threshold }),

  addMember: () => {
    const id = generateMemberId();
    set((state) => ({
      members: [
        ...state.members,
        {
          id,
          ruleId: '',
          alias: `Member ${state.members.length + 1}`,
          weight: 1,
          enabled: true,
        },
      ],
    }));
  },

  removeMember: (index) => {
    set((state) => ({
      members: state.members.filter((_, i) => i !== index),
    }));
  },

  updateMember: (index, updates) => {
    set((state) => ({
      members: state.members.map((m, i) =>
        i === index ? { ...m, ...updates } : m,
      ),
    }));
  },

  reorderMember: (fromIndex, toIndex) => {
    set((state) => {
      const members = [...state.members];
      const [moved] = members.splice(fromIndex, 1);
      members.splice(toIndex, 0, moved);
      return { members };
    });
  },

  loadFromDefinition: (definition) => {
    const def = definition as {
      strategy?: EnsembleStrategy;
      outputField?: string;
      threshold?: number;
      members?: EnsembleMember[];
    };

    const members = (def.members ?? []).map((m) => ({
      id: m.id || generateMemberId(),
      ruleId: m.ruleId || '',
      alias: m.alias || 'Member',
      weight: m.weight ?? 1,
      enabled: m.enabled ?? true,
    }));

    set({
      strategy: def.strategy ?? 'VOTING',
      outputField: def.outputField ?? 'result',
      threshold: def.threshold ?? 0.5,
      members,
    });
  },

  toDefinition: () => {
    const { strategy, outputField, threshold, members } = get();
    return {
      strategy,
      outputField,
      threshold,
      members: members.map((m) => ({
        id: m.id,
        ruleId: m.ruleId,
        alias: m.alias,
        weight: m.weight,
        enabled: m.enabled,
      })),
    };
  },

  reset: () => {
    set(initialState);
  },
}));
