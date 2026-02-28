import { CheckCircle, AlertTriangle, Loader2 } from 'lucide-react';

interface Props {
  status: 'valid' | 'invalid' | 'syncing';
}

export function ModelViewStatusBar({ status }: Props) {
  return (
    <div className={`flex items-center gap-2 px-4 py-1.5 text-xs border-t shrink-0 ${
      status === 'invalid'
        ? 'bg-amber-50 border-amber-200 text-amber-700'
        : status === 'syncing'
        ? 'bg-blue-50 border-blue-200 text-blue-600'
        : 'bg-green-50 border-green-200 text-green-700'
    }`}>
      {status === 'invalid' && <AlertTriangle className="h-3.5 w-3.5 shrink-0" />}
      {status === 'syncing' && <Loader2 className="h-3.5 w-3.5 shrink-0 animate-spin" />}
      {status === 'valid' && <CheckCircle className="h-3.5 w-3.5 shrink-0" />}
      <span>
        {status === 'invalid' && 'Invalid JSON — changes not synced to designer'}
        {status === 'syncing' && 'Syncing\u2026'}
        {status === 'valid' && 'Valid JSON \u00b7 Synced to designer'}
      </span>
    </div>
  );
}
