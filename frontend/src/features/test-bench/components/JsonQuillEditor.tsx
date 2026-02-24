import { useRef, useCallback, useEffect } from 'react';
import Editor, { type OnMount, loader } from '@monaco-editor/react';

interface JsonQuillEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

// Define a custom light theme with colorful JSON tokens on a white background
let themeRegistered = false;
function ensureTheme() {
  if (themeRegistered) return;
  themeRegistered = true;
  loader.init().then((monaco) => {
    monaco.editor.defineTheme('ruleforge-light', {
      base: 'vs',
      inherit: true,
      rules: [
        { token: 'string.key.json', foreground: '7c3aed' },      // purple keys
        { token: 'string.value.json', foreground: '16a34a' },     // green strings
        { token: 'number', foreground: 'd97706' },                // amber numbers
        { token: 'keyword.json', foreground: '2563eb' },          // blue true/false/null
        { token: 'delimiter', foreground: '6b7280' },             // gray brackets/commas
      ],
      colors: {
        'editor.background': '#ffffff',
        'editor.foreground': '#1f2937',
        'editor.lineHighlightBackground': '#f9fafb',
        'editorLineNumber.foreground': '#d1d5db',
        'editorLineNumber.activeForeground': '#9ca3af',
        'editor.selectionBackground': '#e0e7ff',
        'editor.inactiveSelectionBackground': '#f1f5f9',
        'editorCursor.foreground': '#6366f1',
        'editorIndentGuide.background': '#f3f4f6',
        'editorIndentGuide.activeBackground': '#e5e7eb',
        'editorBracketMatch.background': '#e0e7ff',
        'editorBracketMatch.border': '#a5b4fc',
      },
    });
  });
}
ensureTheme();

export function JsonQuillEditor({ value, onChange, placeholder }: JsonQuillEditorProps) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const editorRef = useRef<any>(null);
  const isInternalChange = useRef(false);

  const handleEditorDidMount: OnMount = useCallback((editor) => {
    editorRef.current = editor;
  }, []);

  const handleChange = useCallback(
    (newValue: string | undefined) => {
      if (newValue === undefined) return;
      isInternalChange.current = true;
      onChange(newValue);
    },
    [onChange],
  );

  // Sync external value changes (e.g. sample selection) into the editor
  useEffect(() => {
    if (isInternalChange.current) {
      isInternalChange.current = false;
      return;
    }
    const editor = editorRef.current;
    if (editor && editor.getValue() !== value) {
      editor.setValue(value);
    }
  }, [value]);

  return (
    <div className="h-full flex flex-col json-monaco-editor">
      <style>{`
        .json-monaco-editor {
          position: relative;
        }
        .json-monaco-editor .monaco-placeholder {
          position: absolute;
          top: 0;
          left: 64px;
          pointer-events: none;
          color: #9ca3af;
          font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace;
          font-size: 13px;
          line-height: 1.6;
          padding: 0;
          z-index: 1;
          white-space: pre;
        }
      `}</style>
      {!value && placeholder && (
        <div className="monaco-placeholder">{placeholder}</div>
      )}
      <Editor
        height="100%"
        defaultLanguage="json"
        value={value}
        onChange={handleChange}
        onMount={handleEditorDidMount}
        theme="ruleforge-light"
        options={{
          minimap: { enabled: false },
          fontSize: 13,
          fontFamily: "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace",
          lineHeight: 1.6,
          padding: { top: 12, bottom: 12 },
          scrollBeyondLastLine: false,
          automaticLayout: true,
          tabSize: 2,
          wordWrap: 'on',
          formatOnPaste: true,
          formatOnType: true,
          renderLineHighlight: 'gutter',
          scrollbar: {
            verticalScrollbarSize: 8,
            horizontalScrollbarSize: 8,
          },
          overviewRulerBorder: false,
          hideCursorInOverviewRuler: true,
          contextmenu: true,
          suggest: {
            showWords: false,
          },
        }}
      />
    </div>
  );
}
