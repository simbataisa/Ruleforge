import { useState } from 'react';
import { Copy, Check, RefreshCw, Shield, Clock, Hash } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';
import { useRotateSecret } from '@/api/webhooks.api';
import { useToastStore } from '@/stores/toast.store';

interface Props {
  webhookId: string;
  webhookUrl: string;
}

export function WebhookSecurityTab({ webhookId }: Props) {
  const addToast = useToastStore((s) => s.addToast);
  const rotateSecret = useRotateSecret();

  const [showConfirm, setShowConfirm] = useState(false);
  const [newSecret, setNewSecret] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const handleRotate = () => {
    rotateSecret.mutate(webhookId, {
      onSuccess: (result) => {
        setShowConfirm(false);
        setNewSecret(result.secret);
      },
      onError: () => {
        setShowConfirm(false);
        addToast({ type: 'error', title: 'Failed to rotate secret' });
      },
    });
  };

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text)
      .then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      })
      .catch(() => {
        addToast({ type: 'error', title: 'Copy failed', message: 'Please copy the secret manually.' });
      });
  };

  return (
    <div className="space-y-6 max-w-2xl">
      {/* Signing Secret */}
      <div className="rounded-lg border border-gray-200 p-5">
        <div className="flex items-center gap-2 mb-3">
          <Shield className="h-5 w-5 text-indigo-500" />
          <h3 className="text-sm font-semibold text-gray-900">Signing Secret</h3>
        </div>

        {newSecret ? (
          <div className="space-y-3">
            <div className="rounded-lg bg-amber-50 border border-amber-200 p-3">
              <p className="text-xs font-semibold text-amber-800 mb-2">
                New secret generated — copy it now, it will not be shown again.
              </p>
              <div className="flex items-center gap-2">
                <code className="flex-1 bg-white border border-amber-200 rounded px-3 py-2 text-xs font-mono text-gray-900 break-all">
                  {newSecret}
                </code>
                <button
                  onClick={() => handleCopy(newSecret)}
                  className="flex-shrink-0 p-2 rounded-md bg-amber-100 hover:bg-amber-200 text-amber-800 transition-colors"
                  aria-label="Copy secret"
                >
                  {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                </button>
              </div>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setNewSecret(null)}
            >
              Dismiss
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-gray-600">
              Your webhook secret is stored securely. It was shown once at creation time and cannot be retrieved again.
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowConfirm(true)}
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Rotate Secret
            </Button>
          </div>
        )}
      </div>

      {/* Signing Algorithm */}
      <div className="rounded-lg border border-gray-200 p-5">
        <div className="flex items-center gap-2 mb-3">
          <Hash className="h-5 w-5 text-indigo-500" />
          <h3 className="text-sm font-semibold text-gray-900">Signing Algorithm</h3>
        </div>
        <div className="space-y-3 text-sm text-gray-600">
          <div className="grid grid-cols-[auto,1fr] gap-x-4 gap-y-2">
            <span className="font-medium text-gray-700">Algorithm</span>
            <code className="font-mono text-gray-900">HMAC-SHA256</code>

            <span className="font-medium text-gray-700">Header</span>
            <code className="font-mono text-gray-900">X-Webhook-Signature: sha256={'{'}{'{'}hex{'}'}{'}'}
            </code>

            <span className="font-medium text-gray-700">Signing input</span>
            <code className="font-mono text-gray-900">{'{'}timestamp{'}'}.{'{'}delivery-id{'}'}.{'{'}payload{'}'}</code>
          </div>
          <div className="rounded-md bg-gray-50 border border-gray-200 p-3 text-xs font-mono text-gray-700">
            <p className="mb-1 text-gray-500">// Verification example (Node.js)</p>
            <p>const sig = crypto.createHmac(&apos;sha256&apos;, secret)</p>
            <p className="pl-4">.update(`${'{'}timestamp{'}'}.${'{'}deliveryId{'}'}.${'{'}rawBody{'}'}`)</p>
            <p className="pl-4">.digest(&apos;hex&apos;);</p>
            <p>assert.strictEqual(sig, receivedSig);</p>
          </div>
        </div>
      </div>

      {/* Replay Protection */}
      <div className="rounded-lg border border-gray-200 p-5">
        <div className="flex items-center gap-2 mb-3">
          <Clock className="h-5 w-5 text-indigo-500" />
          <h3 className="text-sm font-semibold text-gray-900">Replay Protection</h3>
        </div>
        <div className="space-y-2 text-sm text-gray-600">
          <p>
            Each delivery includes{' '}
            <code className="bg-gray-100 rounded px-1 py-0.5 text-xs font-mono text-gray-900">X-Webhook-Timestamp</code>{' '}
            (Unix epoch, seconds). Reject deliveries where the timestamp differs from the current time by more than 5 minutes.
          </p>
          <p>
            <code className="bg-gray-100 rounded px-1 py-0.5 text-xs font-mono text-gray-900">X-Webhook-Delivery-Id</code>{' '}
            is unique per delivery attempt (not per event). Use it to deduplicate retried deliveries on your end.
          </p>
        </div>
      </div>

      <ConfirmDialog
        open={showConfirm}
        onClose={() => setShowConfirm(false)}
        onConfirm={handleRotate}
        title="Rotate Webhook Secret"
        message="This will immediately invalidate the current secret. Any existing integrations using the old secret will stop receiving verified webhooks until updated."
        confirmLabel="Rotate Secret"
        confirmVariant="danger"
        isLoading={rotateSecret.isPending}
      />
    </div>
  );
}
