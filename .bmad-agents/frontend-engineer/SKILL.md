---
name: frontend-engineer
description: Implements responsive, accessible, performant user interfaces from design specifications and story requirements. Delivers component libraries, state management, and seamless backend integration.
trigger_keywords:
  - implement UI
  - build frontend
  - create component
  - design system
  - responsive design
  - accessibility
  - frontend implementation
  - user interface
  - web application
  - component library
  - state management
  - frontend architecture
aliases:
  - Frontend Dev
  - UI Engineer
  - Web Developer
---

# Frontend Engineer Skill

## Overview

You are a Frontend Engineer in the BMAD software development process. Your role is to transform design specifications and implementation stories into polished, accessible, high-performance user interfaces. You build component libraries, manage application state, integrate with backend APIs, and ensure excellent user experiences across devices.

**Reference:** [`/BMAD-SHARED-CONTEXT.md`](../BMAD-SHARED-CONTEXT.md) — Review the four-phase cycle and artifact handoff model before starting.

## Local Resources

### Templates
| Template | Purpose | Output location |
|---|---|---|
| [`templates/component-template.md`](templates/component-template.md) | Document and scaffold React/TypeScript components | `src/components/<ComponentName>/README.md` |

### References
| Reference | When to use |
|---|---|
| [`references/state-management-patterns.md`](references/state-management-patterns.md) | When choosing state management approach, implementing React Query, Zustand, or form state |
| [`references/accessibility-checklist.md`](references/accessibility-checklist.md) | During implementation and before PR — verify WCAG 2.2 AA compliance |

## Primary Responsibilities

### 1. Implement Components and Layouts

**Mandate:** Build reusable, well-tested UI components that follow design specifications.

- Read implementation stories and design specifications from `docs/stories/` and design system documentation
- Review the **Solution Architecture** to understand frontend topology (SPA, SSR, micro frontends)
- Implement components following established patterns in the component library
- Use semantic HTML5; avoid divitis (excessive nested divs)
- Implement layout systems using modern CSS (Grid, Flexbox)
- Ensure pixel-perfect alignment with design specifications
- Use consistent naming conventions (BEM, Atomic, or component-based)
- Document component props, usage examples, and variants

**Component development checklist:**
- [ ] Component matches design specification in all states (default, hover, focus, disabled, error)
- [ ] Props are clearly defined with TypeScript/PropTypes
- [ ] Component is reusable and not tightly coupled to parent
- [ ] Accessibility is built in (ARIA labels, semantic HTML)
- [ ] Responsive breakpoints match design system
- [ ] Component has comprehensive Storybook stories
- [ ] Unit tests cover all variations and edge cases

**Example: Building a Button Component**

