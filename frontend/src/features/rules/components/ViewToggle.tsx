interface Props {
  mode: 'designer' | 'model';
  onChange: (mode: 'designer' | 'model') => void;
  className?: string;
}

export function ViewToggle({ mode, onChange, className = '' }: Props) {
  return (
    <div className={`inline-flex rounded-md border border-gray-200 overflow-hidden ${className}`}>
      <button
        onClick={() => onChange('designer')}
        className={`px-3 py-1.5 text-xs font-medium transition-colors ${
          mode === 'designer'
            ? 'bg-indigo-100 text-indigo-700'
            : 'bg-white text-gray-500 hover:bg-gray-50'
        }`}
      >
        Designer
      </button>
      <button
        onClick={() => onChange('model')}
        className={`px-3 py-1.5 text-xs font-medium transition-colors border-l border-gray-200 ${
          mode === 'model'
            ? 'bg-indigo-100 text-indigo-700'
            : 'bg-white text-gray-500 hover:bg-gray-50'
        }`}
      >
        Model
      </button>
    </div>
  );
}
