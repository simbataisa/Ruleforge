import { useState, useCallback } from 'react';
import { Play, Copy, Check, AlertCircle, Clock, ChevronDown, ChevronRight } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { useSolveRule } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import type { SolveResponse } from '@/api/types/rule.types';

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

export function TestBench({ ruleId, version }: TestBenchProps) {
  const addToast = useToastStore((s) => s.addToast);
  const solveRule = useSolveRule();

  const [input, setInput] = useState(SAMPLE_INPUT);
  const [result, setResult] = useState<SolveResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [traceExpanded, setTraceExpanded] = useState(false);

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

    solveRule.mutate(
      {
        ruleId,
        version: version!,
        data: {
          data: parsedInput,
          options: { audit: true },
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
  }, [input, ruleId, version, solveRule, addToast]);

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
            loading={solveRule.isPending}
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
          {!result && !error && !solveRule.isPending && (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-2">
              <Play className="h-8 w-8" />
              <p className="text-sm">
                Enter input data and click "Run Test" to execute the rule.
              </p>
            </div>
          )}

          {/* Loading */}
          {solveRule.isPending && (
            <div className="flex items-center justify-center h-full text-gray-400 gap-2">
              <div className="animate-spin h-5 w-5 border-2 border-indigo-500 border-t-transparent rounded-full" />
              <p className="text-sm">Executing rule...</p>
            </div>
          )}

          {/* Result */}
          {result && (
            <>
              {/* Metadata */}
              <div className="flex items-center gap-4 text-xs">
                <div className="flex items-center gap-1 text-gray-500">
                  <Clock className="h-3.5 w-3.5" />
                  <span>{result.executionTimeMs}ms</span>
                </div>
                <Badge className="bg-indigo-100 text-indigo-700">
                  v{result.version}
                </Badge>
                {result.results && (
                  <span className="text-gray-500">
                    {result.results.length} result
                    {result.results.length !== 1 ? 's' : ''} matched
                  </span>
                )}
              </div>

              {/* Output */}
              <div>
                <h4 className="text-xs font-semibold text-gray-600 uppercase tracking-wide mb-2">
                  Output
                </h4>
                <pre className="bg-gray-900 text-green-400 rounded-lg p-4 text-xs font-mono overflow-auto max-h-64 leading-relaxed">
                  {JSON.stringify(result.results, null, 2)}
                </pre>
              </div>

              {/* Matched Rules */}
              {result.results && result.results.length > 0 && (
                <div>
                  <h4 className="text-xs font-semibold text-gray-600 uppercase tracking-wide mb-2">
                    Matched Rules
                  </h4>
                  <div className="space-y-1">
                    {result.results.map((r, i) => (
                      <div
                        key={i}
                        className="flex items-center justify-between bg-gray-50 rounded px-3 py-2 text-sm"
                      >
                        <span className="text-gray-700">
                          Rule {i + 1}
                          {(r as Record<string, unknown>).ruleName
                            ? `: ${(r as Record<string, unknown>).ruleName}`
                            : ''}
                        </span>
                        {(r as Record<string, unknown>).priority !== undefined && (
                          <Badge className="bg-gray-200 text-gray-600">
                            Priority: {String((r as Record<string, unknown>).priority)}
                          </Badge>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Trace */}
              {(result as unknown as Record<string, unknown>).trace && (
                <div>
                  <button
                    className="flex items-center gap-1 text-xs font-semibold text-gray-600 uppercase tracking-wide mb-2 hover:text-gray-800"
                    onClick={() => setTraceExpanded(!traceExpanded)}
                  >
                    {traceExpanded ? (
                      <ChevronDown className="h-3.5 w-3.5" />
                    ) : (
                      <ChevronRight className="h-3.5 w-3.5" />
                    )}
                    Execution Trace
                  </button>
                  {traceExpanded && (
                    <pre className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-xs font-mono overflow-auto max-h-48 text-gray-600 leading-relaxed">
                      {JSON.stringify(
                        (result as unknown as Record<string, unknown>).trace,
                        null,
                        2,
                      )}
                    </pre>
                  )}
                </div>
              )}

              {/* Correlation ID */}
              <div className="text-xs text-gray-400">
                Correlation ID: {result.correlationId}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