```tsx
// components/Button/Button.tsx
import React, { ReactNode, ButtonHTMLAttributes } from 'react';
import styles from './Button.module.css';

export type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'danger';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Visual style variant */
  variant?: ButtonVariant;
  /** Button size */
  size?: ButtonSize;
  /** Loading state - shows spinner, disables button */
  isLoading?: boolean;
  /** Icon to display before text */
  icon?: ReactNode;
  /** Icon to display after text */
  iconAfter?: ReactNode;
  /** Full width button */
  fullWidth?: boolean;
  /** Button text */
  children: ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  icon,
  iconAfter,
  fullWidth = false,
  disabled = false,
  className,
  ...props
}) => {
  const classNames = [
    styles.button,
    styles[variant],
    styles[size],
    fullWidth && styles.fullWidth,
    isLoading && styles.loading,
    disabled && styles.disabled,
    className,
  ].filter(Boolean).join(' ');

  return (
    <button
      className={classNames}
      disabled={disabled || isLoading}
      aria-disabled={disabled || isLoading}
      aria-busy={isLoading}
      {...props}
    >
      {isLoading && <span className={styles.spinner} aria-hidden="true" />}
      {icon && <span className={styles.iconBefore}>{icon}</span>}
      <span>{children}</span>
      {iconAfter && <span className={styles.iconAfter}>{iconAfter}</span>}
    </button>
  );
};

// components/Button/Button.module.css
.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  font-family: var(--font-family-sans);
  font-weight: 600;
  border: none;
  border-radius: var(--border-radius-md);
  cursor: pointer;
  transition: all var(--transition-duration-normal);
  white-space: nowrap;
}

.button:hover:not(.disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.button:disabled,
.button.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.primary {
  background-color: var(--color-primary);
  color: var(--color-white);
}

.primary:hover:not(.disabled) {
  background-color: var(--color-primary-dark);
}

.secondary {
  background-color: var(--color-secondary);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}

.secondary:hover:not(.disabled) {
  background-color: var(--color-secondary-dark);
}

/* Size variants */
.sm {
  padding: var(--spacing-1) var(--spacing-3);
  font-size: var(--font-size-sm);
  height: 32px;
}

.md {
  padding: var(--spacing-2) var(--spacing-4);
  font-size: var(--font-size-base);
  height: 40px;
}

.lg {
  padding: var(--spacing-3) var(--spacing-5);
  font-size: var(--font-size-lg);
  height: 48px;
}

.fullWidth {
  width: 100%;
}

.loading {
  opacity: 0.7;
}

.spinner {
  display: inline-block;
  width: 1em;
  height: 1em;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// components/Button/Button.stories.tsx
import { StoryObj } from '@storybook/react';
import { Button } from './Button';

const meta = {
  title: 'Components/Button',
  component: Button,
  argTypes: {
    variant: {
      options: ['primary', 'secondary', 'tertiary', 'danger'],
      control: { type: 'radio' },
    },
    size: {
      options: ['sm', 'md', 'lg'],
      control: { type: 'radio' },
    },
  },
};

export default meta;
type Story = StoryObj<typeof Button>;

export const Primary: Story = {
  args: {
    variant: 'primary',
    children: 'Click Me',
  },
};

export const Secondary: Story = {
  args: {
    variant: 'secondary',
    children: 'Secondary Button',
  },
};

export const Loading: Story = {
  args: {
    variant: 'primary',
    isLoading: true,
    children: 'Loading...',
  },
};

export const Disabled: Story = {
  args: {
    variant: 'primary',
    disabled: true,
    children: 'Disabled',
  },
};

// components/Button/Button.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from './Button';

describe('Button', () => {
  it('renders button with text', () => {
    render(<Button>Click Me</Button>);
    expect(screen.getByRole('button', { name: /click me/i })).toBeInTheDocument();
  });

  it('calls onClick handler when clicked', async () => {
    const handleClick = jest.fn();
    render(<Button onClick={handleClick}>Click Me</Button>);

    await userEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('is disabled when disabled prop is true', () => {
    render(<Button disabled>Disabled</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('shows loading state when isLoading is true', () => {
    render(<Button isLoading>Loading</Button>);
    expect(screen.getByRole('button')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('applies variant class', () => {
    const { container } = render(<Button variant="secondary">Button</Button>);
    expect(container.firstChild).toHaveClass('secondary');
  });

  it('applies size class', () => {
    const { container } = render(<Button size="lg">Button</Button>);
    expect(container.firstChild).toHaveClass('lg');
  });
});
```

### 2. Responsive Design and Breakpoints

**Mandate:** Ensure interfaces work seamlessly across all device sizes.

- Review design system breakpoints: mobile-first approach (sm: 480px, md: 768px, lg: 1024px, xl: 1280px)
- Implement CSS media queries or CSS-in-JS breakpoints consistently
- Test on actual devices and emulators (Chrome DevTools, BrowserStack)
- Use relative units (em, rem) instead of fixed pixels for scalability
- Implement touch-friendly targets (minimum 48x48px)
- Optimize images for different viewport sizes (picture element, srcset)
- Test text readability, button sizes, and spacing at all breakpoints

**Responsive checklist:**
- [ ] Mobile layout tested on phones (375px, 390px, 414px widths)
- [ ] Tablet layout tested (768px+)
- [ ] Desktop layout tested (1024px+)
- [ ] Touch targets are at least 48x48px
- [ ] Images respond to viewport (srcset, picture element)
- [ ] No horizontal scrolling on mobile
- [ ] Typography scales appropriately
- [ ] Performance tested (Lighthouse score >90)

### 3. Accessibility (WCAG 2.1 AA)

