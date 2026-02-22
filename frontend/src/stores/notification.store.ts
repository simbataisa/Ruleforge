import { create } from 'zustand';

export interface Notification {
  id: string;
  type: string;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
}

interface NotificationState {
  notifications: Notification[];
  unreadCount: number;
  addNotification: (n: Omit<Notification, 'id' | 'read'>) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  clearAll: () => void;
  connect: () => void;
  disconnect: () => void;
}

const MAX_NOTIFICATIONS = 50;

let eventSource: EventSource | null = null;
let pollingInterval: ReturnType<typeof setInterval> | null = null;
let notifCounter = 0;

function formatEventTitle(eventType: string): string {
  return eventType
    .replace(/\./g, ' ')
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

function formatEventMessage(eventType: string, resourceType: string, resourceId: string): string {
  if (resourceType && resourceId) {
    return `${resourceType} ${resourceId}`;
  }
  if (resourceType) {
    return resourceType;
  }
  return eventType;
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  unreadCount: 0,

  addNotification: (n) => {
    const id = `notif-${++notifCounter}`;
    set((state) => {
      const updated = [{ ...n, id, read: false }, ...state.notifications].slice(0, MAX_NOTIFICATIONS);
      return {
        notifications: updated,
        unreadCount: updated.filter((x) => !x.read).length,
      };
    });
  },

  markAsRead: (id) =>
    set((state) => {
      const notifications = state.notifications.map((n) =>
        n.id === id ? { ...n, read: true } : n,
      );
      return {
        notifications,
        unreadCount: notifications.filter((x) => !x.read).length,
      };
    }),

  markAllAsRead: () =>
    set((state) => ({
      notifications: state.notifications.map((n) => ({ ...n, read: true })),
      unreadCount: 0,
    })),

  clearAll: () => set({ notifications: [], unreadCount: 0 }),

  connect: () => {
    const { disconnect } = get();
    disconnect();

    const token = localStorage.getItem('ruleforge_token');
    if (!token) return;

    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/v1';

    // Try SSE first
    try {
      const url = `${baseUrl}/events/stream?token=${encodeURIComponent(token)}`;
      eventSource = new EventSource(url);

      eventSource.addEventListener('notification', (e: MessageEvent) => {
        try {
          const data = JSON.parse(e.data);
          get().addNotification({
            type: data.eventType || 'info',
            title: formatEventTitle(data.eventType || 'Event'),
            message: formatEventMessage(data.eventType || '', data.resourceType || '', data.resourceId || ''),
            timestamp: data.timestamp || new Date().toISOString(),
          });
        } catch {
          // ignore parse errors
        }
      });

      eventSource.addEventListener('connected', () => {
        // SSE connected successfully; clear any fallback polling
        if (pollingInterval) {
          clearInterval(pollingInterval);
          pollingInterval = null;
        }
      });

      eventSource.onerror = () => {
        // SSE failed; fall back to polling
        if (eventSource) {
          eventSource.close();
          eventSource = null;
        }
        startPolling(baseUrl, token);
      };
    } catch {
      // EventSource not supported or failed; fall back to polling
      startPolling(baseUrl, token);
    }
  },

  disconnect: () => {
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
    if (pollingInterval) {
      clearInterval(pollingInterval);
      pollingInterval = null;
    }
  },
}));

function startPolling(baseUrl: string, token: string) {
  if (pollingInterval) return;

  const poll = async () => {
    try {
      const res = await fetch(`${baseUrl}/audit?size=5`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return;
      const json = await res.json();
      const items = json?.data?.content;
      if (!Array.isArray(items)) return;

      const { notifications, addNotification } = useNotificationStore.getState();
      const existingTimestamps = new Set(notifications.map((n) => n.timestamp));

      for (const item of items) {
        const ts = item.timestamp;
        if (ts && !existingTimestamps.has(ts)) {
          addNotification({
            type: item.eventType || 'info',
            title: formatEventTitle(item.eventType || 'Event'),
            message: formatEventMessage(item.eventType || '', item.resourceType || '', item.resourceId || ''),
            timestamp: ts,
          });
        }
      }
    } catch {
      // ignore polling errors
    }
  };

  pollingInterval = setInterval(poll, 30000);
}
