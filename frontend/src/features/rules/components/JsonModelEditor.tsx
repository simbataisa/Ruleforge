import { useRef, useCallback, useEffect } from 'react';
import Editor, { loader } from '@monaco-editor/react';
import type { OnMount } from '@monaco-editor/react';

let themeRegistered = false;
function ensureTheme() {
  if (themeRegistered) return;
  themeRegistered = true;
  loader.init().then((monaco) => {
    monaco.editor.defineTheme('ruleforge-light', {
      base: 'vs',
      inherit: true,
      rules: [
        { token: 'string.key.json', foreground: '7c3aed' },
        { token: 'string.value.json', foreground: '16a34a' },
        { token: 'number', foreground: 'd97706' },
        { token: 'keyword.json', foreground: '2563eb' },
        { token: 'delimiter', foreground: '6b7280' },
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

interface Props {
  value: string;
  onChange?: (v: string) => void;
  readOnly?: boolean;
  height?: string;
}

export function JsonModelEditor({ value, onChange, readOnly = false, height = '100%' }: Props) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const editorRef = useRef<any>(null);
  const isInternalChange = useRef(false);

  const handleMount: OnMount = useCallback((editor) => {
    editorRef.current = editor;
  }, []);

  const handleChange = useCallback(
    (newValue: string | undefined) => {
      if (newValue === undefined || !onChange) return;
      isInternalChange.current = true;
      onChange(newValue);
    },
    [onChange],
  );

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
    <Editor
      height={height}
      defaultLanguage="json"
      value={value}
      onChange={handleChange}
      onMount={handleMount}
      theme="ruleforge-light"
      options={{
        readOnly,
        domReadOnly: readOnly,
        minimap: { enabled: false },
        lineNumbers: 'on',
        wordWrap: 'on',
        formatOnPaste: true,
        scrollBeyondLastLine: false,
        automaticLayout: true,
        tabSize: 2,
        fontSize: 13,
        fontFamily: "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace",
        lineHeight: 1.6,
        padding: { top: 12, bottom: 12 },
        scrollbar: {
          verticalScrollbarSize: 8,
          horizontalScrollbarSize: 8,
        },
        renderLineHighlight: 'gutter',
        overviewRulerBorder: false,
        hideCursorInOverviewRuler: true,
      }}
    />
  );
}