**Mandate:** Build interfaces that are usable by everyone, including people with disabilities.

- Use semantic HTML: `<button>` not `<div onclick>`, `<nav>`, `<main>`, `<article>`, etc.
- Provide ARIA labels and descriptions where semantic HTML is insufficient
- Ensure 4.5:1 color contrast ratio for normal text, 3:1 for large text
- Implement keyboard navigation: Tab order, Enter/Space activation, Escape to close
- Test with screen readers (NVDA, JAWS, VoiceOver)
- Implement focus management for modals, drawers, and dynamic content
- Use ARIA live regions for dynamic updates (alerts, notifications)
- Test with accessibility tools (Axe, Lighthouse, WebAIM)

**Accessibility example:**

```tsx
// Modal with proper focus management and ARIA
interface ModalProps {
  isOpen: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}

export const Modal: React.FC<ModalProps> = ({ isOpen, title, onClose, children }) => {
  const modalRef = useRef<HTMLDivElement>(null);
  const initialFocusRef = useRef<HTMLButtonElement>(null);

  // Focus management: return focus to trigger button when closing
  useEffect(() => {
    if (isOpen) {
      // Focus first focusable element in modal
      initialFocusRef.current?.focus();
      // Prevent body scroll
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
  }, [isOpen]);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      return () => document.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="modal-overlay"
      onClick={onClose}
      role="presentation"
      aria-hidden={!isOpen}
    >
      <div
        ref={modalRef}
        className="modal-content"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="modal-title">{title}</h2>
        {children}
        <button
          ref={initialFocusRef}
          onClick={onClose}
          aria-label="Close modal"
        >
          Close
        </button>
      </div>
    </div>
  );
};
```

**Accessibility checklist:**
- [ ] All images have descriptive alt text
- [ ] Color is not the only indicator of state (use icons/text too)
- [ ] Form inputs have associated labels
- [ ] Error messages are linked to form fields (aria-describedby)
- [ ] Dynamic content updates are announced (aria-live)
- [ ] Keyboard navigation works (Tab, Shift+Tab, Enter, Space, Escape)
- [ ] Focus is visible and has 3:1 contrast ratio
- [ ] Page tested with screen reader (NVDA on Windows or VoiceOver on Mac)
- [ ] Axe DevTools or Lighthouse shows no critical issues
- [ ] Heading hierarchy is correct (no skipped levels)

### 4. State Management

**Mandate:** Manage application state in a predictable, scalable way.

- Choose state management approach based on complexity: local state → Context API → Redux/MobX/Zustand
- Keep state as close to usage as possible (colocation)
- Use immutable patterns; never mutate state directly
- Normalize state shape to avoid duplication
- Implement proper selector memoization to avoid unnecessary re-renders
- Handle async operations (loading, error, success states)
- Persist state to localStorage when appropriate
- Test state reducers and selectors

**State architecture pattern:**

```tsx
// State definition with proper typing
interface User {
  id: string;
  email: string;
  name: string;
  role: 'admin' | 'user';
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

interface AppState {
  auth: AuthState;
  ui: {
    isDarkMode: boolean;
    sidebarOpen: boolean;
  };
}

// Redux-style reducer
type AuthAction =
  | { type: 'AUTH_START' }
  | { type: 'AUTH_SUCCESS'; payload: User }
  | { type: 'AUTH_FAILURE'; payload: string }
  | { type: 'AUTH_LOGOUT' };

const initialAuthState: AuthState = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
};

function authReducer(state = initialAuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'AUTH_START':
      return { ...state, isLoading: true, error: null };
    case 'AUTH_SUCCESS':
      return {
        ...state,
        user: action.payload,
        isAuthenticated: true,
        isLoading: false,
      };
    case 'AUTH_FAILURE':
      return { ...state, error: action.payload, isLoading: false };
    case 'AUTH_LOGOUT':
      return initialAuthState;
    default:
      return state;
  }
}

// Selectors with memoization
const selectUser = (state: AppState) => state.auth.user;
const selectIsAuthenticated = (state: AppState) => state.auth.isAuthenticated;
const selectAuthError = (state: AppState) => state.auth.error;

// Usage with hooks (assuming Redux)
const useAuth = () => {
  const user = useSelector(selectUser);
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const error = useSelector(selectAuthError);
  const dispatch = useDispatch();

  const login = useCallback((email: string, password: string) => {
    dispatch({ type: 'AUTH_START' });
    // API call...
  }, [dispatch]);

  return { user, isAuthenticated, error, login };
};
```

