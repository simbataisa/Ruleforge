import { useNotificationStore, type Notification } from '@/stores/notification.store';
import {
  FileEdit,
  Trash2,
  Play,
  UserPlus,
  Info,
} from 'lucide-react';

function timeAgo(timestamp: string): string {
  const seconds = Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function eventIcon(type: string) {
  const lower = type.toLowerCase();
  if (lower.includes('create') || lower.includes('created')) return <FileEdit className="h-4 w-4 text-green-500" />;
  if (lower.includes('delete') || lower.includes('deleted')) return <Trash2 className="h-4 w-4 text-red-500" />;
  if (lower.includes('execut') || lower.includes('solve')) return <Play className="h-4 w-4 text-blue-500" />;
  if (lower.includes('user') || lower.includes('login')) return <UserPlus className="h-4 w-4 text-purple-500" />;
  return <Info className="h-4 w-4 text-gray-400" />;
}

function NotificationItem({ notification }: { notification: Notification }) {
  const markAsRead = useNotificationStore((s) => s.markAsRead);

  return (
    <button
      onClick={() => markAsRead(notification.id)}
      className="flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-gray-50 transition-colors"
    >
      <div className="mt-0.5 shrink-0">{eventIcon(notification.type)}</div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-gray-900 truncate">{notification.title}</p>
        <p className="text-xs text-gray-500 truncate">{notification.message}</p>
        <p className="text-xs text-gray-400 mt-0.5">{timeAgo(notification.timestamp)}</p>
      </div>
      {!notification.read && (
        <div className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-indigo-500" />
      )}
    </button>
  );
}

export function NotificationDropdown() {
  const notifications = useNotificationStore((s) => s.notifications);
  const markAllAsRead = useNotificationStore((s) => s.markAllAsRead);

  return (
    <div className="absolute right-0 top-full mt-2 w-80 max-h-96 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg z-50">
      <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <h3 className="text-sm font-semibold text-gray-900">Notifications</h3>
        {notifications.length > 0 && (
          <button
            onClick={markAllAsRead}
            className="text-xs text-indigo-600 hover:text-indigo-700"
          >
            Mark all read
          </button>
        )}
      </div>

      <div className="max-h-80 overflow-y-auto divide-y divide-gray-100">
        {notifications.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm text-gray-400">
            No notifications
          </div>
        ) : (
          notifications.map((n) => <NotificationItem key={n.id} notification={n} />)
        )}
      </div>
    </div>
  );
}
