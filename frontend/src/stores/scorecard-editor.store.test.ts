import { describe, it, expect, beforeEach } from 'vitest';
import { useScorecardEditorStore } from './scorecard-editor.store';

function getState() {
  return useScorecardEditorStore.getState();
}

describe('useScorecardEditorStore', () => {
  beforeEach(() => {
    getState().reset();
  });

  describe('initial state', () => {
    it('starts with defaults', () => {
      const state = getState();
      expect(state.baseScore).toBe(0);
      expect(state.outputField).toBe('score');
      expect(state.reasonCodes).toBe(false);
      expect(state.characteristics).toEqual([]);
    });
  });

  describe('config setters', () => {
    it('setBaseScore', () => {
      getState().setBaseScore(500);
      expect(getState().baseScore).toBe(500);
    });

    it('setOutputField', () => {
      getState().setOutputField('creditScore');
      expect(getState().outputField).toBe('creditScore');
    });

    it('setReasonCodes', () => {
      getState().setReasonCodes(true);
      expect(getState().reasonCodes).toBe(true);
    });
  });

  describe('characteristics', () => {
    it('addCharacteristic adds with defaults', () => {
      getState().addCharacteristic();
      const chars = getState().characteristics;
      expect(chars).toHaveLength(1);
      expect(chars[0].name).toBe('Characteristic 1');
      expect(chars[0].dataType).toBe('NUMBER');
      expect(chars[0].weight).toBe(1);
      expect(chars[0].bins).toEqual([]);
    });

    it('removeCharacteristic removes at index', () => {
      getState().addCharacteristic();
      getState().addCharacteristic();
      getState().removeCharacteristic(0);
      expect(getState().characteristics).toHaveLength(1);
      expect(getState().characteristics[0].name).toBe('Characteristic 2');
    });

    it('updateCharacteristic updates properties', () => {
      getState().addCharacteristic();
      getState().updateCharacteristic(0, { name: 'Income', field: 'annual_income', weight: 2.5 });
      const char = getState().characteristics[0];
      expect(char.name).toBe('Income');
      expect(char.field).toBe('annual_income');
      expect(char.weight).toBe(2.5);
    });
  });

  describe('bins', () => {
    beforeEach(() => {
      getState().addCharacteristic();
    });

    it('addBin adds a bin to a characteristic', () => {
      getState().addBin(0);
      expect(getState().characteristics[0].bins).toHaveLength(1);
      const bin = getState().characteristics[0].bins[0];
      expect(bin.label).toBe('');
      expect(bin.operator).toBe('EQ');
      expect(bin.score).toBe(0);
    });

    it('removeBin removes bin at index', () => {
      getState().addBin(0);
      getState().addBin(0);
      getState().removeBin(0, 0);
      expect(getState().characteristics[0].bins).toHaveLength(1);
    });

    it('updateBin updates bin properties', () => {
      getState().addBin(0);
      getState().updateBin(0, 0, { label: 'High', operator: 'GT', value: '50000', score: 20 });
      const bin = getState().characteristics[0].bins[0];
      expect(bin.label).toBe('High');
      expect(bin.operator).toBe('GT');
      expect(bin.value).toBe('50000');
      expect(bin.score).toBe(20);
    });
  });

  describe('loadFromDefinition / toDefinition', () => {
    it('round-trips', () => {
      getState().setBaseScore(300);
      getState().setOutputField('risk');
      getState().setReasonCodes(true);
      getState().addCharacteristic();
      getState().updateCharacteristic(0, { name: 'Age', field: 'age' });
      getState().addBin(0);
      getState().updateBin(0, 0, { label: 'Young', score: 10 });

      const def = getState().toDefinition();
      getState().reset();

      getState().loadFromDefinition(def);
      expect(getState().baseScore).toBe(300);
      expect(getState().outputField).toBe('risk');
      expect(getState().reasonCodes).toBe(true);
      expect(getState().characteristics).toHaveLength(1);
      expect(getState().characteristics[0].name).toBe('Age');
      expect(getState().characteristics[0].bins).toHaveLength(1);
      expect(getState().characteristics[0].bins[0].label).toBe('Young');
    });

    it('handles empty definition', () => {
      getState().loadFromDefinition({});
      expect(getState().baseScore).toBe(0);
      expect(getState().characteristics).toEqual([]);
    });
  });
});