### 5. API Integration and Data Fetching

**Mandate:** Integrate seamlessly with backend APIs following contracts.

- Review `docs/tech-specs/api-spec.md` for endpoint contracts and authentication
- Use HTTP client library (axios, fetch, SWR, React Query/TanStack Query)
- Implement request/response interceptors for auth tokens and error handling
- Handle all HTTP states: loading, success, error, 401 unauthorized, 403 forbidden
- Implement optimistic updates where appropriate
- Use React Query or SWR for efficient server state management
- Implement proper error boundaries for network failures
- Add retry logic with exponential backoff for transient failures

**API client pattern:**

```tsx
// api/client.ts
import axios, { AxiosError, AxiosInstance } from 'axios';
import { useNavigate } from 'react-router-dom';

export const createApiClient = (navigate?: Function): AxiosInstance => {
  const client = axios.create({
    baseURL: process.env.REACT_APP_API_URL,
    timeout: 10000,
  });

  // Request interceptor: add auth token
  client.interceptors.request.use((config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  // Response interceptor: handle errors
  client.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
      if (error.response?.status === 401) {
        // Unauthorized: clear token and redirect to login
        localStorage.removeItem('auth_token');
        navigate?.('/login');
      } else if (error.response?.status === 403) {
        // Forbidden: show error message
        console.error('Access denied');
      }
      return Promise.reject(error);
    }
  );

  return client;
};

// hooks/useUsers.ts
import { useQuery, useMutation } from '@tanstack/react-query';
import { createApiClient } from '../api/client';

interface User {
  id: string;
  name: string;
  email: string;
}

const apiClient = createApiClient();

export const useUsers = () => {
  return useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      const response = await apiClient.get<User[]>('/users');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });
};

export const useCreateUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (newUser: Omit<User, 'id'>) => {
      const response = await apiClient.post<User>('/users', newUser);
      return response.data;
    },
    onSuccess: (newUser) => {
      // Update the users list cache
      queryClient.setQueryData(['users'], (oldData: User[] | undefined) => [
        ...(oldData || []),
        newUser,
      ]);
      // Show success notification
      showNotification('User created successfully');
    },
    onError: (error: AxiosError) => {
      showNotification(
        error.response?.data?.message || 'Failed to create user',
        'error'
      );
    },
  });
};

// Component usage
function UsersPage() {
  const { data: users, isLoading, error } = useUsers();
  const { mutate: createUser, isPending } = useCreateUser();

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <div>
      {users?.map((user) => (
        <UserCard key={user.id} user={user} />
      ))}
    </div>
  );
}
```

### 6. Performance Optimization

**Mandate:** Deliver fast, responsive interfaces that delight users.

- Use Chrome DevTools Lighthouse to measure performance (target: >90 score)
- Implement code splitting: split by route and feature
- Lazy-load images and components using `React.lazy()` and `IntersectionObserver`
- Memoize expensive computations: `useMemo`, `useCallback`, `React.memo`
- Monitor bundle size: use tools like `webpack-bundle-analyzer`
- Optimize assets: minify CSS/JS, compress images (WebP format), gzip compression
- Implement virtual scrolling for long lists
- Measure Core Web Vitals: LCP (Largest Contentful Paint), FID (First Input Delay), CLS (Cumulative Layout Shift)

**Performance optimization pattern:**

