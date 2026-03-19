import { useState } from 'react';
import { ChevronRight, X } from 'lucide-react';
import { Input } from '@/shared/components/Input';
import { Badge } from '@/shared/components/Badge';
import { cn } from '@/shared/utils/cn';
import type { WebhookRetryConfig } from '@/api/webhooks.api';

const DEFAULT_RETRY_STATUS_CODES = [429, 408, 500, 502, 503, 504];

interface WebhookRetryConfigFormProps {
  retryConfig: Partial<WebhookRetryConfig>;
  onChange: (retryConfig: Partial<WebhookRetryConfig>) => void;
  disabled?: boolean;
}

export function WebhookRetryConfigForm({ retryConfig, onChange, disabled }: WebhookRetryConfigFormProps) {
  const [open, setOpen] = useState(false);
  const [newCode, setNewCode] = useState('');

  const codes = retryConfig.retryOnStatusCodes ?? DEFAULT_RETRY_STATUS_CODES;

  const update = (patch: Partial<WebhookRetryConfig>) => onChange({ ...retryConfig, ...patch });

  const removeCode = (code: number) =>
    update({ retryOnStatusCodes: codes.filter((c) => c !== code) });

  const addCode = () => {
    const code = parseInt(newCode, 10);
    if (!isNaN(code) && code >= 100 && code <= 599 && !codes.includes(code)) {
      update({ retryOnStatusCodes: [...codes, code] });
      setNewCode('');
    }
  };

  return (
    <div>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 transition-colors"
      >
        <ChevronRight
          className={cn('h-4 w-4 transition-transform', open && 'rotate-90')}
        />
        Advanced
      </button>

      {open && (
        <div className="pt-3 space-y-4">
          <p className="text-xs text-gray-500">
            Retry settings. Defaults work for most endpoints.
          </p>
          <div className="grid grid-cols-3 gap-3">
            <Input
              id="initial-delay"
              label="Initial Delay (sec)"
              type="number"
              min={1}
              max={3600}
              value={retryConfig.initialDelaySeconds ?? 30}
              onChange={(e) => update({ initialDelaySeconds: parseInt(e.target.value, 10) || 30 })}
              disabled={disabled}
            />
            <Input
              id="backoff-multiplier"
              label="Backoff Multiplier"
              type="number"
              min={1.0}
              max={10.0}
              step={0.1}
              value={retryConfig.backoffMultiplier ?? 2.0}
              onChange={(e) => update({ backoffMultiplier: parseFloat(e.target.value) || 2.0 })}
              disabled={disabled}
            />
            <Input
              id="max-attempts"
              label="Max Attempts"
              type="number"
              min={1}
              max={25}
              value={retryConfig.maxAttempts ?? 5}
              onChange={(e) => update({ maxAttempts: parseInt(e.target.value, 10) || 5 })}
              disabled={disabled}
            />
          </div>
          <div>
            <p className="mb-1.5 text-sm font-medium text-gray-700">Retry on Status Codes</p>
            <div className="flex flex-wrap gap-1.5 items-center">
              {codes.map((code) => (
                <Badge key={code} color="gray">
                  <span className="flex items-center gap-1">
                    {code}
                    {!disabled && (
                      <button
                        type="button"
                        onClick={() => removeCode(code)}
                        className="rounded-full hover:bg-gray-200 ml-0.5"
                        aria-label={`Remove ${code}`}
                      >
                        <X className="h-3 w-3" />
                      </button>
                    )}
                  </span>
                </Badge>
              ))}
              {!disabled && (
                <input
                  className="h-6 w-16 rounded border border-gray-300 bg-white px-2 text-xs text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  value={newCode}
                  onChange={(e) => setNewCode(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      addCode();
                    }
                  }}
                  placeholder="+ Add"
                  type="number"
                  min={100}
                  max={599}
                  aria-label="Add status code"
                />
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
