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
    const def = definition as Record<string, unknown>;

    // Support engine format (categories/attributes/ranges) or UI format (characteristics/bins)
    const engineCategories = def.categories as Array<Record<string, unknown>> | undefined;
    const uiCharacteristics = def.characteristics as ScorecardCharacteristic[] | undefined;

    let characteristics: ScorecardCharacteristic[];

    if (engineCategories) {
      // Engine format → UI format
      characteristics = engineCategories.map((cat) => {
        const attrs = (cat.attributes as Array<Record<string, unknown>>) ?? [];
        // Use the first attribute as the main characteristic
        const attr = attrs[0] ?? {};
        const ranges = (attr.ranges as Array<Record<string, unknown>>) ?? [];
        return {
          id: generateCharId(),
          name: (cat.name as string) || 'Characteristic',
          field: (attr.factPath as string) || (attr.name as string) || '',
          dataType: 'NUMBER' as const,
          weight: (cat.weight as number) ?? 1,
          bins: ranges.map((r) => ({
            id: generateBinId(),
            label: (r.label as string) || '',
            operator: r.min !== undefined && r.max !== undefined ? 'BETWEEN' : (r.min !== undefined ? 'GTE' : 'LTE'),
            value: r.min !== undefined ? String(r.min) : (r.max !== undefined ? String(r.max) : ''),
            score: (r.points as number) ?? 0,
          })),
        };
      });
    } else {
      // UI format
      characteristics = (uiCharacteristics ?? []).map((c) => ({
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
    }

    set({
      baseScore: (def.baseScore as number) ?? 0,
      outputField: (def.outputField as string) ?? 'score',
      reasonCodes: (def.reasonCodes as boolean) ?? false,
      characteristics,
    });
  },

  toDefinition: () => {
    const { baseScore, characteristics } = get();
    return {
      baseScore,
      strategy: 'ADDITIVE',
      categories: characteristics.map((c) => ({
        name: c.name,
        weight: c.weight,
        attributes: [{
          name: c.name,
          factPath: c.field,
          defaultPoints: 0.0,
          ranges: c.bins.map((b) => {
            const numVal = parseFloat(b.value) || 0;
            if (b.operator === 'BETWEEN') {
              const parts = b.value.split(',').map((s) => parseFloat(s.trim()));
              return {
                min: parts[0] ?? 0,
                max: parts[1] ?? 0,
                points: b.score,
                label: b.label,
              };
            }
            if (b.operator === 'GTE' || b.operator === 'GT') {
              return { min: numVal, max: null, points: b.score, label: b.label };
            }
            if (b.operator === 'LTE' || b.operator === 'LT') {
              return { min: null, max: numVal, points: b.score, label: b.label };
            }
            // EQ and others: use as exact range
            return { min: numVal, max: numVal, points: b.score, label: b.label, minInclusive: true, maxInclusive: true };
          }),
        }],
      })),
    };
  },

  reset: () => {
    set(initialState);
  },
}));