```tsx
// Code splitting by route
import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';

const Dashboard = lazy(() => import('./pages/Dashboard'));
const Settings = lazy(() => import('./pages/Settings'));
const AdminPanel = lazy(() => import('./pages/AdminPanel'));

export const App = () => {
  return (
    <Routes>
      <Route
        path="/dashboard"
        element={
          <Suspense fallback={<LoadingSpinner />}>
            <Dashboard />
          </Suspense>
        }
      />
      <Route
        path="/settings"
        element={
          <Suspense fallback={<LoadingSpinner />}>
            <Settings />
          </Suspense>
        }
      />
    </Routes>
  );
};

// Image lazy loading
const LazyImage: React.FC<{ src: string; alt: string }> = ({ src, alt }) => {
  const [imageSrc, setImageSrc] = useState<string | null>(null);
  const imgRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && imgRef.current) {
        setImageSrc(src);
        observer.unobserve(entry.target);
      }
    });

    if (imgRef.current) {
      observer.observe(imgRef.current);
    }

    return () => observer.disconnect();
  }, [src]);

  return (
    <img
      ref={imgRef}
      src={imageSrc || 'placeholder.png'}
      alt={alt}
      loading="lazy"
    />
  );
};

// Memoization for expensive computations
const UserList = React.memo(({ users, filter }: UserListProps) => {
  const filteredUsers = useMemo(
    () => users.filter((u) => u.name.includes(filter)),
    [users, filter]
  );

  const handleSort = useCallback((field: string) => {
    // Sorting logic
  }, []);

  return (
    <div>
      {filteredUsers.map((user) => (
        <UserCard key={user.id} user={user} onSort={handleSort} />
      ))}
    </div>
  );
});

// Virtual scrolling for large lists
import { FixedSizeList } from 'react-window';

const LargeList = ({ items }: { items: Item[] }) => {
  const Row = ({ index, style }: { index: number; style: React.CSSProperties }) => (
    <div style={style}>
      <UserCard user={items[index]} />
    </div>
  );

  return (
    <FixedSizeList
      height={600}
      itemCount={items.length}
      itemSize={50}
      width="100%"
    >
      {Row}
    </FixedSizeList>
  );
};
```

**Performance checklist:**
- [ ] Lighthouse score >90 on all pages
- [ ] Bundle size <250KB (gzipped)
- [ ] Code split by route
- [ ] Images optimized (WebP, correct sizes)
- [ ] No unnecessary re-renders (React DevTools Profiler)
- [ ] Core Web Vitals pass (LCP <2.5s, FID <100ms, CLS <0.1)
- [ ] Network requests optimized (debounced, batched)
- [ ] Caching strategy implemented

### 7. Testing Strategy

**Mandate:** Ensure code quality through comprehensive testing.

**Unit Tests:**
- Test components in isolation with mocked children and props
- Test logic (functions, reducers, selectors)
- Aim for >80% coverage on business logic
- Use React Testing Library (behavior-driven, not implementation-driven)

**Integration Tests:**
- Test component interactions and state changes
- Test form submission and validation
- Test API integration with mocked endpoints

**E2E Tests:**
- Test complete user flows (login → navigate → submit form)
- Use tools like Playwright or Cypress
- Test on multiple browsers

**Testing pattern:**

```tsx
// components/UserForm.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserForm } from './UserForm';

describe('UserForm', () => {
  it('renders form with required fields', () => {
    render(<UserForm onSubmit={jest.fn()} />);
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('validates email format', async () => {
    const handleSubmit = jest.fn();
    render(<UserForm onSubmit={handleSubmit} />);

    await userEvent.type(screen.getByLabelText(/email/i), 'invalid-email');
    await userEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid email/i)).toBeInTheDocument();
    });
    expect(handleSubmit).not.toHaveBeenCalled();
  });

  it('submits form with valid data', async () => {
    const handleSubmit = jest.fn();
    render(<UserForm onSubmit={handleSubmit} />);

    await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'SecurePass123');
    await userEvent.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(handleSubmit).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: 'SecurePass123',
      });
    });
  });
});

// pages/Dashboard.test.tsx (Integration test)
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Dashboard } from './Dashboard';

// Mock API client
jest.mock('../api/client', () => ({
  createApiClient: () => ({
    get: jest.fn().mockResolvedValue({
      data: [{ id: '1', name: 'John', email: 'john@example.com' }],
    }),
  }),
}));

describe('Dashboard', () => {
  it('displays user list after loading', async () => {
    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <Dashboard />
      </QueryClientProvider>
    );

    expect(screen.getByText(/loading/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('John')).toBeInTheDocument();
    });
  });
});
```

### 8. Design System and Component Library

**Mandate:** Build a shared component library that scales across products.

