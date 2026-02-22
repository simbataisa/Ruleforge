import { create } from 'zustand';

export type DmnHitPolicy = 'UNIQUE' | 'ANY' | 'PRIORITY' | 'FIRST' | 'OUTPUT_ORDER' | 'RULE_ORDER' | 'COLLECT';

export interface DmnInputColumn {
  id: string;
  label: string;
  expression: string;
  typeRef: string;
}

export interface DmnOutputColumn {
  id: string;
  label: string;
  name: string;
  typeRef: string;
}

export interface DmnRule {
  id: string;
  inputEntries: Record<string, string>;
  outputEntries: Record<string, string>;
  annotation: string;
}

export interface DmnDecision {
  id: string;
  name: string;
  hitPolicy: DmnHitPolicy;
  inputColumns: DmnInputColumn[];
  outputColumns: DmnOutputColumn[];
  rules: DmnRule[];
}

interface DmnEditorState {
  decisions: DmnDecision[];
  activeDecisionIndex: number;

  addDecision: () => void;
  removeDecision: (index: number) => void;
  setActiveDecision: (index: number) => void;
  updateDecision: (index: number, updates: Partial<DmnDecision>) => void;
  addInputColumn: (decIndex: number) => void;
  removeInputColumn: (decIndex: number, colIndex: number) => void;
  updateInputColumn: (decIndex: number, colIndex: number, updates: Partial<DmnInputColumn>) => void;
  addOutputColumn: (decIndex: number) => void;
  removeOutputColumn: (decIndex: number, colIndex: number) => void;
  updateOutputColumn: (decIndex: number, colIndex: number, updates: Partial<DmnOutputColumn>) => void;
  addRule: (decIndex: number) => void;
  removeRule: (decIndex: number, ruleIndex: number) => void;
  updateRuleEntry: (decIndex: number, ruleIndex: number, colId: string, value: string, type: 'input' | 'output') => void;
  updateRuleAnnotation: (decIndex: number, ruleIndex: number, annotation: string) => void;
  loadFromDefinition: (definition: Record<string, unknown>) => void;
  toDefinition: () => Record<string, unknown>;
  reset: () => void;
}

let dmnCounter = 0;

function generateId(prefix: string): string {
  return `${prefix}_${++dmnCounter}_${Date.now()}`;
}

const initialState = {
  decisions: [] as DmnDecision[],
  activeDecisionIndex: 0,
};

