import { useState, useCallback } from 'react';
import { Play, Copy, Check, AlertCircle, Clock, ChevronDown, ChevronRight, CheckCircle, XCircle } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { useTestRule } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import type { TestResponse } from '@/api/types/rule.types';

interface TestBenchProps {
  ruleId: string;
  version?: number;
}

const SAMPLE_INPUT = JSON.stringify(
  {
    age: 25,
    income: 50000,
    creditScore: 720,
  },
  null,
  2,
);

export function TestBench({ ruleId }: TestBenchProps) {
  const addToast = useToastStore((s) => s.addToast);
  const testRule = useTestRule();

  const [input, setInput] = useState(SAMPLE_INPUT);
  const [result, setResult] = useState<TestResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [logsExpanded, setLogsExpanded] = useState(false);

  const handleRun = useCallback(() => {
    setError(null);
    setResult(null);

    let parsedInput: Record<string, unknown>;
    try {
      parsedInput = JSON.parse(input);
    } catch {
      setError('Invalid JSON input. Please check your input and try again.');
      return;
    }

    testRule.mutate(
      {
        ruleId,
        data: {
          input: parsedInput,
        },
      },
      {
        onSuccess: (response) => {
          setResult(response);
        },
        onError: (err) => {
          const message =
            (err as { response?: { data?: { message?: string } } })?.response
              ?.data?.message ?? 'Rule execution failed. Please try again.';
          setError(message);
          addToast({ type: 'error', title: 'Test execution failed' });
        },
      },
    );
  }, [input, ruleId, testRule, addToast]);

  const handleCopyResult = useCallback(() => {
    if (!result) return;
    navigator.clipboard.writeText(JSON.stringify(result, null, 2));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [result]);

  const isValidJson = useCallback(() => {
    try {
      JSON.parse(input);
      return true;
    } catch {
      return false;
    }
  }, [input]);

  return (
    <div className="flex h-full">
      {/* Left Panel - Input */}
      <div className="flex-1 flex flex-col border-r border-gray-200">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-gray-50">
          <h3 className="text-sm font-semibold text-gray-700">Input</h3>
          <Button
            size="sm"
            onClick={handleRun}
            loading={testRule.isPending}
            disabled={!isValidJson()}
          >
            <Play className="h-3.5 w-3.5" />
            Run Test
          </Button>
        </div>
        <div className="flex-1 relative">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            className="w-full h-full resize-none border-none outline-none p-4 font-mono text-sm leading-relaxed bg-white text-gray-800 focus:ring-0"
            spellCheck={false}
            placeholder='Enter JSON input...\n{\n  "key": "value"\n}'
          />
          {!isValidJson() && input.trim().length > 0 && (
            <div className="absolute bottom-3 left-4 right-4">
              <div className="flex items-center gap-2 text-xs text-red-600 bg-red-50 border border-red-200 rounded px-2 py-1">
                <AlertCircle className="h-3 w-3 flex-shrink-0" />
                Invalid JSON
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Right Panel - Result */}
      <div className="flex-1 flex flex-col">
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 bg-gray-50">
          <h3 className="text-sm font-semibold text-gray-700">Result</h3>
          {result && (
            <button
              onClick={handleCopyResult}
              className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 transition-colors"
            >
              {copied ? (
                <>
                  <Check className="h-3.5 w-3.5 text-green-500" />
                  Copied
                </>
              ) : (
                <>
                  <Copy className="h-3.5 w-3.5" />
                  Copy
                </>
              )}
            </button>
          )}
        </div>

        <div className="flex-1 overflow-auto p-4 space-y-4">
          {/* Error */}
          {error && (
            <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-lg p-4">
              <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
              <div>
                <h4 className="text-sm font-medium text-red-800">
                  Execution Error
                </h4>
                <p className="text-sm text-red-600 mt-1">{error}</p>
              </div>
            </div>
          )}

          {/* No result yet */}
          {!result && !error && !testRule.isPending && (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-2">
              <Play className="h-8 w-8" />
              <p className="text-sm">
                Enter input data and click "Run Test" to execute the rule.
              </p>
            </div>
          )}

          {/* Loading */}
          {testRule.isPending && (
            <div className="flex items-center justify-center h-full text-gray-400 gap-2">
              <div className="animate-spin h-5 w-5 border-2 border-indigo-500 border-t-transparent rounded-full" />
              <p className="text-sm">Executing rule...</p>
            </div>
          )}

          {/* Result */}
          {result && (
            <>
              {/* Status + Metadata */}
              <div className="flex items-center gap-4 text-xs">
                {result.success ? (
                  <Badge className="bg-green-100 text-green-700 flex items-center gap-1">
                    <CheckCircle className="h-3 w-3" />
                    Success
                  </Badge>
                ) : (
                  <Badge className="bg-red-100 text-red-700 flex items-center gap-1">
                    <XCircle className="h-3 w-3" />
                    Failed
                  </Badge>
                )}
                <div className="flex items-center gap-1 text-gray-500">
                  <Clock className="h-3.5 w-3.5" />
                  <span>{result.executionTimeMs}ms</span>
                </div>
              </div>

              {/* Error message from engine */}
              {result.error && (
                <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-lg p-4">
                  <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
                  <div>
                    <h4 className="text-sm font-medium text-red-800">Engine Error</h4>
                    <p className="text-sm text-red-600 mt-1">{result.error}</p>
                  </div>
                </div>
              )}

              {/* Output */}
              <div>
                <h4 className="text-xs font-semibold text-gray-600 uppercase tracking-wide mb-2">
                  Output
                </h4>
                <pre className="bg-gray-900 text-green-400 rounded-lg p-4 text-xs font-mono overflow-auto max-h-64 leading-relaxed">
                  {JSON.stringify(result.output, null, 2)}
                </pre>
              </div>

              {/* Logs */}
              {result.logs && result.logs.length > 0 && (
                <div>
                  <button
                    className="flex items-center gap-1 text-xs font-semibold text-gray-600 uppercase tracking-wide mb-2 hover:text-gray-800"
                    onClick={() => setLogsExpanded(!logsExpanded)}
                  >
                    {logsExpanded ? (
                      <ChevronDown className="h-3.5 w-3.5" />
                    ) : (
                      <ChevronRight className="h-3.5 w-3.5" />
                    )}
                    Execution Logs ({result.logs.length})
                  </button>
                  {logsExpanded && (
                    <div className="bg-gray-50 border border-gray-200 rounded-lg p-3 space-y-1">
                      {result.logs.map((log, i) => (
                        <div key={i} className="text-xs font-mono text-gray-600">
                          {log}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
