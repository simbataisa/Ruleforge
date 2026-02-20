import { useState, useCallback } from 'react';

interface ScriptEditorProps {
  scriptBody: string;
  language?: string;
  timeout?: number;
  maxMemoryMb?: number;
  onChange?: (definition: ScriptDefinition) => void;
  readOnly?: boolean;
}

interface ScriptDefinition {
  scriptBody: string;
  language: string;
  timeout: number;
  maxMemoryMb: number;
}

export function ScriptEditor({
  scriptBody = 'function evaluate(input) {\n  // Write your rule logic here\n  return {\n    result: true\n  };\n}',
  language = 'js',
  timeout = 5000,
  maxMemoryMb = 64,
  onChange,
  readOnly = false,
}: ScriptEditorProps) {
  const [code, setCode] = useState(scriptBody);
  const [lang, setLang] = useState(language);
  const [timeoutMs, setTimeoutMs] = useState(timeout);
  const [maxMem, setMaxMem] = useState(maxMemoryMb);

  const handleChange = useCallback((value: string) => {
    setCode(value);
    onChange?.({ scriptBody: value, language: lang, timeout: timeoutMs, maxMemoryMb: maxMem });
  }, [lang, timeoutMs, maxMem, onChange]);

  return (
    <div className="flex flex-col h-full gap-4">
      <div className="flex items-center gap-4 p-3 bg-gray-50 border rounded-lg">
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-700">Language:</label>
          <select
            value={lang}
            onChange={e => setLang(e.target.value)}
            disabled={readOnly}
            className="text-sm border rounded px-2 py-1"
          >
            <option value="js">JavaScript</option>
          </select>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-700">Timeout (ms):</label>
          <input
            type="number"
            value={timeoutMs}
            onChange={e => setTimeoutMs(Number(e.target.value))}
            disabled={readOnly}
            className="text-sm border rounded px-2 py-1 w-24"
            min={100}
            max={30000}
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium text-gray-700">Max Memory (MB):</label>
          <input
            type="number"
            value={maxMem}
            onChange={e => setMaxMem(Number(e.target.value))}
            disabled={readOnly}
            className="text-sm border rounded px-2 py-1 w-20"
            min={16}
            max={512}
          />
        </div>
      </div>

      <div className="flex-1 border rounded-lg overflow-hidden">
        <div className="bg-gray-800 px-4 py-2 text-xs text-gray-400 flex items-center justify-between">
          <span>script.{lang === 'js' ? 'js' : lang}</span>
          <span className="text-yellow-400">GraalVM Sandboxed</span>
        </div>
        <textarea
          value={code}
          onChange={e => handleChange(e.target.value)}
          readOnly={readOnly}
          className="w-full h-full min-h-64 p-4 font-mono text-sm bg-gray-900 text-green-400 resize-none focus:outline-none"
          placeholder="function evaluate(input) { return {}; }"
          spellCheck={false}
        />
      </div>

      <div className="text-xs text-gray-500 p-2 bg-blue-50 border border-blue-200 rounded">
        <strong>Contract:</strong> Define a function <code>evaluate(input)</code> that returns an object with output fields.
        The <code>input</code> parameter contains all working memory facts.
      </div>
    </div>
  );
}

export default ScriptEditor;
