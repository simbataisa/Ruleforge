# Frontend State Management Patterns

## State Taxonomy

### Server State
Data sourced from backend APIs; shared across application; requires synchronization.

**Characteristics:**
- Originates on server
- Must stay synchronized with server
- Often shared across multiple routes/components
- Requires cache invalidation strategy
- May have stale/fresh concepts

**Examples:**
- User profile and authentication data
- List of invoices with pagination
- Product catalog with filters/search
- Comments on a post
- Real-time notifications

**Tools:** React Query / TanStack Query (preferred), SWR, Axios + custom hooks

### Client State
Application-level state derived from or computed from server state; specific to user session.

**Characteristics:**
- Computed from other state
- Specific to current session/user
- Survives page navigation/refresh (optional)
- Often synchronized with local storage
- Affects application behavior globally

**Examples:**
- User preferences (dark mode, language, theme)
- Application-wide filters (selected category, date range)
- Sidebar collapse state
- Selected view mode (list vs grid)

**Tools:** Zustand, Redux, Context API (if small)

### UI State
Temporary, component-specific state controlling visual presentation.

**Characteristics:**
- Temporary (doesn't persist)
- Component-specific
- Not shared across components
- Includes animation state, hover state, expanded state
- Performance-critical (frequent updates)

**Examples:**
- Modal open/closed state
- Dropdown expanded/collapsed
- Form input focused state
- Animated element position
- Temporary hover states

**Tools:** React.useState (local component state)

### Form State
Input values, validation state, and submission state for forms.

**Characteristics:**
- Input values and their changes
- Validation errors and status
- Dirty/touched tracking
- Form-level errors
- Submission state (pending, success, error)

**Examples:**
- Login form (email, password inputs)
- Checkout form (address, payment info)
- Settings form (preferences)
- Search form (query, filters)

**Tools:** React Hook Form (preferred), Formik, custom useState

---

## Decision Guide: Where State Lives

```
┌───────────────────────────────┐
│ What type of state is this?   │
└────────┬──────────────────────┘
         │
    ┌────┴────────────────────┬──────────────────┬──────────┐
    │                         │                  │          │
    ▼                         ▼                  ▼          ▼
Server State?          Client State?         UI State?    Form State?
    │                         │                  │          │
    ▼                         ▼                  ▼          ▼
    │                         │                  │          │
    ├─ Shared across       ├─ Persistent?  ├─ Local?    ├─ Simple form?
    │  components?         │                │            │
    │                      ▼                ▼            ▼
    ▼                      │                │            │
React Query        ┌───────┴──────┐    ▼ useState   ├─ React Hook Form
  ↓ Cache ↓        │              │                 │    ↓ Complex/Async
Sync across        │ Yes → Zustand │ Use useState   ├─ React Hook Form
multiple routes    │    + localStorage              │    with Zod
                   │              │                 │
                   │ No → Zustand │                 ├─ Formik
                   │    (session)  │                 │
                   │              │                 ├─ Custom hook
                   └──────────────┘                 │
                                                   └─ (Avoid: Context)
```

### Decision Matrix

| State Type | Primary Tool | Secondary | Avoid | Reasoning |
|---|---|---|---|---|
| **Server** | React Query | SWR | Zustand | Caching, sync, stale-while-revalidate patterns |
| **Client (Persistent)** | Zustand + localStorage | Redux | Context | Performance, easy persistence, devtools |
| **Client (Session)** | Zustand | Context | Redux | Simpler than Redux, good for session |
| **UI State** | useState | Context (if parent needed) | Zustand | Frequent updates, local-only, simplicity |
| **Form State** | React Hook Form | Formik | Zustand | Performance, validation, standard patterns |

---

## React Query / TanStack Query Patterns

### Query Keys Convention

**Format:** Array representing hierarchy and dependencies

```typescript
// Pattern: [resource, id?, params]
const queryKeys = {
  // User
  users: () => ['users'],
  userById: (id: string) => ['users', id],
  userProfile: (userId: string) => ['users', userId, 'profile'],
  userPreferences: (userId: string) => ['users', userId, 'preferences'],

  // Invoices
  invoices: () => ['invoices'],
  invoicesByUser: (userId: string) => ['invoices', { userId }],
  invoiceDetail: (invoiceId: string) => ['invoices', invoiceId],
  invoicesByDate: (userId: string, dateRange: DateRange) =>
    ['invoices', { userId, dateRange }],

  // Search/Filtered
  invoicesFiltered: (filters: InvoiceFilters) =>
    ['invoices', { ...filters }],
};

// Usage
const { data } = useQuery({
  queryKey: queryKeys.userById(userId),
  queryFn: () => apiClient.getUser(userId),
});
```

**Best Practices:**
- Hierarchical: parent key should be prefix of child
- Immutable: arrays for serializable structure
- Include all dependencies (userId, filters, etc.)
- Avoid functions in keys (only primitives)

### Stale Time Configuration

**Default:** 0 (immediately stale)

| Scenario | Stale Time | Reasoning |
|---|---|---|
| **Static content** (user profile) | 5-10 minutes | Unlikely to change frequently |
| **Semi-dynamic** (article, invoice) | 1-2 minutes | Updates possible but not constant |
| **Dynamic list** (messages, notifications) | 10-30 seconds | Frequent updates expected |
| **Real-time** (live chat, stocks) | 0 (disable caching) | Must always be fresh |
| **Search results** | 0-5 minutes | Depends on user behavior |

```typescript
const { data: user } = useQuery({
  queryKey: queryKeys.userById(userId),
  queryFn: () => apiClient.getUser(userId),
  staleTime: 1000 * 60 * 5, // 5 minutes
  gcTime: 1000 * 60 * 10, // 10 minutes (formerly cacheTime)
});
```

### Optimistic Updates Pattern

Immediately update UI before server response; rollback on error.

```typescript
const queryClient = useQueryClient();

const updateInvoiceMutation = useMutation({
  mutationFn: (invoice: Invoice) => apiClient.updateInvoice(invoice),

  // Optimistic update: update cache before server response
  onMutate: async (newInvoice: Invoice) => {
    // Cancel outgoing queries
    await queryClient.cancelQueries({
      queryKey: queryKeys.invoiceDetail(newInvoice.id),
    });

    // Save previous value for rollback
    const previousInvoice = queryClient.getQueryData(
      queryKeys.invoiceDetail(newInvoice.id)
    );

    // Optimistically update cache
    queryClient.setQueryData(
      queryKeys.invoiceDetail(newInvoice.id),
      newInvoice
    );

    // Return context for rollback in onError
    return { previousInvoice, newInvoice };
  },

  // Server confirmed update
  onSuccess: (updatedInvoice) => {
    queryClient.setQueryData(
      queryKeys.invoiceDetail(updatedInvoice.id),
      updatedInvoice
    );
  },

  // Rollback on error
  onError: (error, newInvoice, context) => {
    if (context?.previousInvoice) {
      queryClient.setQueryData(
        queryKeys.invoiceDetail(newInvoice.id),
        context.previousInvoice
      );
    }
  },

  // Always refetch fresh data
  onSettled: (data, error, newInvoice) => {
    queryClient.invalidateQueries({
      queryKey: queryKeys.invoiceDetail(newInvoice.id),
    });
  },
});

// Usage
const handleSave = async (invoice: Invoice) => {
  try {
    await updateInvoiceMutation.mutateAsync(invoice);
    toast.success('Invoice saved');
  } catch (error) {
    toast.error('Failed to save invoice');
  }
};
```

### Cache Invalidation Strategies

```typescript
// 1. Invalidate single query
queryClient.invalidateQueries({
  queryKey: queryKeys.invoiceDetail(invoiceId),
});

// 2. Invalidate all queries matching pattern
queryClient.invalidateQueries({
  queryKey: queryKeys.invoices(),
});

// 3. Invalidate by predicate
queryClient.invalidateQueries({
  predicate: (query) =>
    query.queryKey[0] === 'invoices' &&
    query.queryKey[1]?.userId === currentUserId,
});

// 4. Refetch immediately (not just mark stale)
queryClient.refetchQueries({
  queryKey: queryKeys.invoices(),
});

// 5. Clear entire cache
queryClient.clear();
```

### Prefetching Pattern

Load data before user navigates to route.

```typescript
const prefetchUser = (userId: string) => {
  queryClient.prefetchQuery({
    queryKey: queryKeys.userById(userId),
    queryFn: () => apiClient.getUser(userId),
    staleTime: 1000 * 60 * 5,
  });
};

// In router
<Link
  to={`/users/${userId}`}
  onMouseEnter={() => prefetchUser(userId)}
>
  View User
</Link>

// Or in component
useEffect(() => {
  prefetchUser(userId);
}, [userId]);
```

---

## Zustand Patterns

### Store Structure

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// Define state type
interface UserPreferences {
  theme: 'light' | 'dark';
  language: 'en' | 'es' | 'fr';
  sidebarCollapsed: boolean;
  selectedCategory: string | null;
}

// Define store actions
interface UserPreferencesActions {
  setTheme: (theme: 'light' | 'dark') => void;
  setLanguage: (language: 'en' | 'es' | 'fr') => void;
  toggleSidebar: () => void;
  setSelectedCategory: (category: string | null) => void;
  reset: () => void;
}

// Create store with persistence
export const useUserPreferences = create<
  UserPreferences & UserPreferencesActions
>()(
  persist(
    (set) => ({
      // Initial state
      theme: 'light',
      language: 'en',
      sidebarCollapsed: false,
      selectedCategory: null,

      // Actions
      setTheme: (theme) => set({ theme }),
      setLanguage: (language) => set({ language }),
      toggleSidebar: () =>
        set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
      setSelectedCategory: (selectedCategory) => set({ selectedCategory }),
      reset: () =>
        set({
          theme: 'light',
          language: 'en',
          sidebarCollapsed: false,
          selectedCategory: null,
        }),
    }),
    {
      name: 'user-preferences', // localStorage key
      version: 1, // Migration version
      migrate: (persistedState: any, version: number) => {
        if (version < 1) {
          // Migration logic if needed
        }
        return persistedState as UserPreferences & UserPreferencesActions;
      },
    }
  )
);

// Usage
function App() {
  const { theme, setTheme } = useUserPreferences();
  return <div className={theme}>...</div>;
}
```

### Slice Pattern (Modular Stores)

Split large stores into slices for better organization.

```typescript
// slices/userSlice.ts
export interface UserSlice {
  user: User | null;
  isLoading: boolean;
  error: string | null;
  setUser: (user: User) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

export const createUserSlice = (set: SetState<any>): UserSlice => ({
  user: null,
  isLoading: false,
  error: null,
  setUser: (user) => set({ user }),
  setLoading: (isLoading) => set({ isLoading }),
  setError: (error) => set({ error }),
});

// slices/themeSlice.ts
export interface ThemeSlice {
  theme: 'light' | 'dark';
  setTheme: (theme: 'light' | 'dark') => void;
}

export const createThemeSlice = (set: SetState<any>): ThemeSlice => ({
  theme: 'light',
  setTheme: (theme) => set({ theme }),
});

// store.ts (combine slices)
import { create, SetState } from 'zustand';
import { createUserSlice, UserSlice } from './slices/userSlice';
import { createThemeSlice, ThemeSlice } from './slices/themeSlice';

type Store = UserSlice & ThemeSlice;

export const useStore = create<Store>()((set) => ({
  ...createUserSlice(set),
  ...createThemeSlice(set),
}));
```

### DevTools Integration

```typescript
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

export const useStore = create<Store>()(
  devtools((set) => ({
    // ... state and actions
  }), { name: 'MyStore' })
);

// In browser: Install Redux DevTools extension
// Can inspect actions, time-travel debug, export/import state
```

### Selector Pattern (Performance)

Memoize selectors to prevent unnecessary re-renders.

```typescript
// Bad: creates new object every render
const { user, theme } = useStore(); // Re-renders on any store change

// Good: only re-render if selected value changes
const user = useStore((state) => state.user);
const theme = useStore((state) => state.theme);

// Better: complex selectors with memoization
const userDisplayName = useStore(
  (state) => `${state.user?.firstName} ${state.user?.lastName}`,
  (a, b) => a === b // Shallow equality check
);

// Or use shallowEqual for objects
import { shallowEqual } from 'zustand/react/shallow';

const { user, preferences } = useStore(
  (state) => ({
    user: state.user,
    preferences: state.preferences,
  }),
  shallowEqual
);
```

---

## Form State Patterns

### React Hook Form + Zod Validation

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

// Define validation schema
const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Invalid email format'),
  password: z
    .string()
    .min(1, 'Password is required')
    .min(8, 'Password must be at least 8 characters'),
  rememberMe: z.boolean().default(false),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting, isDirty, isValid },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur', // Validate on blur for better UX
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await apiClient.login(data);
      // Success handling
    } catch (error) {
      // Error handling
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <div>
        <input
          {...register('email')}
          placeholder="Email"
          aria-invalid={!!errors.email}
          aria-describedby={errors.email ? 'email-error' : undefined}
        />
        {errors.email && (
          <span id="email-error" role="alert">
            {errors.email.message}
          </span>
        )}
      </div>

      <div>
        <input
          {...register('password')}
          type="password"
          placeholder="Password"
          aria-invalid={!!errors.password}
          aria-describedby={errors.password ? 'password-error' : undefined}
        />
        {errors.password && (
          <span id="password-error" role="alert">
            {errors.password.message}
          </span>
        )}
      </div>

      <label>
        <input {...register('rememberMe')} type="checkbox" />
        Remember me
      </label>

      <button
        type="submit"
        disabled={isSubmitting || !isDirty}
        aria-busy={isSubmitting}
      >
        {isSubmitting ? 'Logging in...' : 'Login'}
      </button>
    </form>
  );
}
```

---

## Global UI State Patterns

### Modals / Dialogs

```typescript
// modalStore.ts
interface ModalState {
  modals: Record<string, boolean>;
  openModal: (id: string) => void;
  closeModal: (id: string) => void;
  closeAll: () => void;
}

export const useModalStore = create<ModalState>((set) => ({
  modals: {},
  openModal: (id) =>
    set((state) => ({
      modals: { ...state.modals, [id]: true },
    })),
  closeModal: (id) =>
    set((state) => ({
      modals: { ...state.modals, [id]: false },
    })),
  closeAll: () => set({ modals: {} }),
}));

// Usage
function MyComponent() {
  const { modals, openModal, closeModal } = useModalStore();

  return (
    <>
      <button onClick={() => openModal('confirm-delete')}>Delete</button>
      {modals['confirm-delete'] && (
        <ConfirmDialog
          onConfirm={() => {
            handleDelete();
            closeModal('confirm-delete');
          }}
          onCancel={() => closeModal('confirm-delete')}
        />
      )}
    </>
  );
}
```

### Toasts/Notifications

```typescript
// toastStore.ts
interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
}

interface ToastStore {
  toasts: Toast[];
  addToast: (toast: Omit<Toast, 'id'>) => void;
  removeToast: (id: string) => void;
}

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  addToast: (toast) =>
    set((state) => ({
      toasts: [...state.toasts, { ...toast, id: crypto.randomUUID() }],
    })),
  removeToast: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    })),
}));

// Toast hook
export function useToast() {
  const { addToast } = useToastStore();
  return {
    success: (message: string) =>
      addToast({ message, type: 'success', duration: 3000 }),
    error: (message: string) =>
      addToast({ message, type: 'error', duration: 5000 }),
    warning: (message: string) =>
      addToast({ message, type: 'warning', duration: 4000 }),
    info: (message: string) =>
      addToast({ message, type: 'info', duration: 3000 }),
  };
}

// Usage
function Component() {
  const toast = useToast();
  const handleSave = async () => {
    try {
      await apiClient.save();
      toast.success('Saved successfully');
    } catch {
      toast.error('Failed to save');
    }
  };
}
```

---

## Performance Optimization

### Avoiding Unnecessary Re-renders

**Problem:** React Context causes all consumers to re-render when any value changes.

```typescript
// Bad: Single context causes over-rendering
const AppContext = createContext<AppState | undefined>(undefined);

function AppProvider({ children }) {
  const [state, setState] = useState<AppState>(initialState);
  // Any change causes all consumers to re-render
  return <AppContext.Provider value={state}>{children}</AppContext.Provider>;
}

// Good: Split contexts by frequency of change
const UserContext = createContext<User | undefined>(undefined);
const PreferencesContext = createContext<Preferences | undefined>(
  undefined
);

function AppProvider({ children }) {
  const [user, setUser] = useState<User | null>(null); // Rarely changes
  const [preferences, setPreferences] = useState<Preferences>(); // Frequently changes

  return (
    <UserContext.Provider value={user}>
      <PreferencesContext.Provider value={preferences}>
        {children}
      </PreferencesContext.Provider>
    </UserContext.Provider>
  );
}
```

### useMemo / useCallback Guidelines

**Use `useMemo` when:**
- Computing expensive value (sorting large list, complex calculation)
- Value is passed as prop to memoized child component
- Object/array comparison is crucial

**Don't use `useMemo` when:**
- Computing simple values (string concatenation, simple math)
- Dependencies change frequently (defeats purpose)
- Primitive values (numbers, strings, booleans)

```typescript
// Bad: Over-memoizing primitive
const count = useMemo(() => list.length, [list]);

// Good: Only memoize when necessary
const sortedList = useMemo(
  () => expensiveSort([...list]),
  [list]
);

// Bad: Dependency array too loose
const callback = useCallback(() => {
  handleClick(id, status); // Missing dependency 'status'
}, [id]); // Will cause stale closure!

// Good: Include all dependencies
const callback = useCallback(() => {
  handleClick(id, status);
}, [id, status]);
```

### React Query Caching Best Practices

```typescript
// Good: Specific cache keys enable fine-grained invalidation
const { data: user } = useQuery({
  queryKey: ['users', userId],
  queryFn: () => apiClient.getUser(userId),
});

const { data: posts } = useQuery({
  queryKey: ['users', userId, 'posts'],
  queryFn: () => apiClient.getUserPosts(userId),
});

// When user updates, only invalidate user, not posts
queryClient.invalidateQueries({
  queryKey: ['users', userId],
  exact: true, // Only exact match, not prefix
});

// Bad: Overly broad cache key causes excessive invalidations
const { data: everything } = useQuery({
  queryKey: ['data'],
  queryFn: () => apiClient.getAllData(),
}); // Invalidating any data causes refetch
```

---

## Server State Synchronization

### Polling vs WebSocket vs SSE

| Mechanism | Latency | Efficiency | Use Case | Implementation |
|---|---|---|---|---|
| **Polling** | 5-30s | Poor (many empty checks) | Low-priority updates | `setInterval` or React Query `refetchInterval` |
| **WebSocket** | < 100ms | Good (bidirectional) | Real-time (chat, live data) | Socket.io, native WebSocket |
| **SSE** | < 100ms | Good (server pushes) | Server → client updates | EventSource API |

```typescript
// Polling pattern
const { data } = useQuery({
  queryKey: ['notifications'],
  queryFn: () => apiClient.getNotifications(),
  refetchInterval: 5000, // Poll every 5 seconds
  refetchIntervalInBackground: false, // Stop polling when tab hidden
});

// WebSocket pattern
useEffect(() => {
  const socket = io('http://api.example.com');

  socket.on('message', (data) => {
    // Update cache when server sends update
    queryClient.setQueryData(['messages'], (old) => [...old, data]);
  });

  return () => socket.disconnect();
}, []);

// SSE pattern
useEffect(() => {
  const eventSource = new EventSource('/api/stream');

  eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    queryClient.setQueryData(['live-data'], data);
  };

  return () => eventSource.close();
}, []);
```

### Optimistic UI Rollback

Already covered in React Query section; pattern applies to any mutation.

```typescript
// On error, rollback UI to previous state
try {
  optimisticUpdate(newData); // Update UI immediately
  await apiClient.update(newData); // Send to server
} catch (error) {
  rollbackUpdate(previousData); // Revert on error
  showError('Failed to update');
}
```