export const useDmnEditorStore = create<DmnEditorState>((set, get) => ({
  ...initialState,

  addDecision: () => {
    set((state) => ({
      decisions: [
        ...state.decisions,
        {
          id: generateId('dec'),
          name: `Decision ${state.decisions.length + 1}`,
          hitPolicy: 'UNIQUE',
          inputColumns: [],
          outputColumns: [],
          rules: [],
        },
      ],
    }));
  },

  removeDecision: (index) => {
    set((state) => {
      const decisions = state.decisions.filter((_, i) => i !== index);
      return {
        decisions,
        activeDecisionIndex: Math.min(state.activeDecisionIndex, Math.max(0, decisions.length - 1)),
      };
    });
  },

  setActiveDecision: (index) => set({ activeDecisionIndex: index }),

  updateDecision: (index, updates) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) =>
        i === index ? { ...d, ...updates } : d,
      ),
    }));
  },

  addInputColumn: (decIndex) => {
    const id = generateId('input');
    set((state) => ({
      decisions: state.decisions.map((d, i) =>
        i === decIndex
          ? {
              ...d,
              inputColumns: [
                ...d.inputColumns,
                { id, label: `Input ${d.inputColumns.length + 1}`, expression: '', typeRef: 'string' },
              ],
              rules: d.rules.map((r) => ({
                ...r,
                inputEntries: { ...r.inputEntries, [id]: '' },
              })),
            }
          : d,
      ),
    }));
  },

  removeInputColumn: (decIndex, colIndex) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) => {
        if (i !== decIndex) return d;
        const col = d.inputColumns[colIndex];
        if (!col) return d;
        return {
          ...d,
          inputColumns: d.inputColumns.filter((_, ci) => ci !== colIndex),
          rules: d.rules.map((r) => {
            const entries = { ...r.inputEntries };
            delete entries[col.id];
            return { ...r, inputEntries: entries };
          }),
        };
      }),
    }));
  },

  updateInputColumn: (decIndex, colIndex, updates) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) =>
        i === decIndex
          ? {
              ...d,
              inputColumns: d.inputColumns.map((c, ci) =>
                ci === colIndex ? { ...c, ...updates } : c,
              ),
            }
          : d,
      ),
    }));
  },

  addOutputColumn: (decIndex) => {
    const id = generateId('output');
    set((state) => ({
      decisions: state.decisions.map((d, i) =>
        i === decIndex
          ? {
              ...d,
              outputColumns: [
                ...d.outputColumns,
                { id, label: `Output ${d.outputColumns.length + 1}`, name: '', typeRef: 'string' },
              ],
              rules: d.rules.map((r) => ({
                ...r,
                outputEntries: { ...r.outputEntries, [id]: '' },
              })),
            }
          : d,
      ),
    }));
  },

  removeOutputColumn: (decIndex, colIndex) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) => {
        if (i !== decIndex) return d;
        const col = d.outputColumns[colIndex];
        if (!col) return d;
        return {
          ...d,
          outputColumns: d.outputColumns.filter((_, ci) => ci !== colIndex),
          rules: d.rules.map((r) => {
            const entries = { ...r.outputEntries };
            delete entries[col.id];
            return { ...r, outputEntries: entries };
          }),
        };
      }),
    }));
  },

  updateOutputColumn: (decIndex, colIndex, updates) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) =>
        i === decIndex
          ? {
              ...d,
              outputColumns: d.outputColumns.map((c, ci) =>
                ci === colIndex ? { ...c, ...updates } : c,
              ),
            }
          : d,
      ),
    }));
  },

  addRule: (decIndex) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) => {
        if (i !== decIndex) return d;
        const inputEntries: Record<string, string> = {};
        d.inputColumns.forEach((c) => { inputEntries[c.id] = ''; });
        const outputEntries: Record<string, string> = {};
        d.outputColumns.forEach((c) => { outputEntries[c.id] = ''; });
        return {
          ...d,
          rules: [
            ...d.rules,
            { id: generateId('rule'), inputEntries, outputEntries, annotation: '' },
          ],
        };
      }),
    }));
  },

  removeRule: (decIndex, ruleIndex) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) =>
        i === decIndex
          ? { ...d, rules: d.rules.filter((_, ri) => ri !== ruleIndex) }
          : d,
      ),
    }));
  },

  updateRuleEntry: (decIndex, ruleIndex, colId, value, type) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) =>
        i === decIndex
          ? {
              ...d,
              rules: d.rules.map((r, ri) =>
                ri === ruleIndex
                  ? {
                      ...r,
                      ...(type === 'input'
                        ? { inputEntries: { ...r.inputEntries, [colId]: value } }
                        : { outputEntries: { ...r.outputEntries, [colId]: value } }),
                    }
                  : r,
              ),
            }
          : d,
      ),
    }));
  },

  updateRuleAnnotation: (decIndex, ruleIndex, annotation) => {
    set((state) => ({
      decisions: state.decisions.map((d, i) =>
        i === decIndex
          ? {
              ...d,
              rules: d.rules.map((r, ri) =>
                ri === ruleIndex ? { ...r, annotation } : r,
              ),
            }
          : d,
      ),
    }));
  },

  loadFromDefinition: (definition) => {
    const def = definition as Record<string, unknown>;
    const rawDecisions = (def.decisions ?? []) as Array<Record<string, unknown>>;

    const decisions: DmnDecision[] = rawDecisions.map((d) => {
      // Engine format has decisionTable wrapper; UI format has flat inputColumns/outputColumns
      const dt = (d.decisionTable as Record<string, unknown>) ?? d;
      const inputClauses = (dt.inputClauses ?? dt.inputColumns ?? []) as Array<Record<string, unknown> | string>;
      const outputClauses = (dt.outputClauses ?? dt.outputColumns ?? []) as Array<Record<string, unknown> | string>;
      const rawRules = (dt.rules ?? []) as Array<Record<string, unknown>>;
      const hitPolicy = ((dt.hitPolicy ?? d.hitPolicy) as string) || 'UNIQUE';

      // Build input columns
      const inputColumns: DmnInputColumn[] = inputClauses.map((c) => {
        if (typeof c === 'string') {
          const id = generateId('input');
          return { id, label: c, expression: c, typeRef: 'string' };
        }
        return {
          id: (c.id as string) || generateId('input'),
          label: (c.label as string) || (c.name as string) || '',
          expression: (c.expression as string) || '',
          typeRef: (c.typeRef as string) || 'string',
        };
      });

      // Build output columns
      const outputColumns: DmnOutputColumn[] = outputClauses.map((c) => {
        if (typeof c === 'string') {
          const id = generateId('output');
          return { id, label: c, name: c, typeRef: 'string' };
        }
        return {
          id: (c.id as string) || generateId('output'),
          label: (c.label as string) || (c.name as string) || '',
          name: (c.name as string) || '',
          typeRef: (c.typeRef as string) || 'string',
        };
      });

      // Build rules - engine format uses arrays, UI format uses maps
      const rules: DmnRule[] = rawRules.map((r) => {
        const rawInputEntries = r.inputEntries;
        const rawOutputEntries = r.outputEntries;

        let inputEntries: Record<string, string>;
        let outputEntries: Record<string, string>;

        if (Array.isArray(rawInputEntries)) {
          // Engine format: array of strings → map keyed by column id
          inputEntries = {};
          (rawInputEntries as string[]).forEach((val, idx) => {
            if (idx < inputColumns.length) {
              inputEntries[inputColumns[idx].id] = val;
            }
          });
        } else {
          inputEntries = (rawInputEntries as Record<string, string>) ?? {};
        }

        if (Array.isArray(rawOutputEntries)) {
          outputEntries = {};
          (rawOutputEntries as string[]).forEach((val, idx) => {
            if (idx < outputColumns.length) {
              outputEntries[outputColumns[idx].id] = val;
            }
          });
        } else {
          outputEntries = (rawOutputEntries as Record<string, string>) ?? {};
        }

        return {
          id: (r.id as string) || generateId('rule'),
          inputEntries,
          outputEntries,
          annotation: (r.annotation as string) || '',
        };
      });

      return {
        id: (d.id as string) || generateId('dec'),
        name: (d.name as string) || 'Decision',
        hitPolicy: hitPolicy as DmnHitPolicy,
        inputColumns,
        outputColumns,
        rules,
      };
    });

    set({
      decisions,
      activeDecisionIndex: 0,
    });
  },

  toDefinition: () => {
    const { decisions } = get();
    return {
      decisions: decisions.map((d) => ({
        id: d.id,
        name: d.name,
        decisionTable: {
          hitPolicy: d.hitPolicy,
          inputClauses: d.inputColumns.map((c) => ({
            name: c.label || c.expression,
            expression: c.expression,
          })),
          outputClauses: d.outputColumns.map((c) => ({
            name: c.name || c.label,
          })),
          rules: d.rules.map((r, ri) => ({
            ruleNumber: ri + 1,
            inputEntries: d.inputColumns.map((col) => r.inputEntries[col.id] ?? ''),
            outputEntries: d.outputColumns.map((col) => r.outputEntries[col.id] ?? ''),
            annotation: r.annotation || undefined,
          })),
        },
      })),
    };
  },

  reset: () => {
    set(initialState);
  },
}));