- Maintain a living component library in Storybook
- Document design tokens: colors, typography, spacing, shadows, animations
- Create component variants for all use cases
- Use consistent naming: Component, ComponentVariant, ComponentSize
- Export TypeScript types for all components
- Publish to npm or internal registry for sharing across teams
- Version components and maintain changelog

**Design system structure:**

```
src/
├── design-system/
│   ├── tokens/
│   │   ├── colors.ts
│   │   ├── typography.ts
│   │   ├── spacing.ts
│   │   └── shadows.ts
│   ├── components/
│   │   ├── Button/
│   │   ├── Input/
│   │   ├── Card/
│   │   ├── Modal/
│   │   └── ...
│   └── index.ts
```

## Workflow: From Story to Implementation

### Step 1: Read the Story and Design Spec
```markdown
**Story:** User Registration Form

**User Flow:**
1. User lands on /register
2. User fills email, password, confirm password
3. User clicks Submit
4. Form validates and submits to API
5. On success, redirect to /dashboard
6. On error, show error message

**Design Spec:** See Figma link for exact colors, typography, spacing
**API Contract:** See docs/tech-specs/api-spec.md for POST /users/register
```

### Step 2: Check Architecture and Tech Specs
- Review solution architecture for frontend topology
- Check API specification for endpoint contract
- Review design tokens and component library
- Check authentication flow documentation

### Step 3: Implement Feature

**File structure:**
```
src/
├── pages/
│   └── Register/
│       ├── Register.tsx
│       ├── Register.module.css
│       ├── Register.test.tsx
│       └── useRegister.ts
├── components/
│   └── AuthForm/
│       ├── AuthForm.tsx
│       ├── AuthForm.module.css
│       └── AuthForm.test.tsx
└── hooks/
    └── useAuth.ts
```

### Step 4: Write Tests and Documentation

Create comprehensive unit and integration tests, and Storybook stories.

### Step 5: Document in Design System

Add component to Storybook with usage examples and props documentation.

## Code Quality Standards

### Coding Conventions
- Use functional components with hooks (no class components)
- Follow React best practices: dependency arrays, event handler naming (handleX)
- Keep components small and focused (<200 lines)
- Use TypeScript for type safety
- Name components PascalCase, files match component name
- Use meaningful hook names: useX convention

### ESLint Rules
- Enforce React hooks rules
- Prevent prop-drilling (enable warnings)
- Enforce accessibility rules (jsx-a11y)
- Enforce performance rules (memoization)

## Artifact References

- **Design System:** Figma link or component library documentation
- **API Specification:** `docs/tech-specs/api-spec.md`
- **Solution Architecture:** `docs/architecture/solution-architecture.md`
- **Implementation Stories:** `docs/stories/`
- **Accessibility Guidelines:** `docs/tech-specs/wcag-guidelines.md`
- **Performance Budget:** `docs/tech-specs/performance-budget.md`

## Escalation & Collaboration

### Request Input From
- **Design Lead:** When design interpretation is unclear
- **Tech Lead:** When architecture conflicts with implementation
- **Backend Engineer:** When API contract needs clarification
- **QA:** When test strategy or edge cases need clarity

### Document Handoff
When feature is complete:
1. Update `.bmad/handoff-log.md` with implementation summary
2. Ensure all tests pass and Lighthouse score >90
3. Document any blocking issues in `.bmad/project-state.md`
4. Notify Tech Lead for code review

## Tools & Commands

```bash
# Development
npm start                          # Start dev server
npm run dev                        # Alternative dev server

# Testing
npm test                           # Run all tests
npm run test:watch                 # Watch mode
npm run coverage                   # Coverage report

# Code quality
npm run lint                       # ESLint
npm run format                     # Prettier format
npm run type-check                 # TypeScript check

# Build & Performance
npm run build                      # Production build
npm run analyze                    # Bundle size analysis
npm run lighthouse                 # Lighthouse report

# Design system
npm run storybook                  # Start Storybook dev server
npm run build-storybook            # Build Storybook
```

---

**Last Updated:** [Current Phase]
**Trigger:** When implementation stories and design specs are ready
**Output:** Responsive, accessible, performant UI components and pages with tests
