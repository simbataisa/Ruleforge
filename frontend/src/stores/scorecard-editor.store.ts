import { create } from 'zustand';

export interface ScorecardBin {
  id: string;
  label: string;
  operator: string;
  value: string;
  score: number;
}

export interface ScorecardCharacteristic {
  id: string;
  name: string;
  field: string;
  dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE';
  weight: number;
  bins: ScorecardBin[];
}

interface ScorecardEditorState {
  baseScore: number;
  outputField: string;
  reasonCodes: boolean;
  characteristics: ScorecardCharacteristic[];

  setBaseScore: (score: number) => void;
  setOutputField: (field: string) => void;
  setReasonCodes: (enabled: boolean) => void;
  addCharacteristic: () => void;
  removeCharacteristic: (index: number) => void;
  updateCharacteristic: (index: number, updates: Partial<ScorecardCharacteristic>) => void;
  addBin: (charIndex: number) => void;
  removeBin: (charIndex: number, binIndex: number) => void;
  updateBin: (charIndex: number, binIndex: number, updates: Partial<ScorecardBin>) => void;
  loadFromDefinition: (definition: Record<string, unknown>) => void;
  toDefinition: () => Record<string, unknown>;
  reset: () => void;
}

let charCounter = 0;
let binCounter = 0;

function generateCharId(): string {
  return `char_${++charCounter}_${Date.now()}`;
}

function generateBinId(): string {
  return `bin_${++binCounter}_${Date.now()}`;
}

const initialState = {
  baseScore: 0,
  outputField: 'score',
  reasonCodes: false,
  characteristics: [] as ScorecardCharacteristic[],
};

export const useScorecardEditorStore = create<ScorecardEditorState>((set, get) => ({
  ...initialState,

  setBaseScore: (score) => set({ baseScore: score }),

  setOutputField: (field) => set({ outputField: field }),

  setReasonCodes: (enabled) => set({ reasonCodes: enabled }),

  addCharacteristic: () => {
    const id = generateCharId();
    set((state) => ({
      characteristics: [
        ...state.characteristics,
        {
          id,
          name: `Characteristic ${state.characteristics.length + 1}`,
          field: '',
          dataType: 'NUMBER',
          weight: 1,
          bins: [],
        },
      ],
    }));
  },

  removeCharacteristic: (index) => {
    set((state) => ({
      characteristics: state.characteristics.filter((_, i) => i !== index),
    }));
  },

  updateCharacteristic: (index, updates) => {
    set((state) => ({
      characteristics: state.characteristics.map((c, i) =>
        i === index ? { ...c, ...updates } : c,
      ),
    }));
  },

  addBin: (charIndex) => {
    const id = generateBinId();
    set((state) => ({
      characteristics: state.characteristics.map((c, i) =>
        i === charIndex
          ? {
              ...c,
              bins: [
                ...c.bins,
                { id, label: '', operator: 'EQ', value: '', score: 0 },
              ],
            }
          : c,
      ),
    }));
  },

  removeBin: (charIndex, binIndex) => {
    set((state) => ({
      characteristics: state.characteristics.map((c, i) =>
        i === charIndex
          ? { ...c, bins: c.bins.filter((_, bi) => bi !== binIndex) }
          : c,
      ),
    }));
  },

  updateBin: (charIndex, binIndex, updates) => {
    set((state) => ({
      characteristics: state.characteristics.map((c, i) =>
        i === charIndex
          ? {
              ...c,
              bins: c.bins.map((b, bi) =>
                bi === binIndex ? { ...b, ...updates } : b,
              ),
            }
          : c,
      ),
    }));
  },

  loadFromDefinition: (definition) => {
    const def = definition as {
      baseScore?: number;
      outputField?: string;
      reasonCodes?: boolean;
      characteristics?: ScorecardCharacteristic[];
    };

    const characteristics = (def.characteristics ?? []).map((c) => ({
      id: c.id || generateCharId(),
      name: c.name || 'Characteristic',
      field: c.field || '',
      dataType: c.dataType || 'NUMBER' as const,
      weight: c.weight ?? 1,
      bins: (c.bins ?? []).map((b) => ({
        id: b.id || generateBinId(),
        label: b.label || '',
        operator: b.operator || 'EQ',
        value: b.value || '',
        score: b.score ?? 0,
      })),
    }));

    set({
      baseScore: def.baseScore ?? 0,
      outputField: def.outputField ?? 'score',
      reasonCodes: def.reasonCodes ?? false,
      characteristics,
    });
  },

  toDefinition: () => {
    const { baseScore, outputField, reasonCodes, characteristics } = get();
    return {
      baseScore,
      outputField,
      reasonCodes,
      characteristics: characteristics.map((c) => ({
        id: c.id,
        name: c.name,
        field: c.field,
        dataType: c.dataType,
        weight: c.weight,
        bins: c.bins.map((b) => ({
          id: b.id,
          label: b.label,
          operator: b.operator,
          value: b.value,
          score: b.score,
        })),
      })),
    };
  },

  reset: () => {
    set(initialState);
  },
}));
