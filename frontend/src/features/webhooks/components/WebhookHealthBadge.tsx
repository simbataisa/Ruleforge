import { useWebhookMetrics } from '@/api/webhooks.api';

interface WebhookHealthBadgeProps {
  webhookId: string;
}

export function WebhookHealthBadge({ webhookId }: WebhookHealthBadgeProps) {
  const { data: metrics } = useWebhookMetrics(webhookId);

  let icon: React.ReactNode;
  let healthLabel: string;
  let title: string;

  if (!metrics || metrics.totalDeliveries === 0) {
    icon = <span className="text-gray-400 text-lg">–</span>;
    healthLabel = 'No data';
    title = 'No data';
  } else if (metrics.successRate >= 95 && metrics.dlqDepth === 0) {
    icon = <span className="text-green-500 text-lg">●</span>;
    healthLabel = 'Healthy';
    title = `${metrics.successRate.toFixed(1)}% success · DLQ: ${metrics.dlqDepth}`;
  } else if (metrics.successRate >= 80 && metrics.dlqDepth <= 5) {
    icon = <span className="text-yellow-500 text-lg">●</span>;
    healthLabel = 'Degraded';
    title = `${metrics.successRate.toFixed(1)}% success · DLQ: ${metrics.dlqDepth}`;
  } else {
    icon = <span className="text-red-500 text-lg">▲</span>;
    healthLabel = 'Unhealthy';
    title = `${metrics.successRate.toFixed(1)}% success · DLQ: ${metrics.dlqDepth}`;
  }

  return (
    <span
      title={title}
      role="status"
      aria-label={`Health: ${healthLabel}`}
    >
      {icon}
    </span>
  );
}
