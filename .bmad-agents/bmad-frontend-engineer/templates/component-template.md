# Component Template: [ComponentName]

## Component Specification

- **Component Name:** [ComponentName]
- **Component Type:** [Atom | Molecule | Organism | Page | Layout]
- **Component Owner:** [Developer Name / Team]
- **Last Updated:** [ISO 8601 Date]
- **Design Link:** [Figma URL to component design]
- **Storybook:** [Link to component in Storybook instance]

---

## Purpose & Usage

### Description
[2-3 sentence explanation of what this component does, its primary business purpose, and where it's used in the application]

### Use Cases
1. [Primary use case with context]
2. [Secondary use case]
3. [Common integration pattern]

### When to Use
- Use this component when [specific condition]
- Use this component to [specific purpose]
- Avoid using this component if [anti-pattern]

### Related Components
- [ParentComponent] - Contains this component
- [SiblingComponent] - Often used alongside
- [AlternativeComponent] - Use if [different requirement]

---

## Props Interface

| Prop Name | Type | Required | Default | Description |
|---|---|---|---|---|
| `id` | string | No | undefined | Unique identifier for the component instance (required for accessibility) |
| `className` | string | No | undefined | Additional CSS classes to apply to root element |
| `data-testid` | string | No | undefined | Test identifier for automated testing |
| `disabled` | boolean | No | false | Disables the component, graying out and preventing interaction |
| `loading` | boolean | No | false | Shows loading state; disables interaction |
| `error` | string \| null | No | null | Error message to display; places component in error state |
| `size` | 'sm' \| 'md' \| 'lg' | No | 'md' | Visual size variant (affects padding, font size, dimensions) |
| `variant` | 'default' \| 'primary' \| 'secondary' \| 'danger' | No | 'default' | Visual style variant (affects colors and styling) |
| `isVisible` | boolean | No | true | Controls visibility; hidden elements not rendered in DOM |
| `tooltip` | string | No | undefined | Tooltip text shown on hover; automatically truncates |
| `aria-label` | string | No | undefined | Accessible label for screen readers (required if no visible label) |
| `aria-describedby` | string | No | undefined | ID of element containing description for screen readers |
| [Custom Prop 1] | type | required? | default | [Description] |
| [Custom Prop 2] | type | required? | default | [Description] |

### Props Notes
- Props marked `required: true` must be provided; omitting causes TypeScript error
- Props marked `required: false` have sensible defaults; always check defaults
- String props should be validated against allowed values using discriminated unions
- Never pass objects/arrays as props if they need deep equality checks; use memoization or derived values

---

## State Management

### Local State
```typescript
// State variables managed internally by this component
const [isExpanded, setIsExpanded] = useState(false);
const [selectedValue, setSelectedValue] = useState<string | null>(null);
```

**Local state should be used for:**
- Temporary UI state (expanded/collapsed, visible dropdowns)
- Form input focus and validation
- Animation state
- Transient user interactions not affecting application

### Global Store Slices
```typescript
// Zustand store slices used by this component
const { user, setUser } = useUserStore();
const { themeMode, setThemeMode } = useThemeStore();
```

**Global state should be used for:**
- User authentication and profile
- Application theme/preferences
- Persistent application settings
- Data shared across multiple components

### Server State via React Query
```typescript
// Data fetching and caching via React Query
const { data: items, isLoading, error } = useQuery({
  queryKey: ['items', id],
  queryFn: () => apiClient.getItems(id),
});
```

**Server state should be used for:**
- Data from backend APIs
- Lists requiring pagination/filtering
- Data shared across multiple routes/pages
- Data with cache invalidation requirements

---

## Events & Callbacks

| Event/Callback | Payload Type | When Fired | Required |
|---|---|---|---|
| `onClick` | `React.MouseEvent<HTMLButtonElement>` | User clicks the component | No |
| `onChange` | `T` (value type) | User changes value (input, select, checkbox) | No |
| `onFocus` | `React.FocusEvent<HTMLElement>` | Component receives focus (keyboard or mouse) | No |
| `onBlur` | `React.FocusEvent<HTMLElement>` | Component loses focus | No |
| `onError` | `Error` | Error occurs during operation | No |
| `onSubmit` | `FormData` | Form submission (if form component) | No |
| `onCancel` | `void` | User cancels operation | No |
| `onOpen` | `void` | Dialog/modal opens | No |
| `onClose` | `void` | Dialog/modal closes | No |
| `onSuccess` | `T` (result type) | Async operation completes successfully | No |

### Callback Usage Notes
- Callbacks are optional; component works with defaults if not provided
- Always provide `data-testid` or `aria-label` for elements firing callbacks (accessibility)
- Debounce `onChange` callbacks for high-frequency events (typing, scrolling)
- Handle null/undefined safely in callback implementations

---

## Accessibility (WCAG 2.2 AA)

### ARIA Roles
- **Role:** [semantic-html-element or explicit ARIA role]
  - Example: `<button>` (implicit `role="button"`) or `<div role="tablist">`
- **States/Properties:** `aria-expanded`, `aria-selected`, `aria-disabled`, `aria-invalid`
- **Live Regions:** Use `aria-live="polite"` for dynamic content updates

### Keyboard Interactions

| Key(s) | Behavior | Notes |
|---|---|---|
| Tab | Move focus to next interactive element | Standard; focus management required |
| Shift+Tab | Move focus to previous element | Standard; no special handling |
| Enter | Activate button/submit form | Standard; no special handling |
| Space | Toggle checkbox/toggle button | Standard |
| Arrow Up/Down | Navigate in list/select/menu | Custom focus management required |
| Escape | Close modal/dismiss dropdown | Custom handler required |
| Home | Jump to first item (list context) | Optional; nice-to-have |
| End | Jump to last item (list context) | Optional; nice-to-have |

### Focus Management
- **Focus Visible Indicator:** Minimum 3px outline/border, 3:1 contrast ratio
- **Focus Trap:** Modal/dialog should trap focus within it (use `FocusScope` library)
- **Focus Restore:** After closing modal, restore focus to triggering element
- **Initial Focus:** Modals should focus first interactive element (button, input)

```typescript
// Focus management example
const modalRef = useRef<HTMLDivElement>(null);
const triggerRef = useRef<HTMLButtonElement>(null);

const openModal = () => {
  setIsOpen(true);
  // Defer focus to next frame to allow DOM update
  setTimeout(() => modalRef.current?.focus(), 0);
};

const closeModal = () => {
  setIsOpen(false);
  // Restore focus to trigger element
  triggerRef.current?.focus();
};
```

### Screen Reader Considerations
- **Labels:** All form inputs need associated `<label>` or `aria-label`
- **Descriptions:** Use `aria-describedby` to link detailed descriptions
- **Images:** `<img>` must have descriptive `alt` text (or `alt=""` if decorative)
- **Links:** Link text should describe destination ("Click here" is bad; "View user profile" is good)
- **Status Messages:** Use `aria-live="assertive"` for critical updates (errors, confirmations)
- **Abbreviations:** Use `<abbr title="...">` for abbreviations to expand on hover/focus

```typescript
// Screen reader best practices
<div>
  <label htmlFor="email-input">Email Address</label>
  <input
    id="email-input"
    type="email"
    aria-describedby="email-help"
    aria-invalid={!!error}
    aria-errormessage={error ? "email-error" : undefined}
  />
  <p id="email-help" className="text-sm text-gray-500">
    We'll never share your email
  </p>
  {error && (
    <p id="email-error" role="alert" className="text-red-600">
      {error}
    </p>
  )}
</div>
```

---

## Usage Examples

### Basic Usage
```typescript
import { ComponentName } from '@bmad/ui';

export default function BasicExample() {
  return (
    <ComponentName
      id="component-basic"
      variant="primary"
      size="md"
    >
      Click me
    </ComponentName>
  );
}
```

### With Event Handlers
```typescript
export default function WithHandlers() {
  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    console.log('Clicked!', event);
  };

  return (
    <ComponentName
      id="component-with-handler"
      onClick={handleClick}
      disabled={false}
      aria-label="Trigger action"
    >
      Action Button
    </ComponentName>
  );
}
```

### With State Management
```typescript
import { useState } from 'react';

export default function WithState() {
  const [isActive, setIsActive] = useState(false);

  return (
    <ComponentName
      id="component-stateful"
      variant={isActive ? 'primary' : 'default'}
      onClick={() => setIsActive(!isActive)}
      aria-pressed={isActive}
    >
      {isActive ? 'Active' : 'Inactive'}
    </ComponentName>
  );
}
```

### With Form Integration
```typescript
import { useForm } from 'react-hook-form';

export default function InForm() {
  const { register, handleSubmit } = useForm();

  return (
    <form onSubmit={handleSubmit((data) => console.log(data))}>
      <ComponentName
        id="component-form"
        {...register('fieldName', { required: 'This field is required' })}
      />
      <button type="submit">Submit</button>
    </form>
  );
}
```

### Responsive / With Props Variants
```typescript
export default function ResponsiveExample() {
  const [size, setSize] = useState<'sm' | 'md' | 'lg'>('md');

  return (
    <div className="space-y-4">
      <ComponentName size="sm" variant="default">Small</ComponentName>
      <ComponentName size="md" variant="primary">Medium</ComponentName>
      <ComponentName size="lg" variant="secondary">Large</ComponentName>
      <ComponentName variant="danger" disabled>Disabled</ComponentName>
    </div>
  );
}
```

---

## Visual Variants

| Variant Name | Use Case | Screenshot/Link | Notes |
|---|---|---|---|
| `default` | Standard/neutral appearance | [Figma link] | Suitable for non-critical actions |
| `primary` | Primary action, draws attention | [Figma link] | Use for main CTA on page |
| `secondary` | Secondary action, lower emphasis | [Figma link] | Alternative actions |
| `danger` | Destructive action (delete, remove) | [Figma link] | Red/warning colors; use with care |
| `loading` | Async operation in progress | [Figma link] | Shows spinner; disables interaction |
| `disabled` | Component unavailable | [Figma link] | Gray out; remove pointer events |
| `error` | Error state (validation failure) | [Figma link] | Red border/text; show error message |
| `success` | Success state (operation complete) | [Figma link] | Green colors; optional celebration |

### Size Variants

| Size | Padding | Font Size | Height | Use Case |
|---|---|---|---|---|
| `sm` | 6px 12px | 12px | 32px | Compact layouts, secondary actions |
| `md` | 10px 16px | 14px | 40px | Default, most common |
| `lg` | 14px 20px | 16px | 48px | Primary CTAs, mobile |

---

## Performance Considerations

### Memoization
```typescript
// Memoize component if expensive to render
const ComponentName = React.memo(function ComponentName(props) {
  return <button>{props.children}</button>;
});

// Or use useMemo for expensive computations
const expensiveValue = useMemo(() => {
  return calculateComplexValue(items);
}, [items]);
```

**When to use React.memo:**
- Component has many props that change infrequently
- Component has expensive render logic
- Parent component re-renders often
- Component props don't change between renders

**When NOT to use:**
- Simple components (no complex logic)
- Callbacks defined inline in parent (breaks memoization)
- Props contain objects/arrays created inline (breaks equality check)

### Lazy Loading
```typescript
// Lazy load component if heavy or conditionally used
const ComponentName = lazy(() => import('./ComponentName'));

export default function Page() {
  return (
    <Suspense fallback={<Spinner />}>
      <ComponentName />
    </Suspense>
  );
}
```

**Use lazy loading for:**
- Large components (> 50KB)
- Components used conditionally or below fold
- Modal/dialog content
- Tab content in tabbed interface

### Virtualization
```typescript
// Virtualize long lists for performance
import { FixedSizeList } from 'react-window';

const Row = ({ index, style }) => (
  <div style={style}>{items[index]}</div>
);

<FixedSizeList
  height={600}
  itemCount={items.length}
  itemSize={35}
  width="100%"
>
  {Row}
</FixedSizeList>
```

**Use virtualization for:**
- Lists with > 100 items
- Dynamic data requiring full-table rendering
- Complex row components with many dependencies

---

## Testing

### Key Test Cases to Cover
1. **Rendering:** Component renders with required props
2. **Props:** Component respects all prop combinations
3. **Callbacks:** Event handlers fire with correct payload
4. **Accessibility:** ARIA attributes present, keyboard navigation works
5. **Edge Cases:** Null/undefined props, empty state, error state
6. **Integration:** Component works within parent context
7. **Variants:** All visual variants render correctly
8. **Loading/Error States:** Loading and error props render appropriately

### Test ID Naming Convention
```typescript
// Pattern: [component]-[element]
data-testid="component-name-button"
data-testid="component-name-input-field"
data-testid="component-name-error-message"
data-testid="component-name-loading-spinner"
```

### Example Test Suite
```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ComponentName } from './ComponentName';

describe('ComponentName', () => {
  it('renders with children', () => {
    render(<ComponentName>Click me</ComponentName>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('fires onClick callback when clicked', async () => {
    const handleClick = jest.fn();
    render(<ComponentName onClick={handleClick}>Button</ComponentName>);

    await userEvent.click(screen.getByText('Button'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('has accessible label', () => {
    render(<ComponentName aria-label="Test button">Button</ComponentName>);
    expect(screen.getByLabelText('Test button')).toBeInTheDocument();
  });

  it('disables interaction when disabled prop is true', () => {
    const handleClick = jest.fn();
    render(
      <ComponentName disabled onClick={handleClick}>
        Button
      </ComponentName>
    );

    fireEvent.click(screen.getByText('Button'));
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('supports keyboard interaction', async () => {
    const handleClick = jest.fn();
    render(<ComponentName onClick={handleClick}>Button</ComponentName>);

    const button = screen.getByText('Button');
    button.focus();
    fireEvent.keyDown(button, { key: 'Enter', code: 'Enter' });
    expect(handleClick).toHaveBeenCalled();
  });
});
```

---

## Storybook Story

```typescript
import type { Meta, StoryObj } from '@storybook/react';
import { ComponentName } from './ComponentName';

const meta: Meta<typeof ComponentName> = {
  title: 'UI/[ComponentName]',
  component: ComponentName,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: { type: 'select' },
      options: ['sm', 'md', 'lg'],
      description: 'Component size variant',
    },
    variant: {
      control: { type: 'select' },
      options: ['default', 'primary', 'secondary', 'danger'],
      description: 'Visual style variant',
    },
    disabled: {
      control: { type: 'boolean' },
      description: 'Disable interaction',
    },
    onClick: { action: 'clicked' },
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    children: 'Click me',
    size: 'md',
    variant: 'default',
  },
};

export const Primary: Story = {
  args: {
    children: 'Primary Action',
    size: 'md',
    variant: 'primary',
  },
};

export const Disabled: Story = {
  args: {
    children: 'Disabled Button',
    disabled: true,
  },
};

export const Loading: Story = {
  args: {
    children: 'Loading...',
    loading: true,
  },
};

export const WithError: Story = {
  args: {
    children: 'Submit',
    error: 'Form validation failed',
  },
};

export const Sizes: Story = {
  render: () => (
    <div className="flex gap-4">
      <ComponentName size="sm">Small</ComponentName>
      <ComponentName size="md">Medium</ComponentName>
      <ComponentName size="lg">Large</ComponentName>
    </div>
  ),
};
```

---

## TypeScript Interface

```typescript
import { ReactNode, ReactElement } from 'react';

export interface ComponentNameProps {
  /**
   * Unique identifier for the component instance
   * Required for proper ARIA labeling and testing
   */
  id?: string;

  /**
   * Child content to render inside component
   */
  children: ReactNode;

  /**
   * Component size variant
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg';

  /**
   * Visual style variant
   * @default 'default'
   */
  variant?: 'default' | 'primary' | 'secondary' | 'danger';

  /**
   * Disables interaction and grays out component
   * @default false
   */
  disabled?: boolean;

  /**
   * Shows loading state; disables interaction
   * @default false
   */
  loading?: boolean;

  /**
   * Error message; renders error state
   */
  error?: string | null;

  /**
   * Fired when user clicks component
   */
  onClick?: (event: React.MouseEvent<HTMLButtonElement>) => void;

  /**
   * Additional CSS classes applied to root element
   */
  className?: string;

  /**
   * ARIA label for accessibility
   * Required if component has no visible label
   */
  'aria-label'?: string;

  /**
   * ID of element describing this component
   */
  'aria-describedby'?: string;

  /**
   * Test identifier for automated testing
   */
  'data-testid'?: string;
}

export function ComponentName({
  id,
  children,
  size = 'md',
  variant = 'default',
  disabled = false,
  loading = false,
  error = null,
  onClick,
  className,
  ...a11yProps
}: ComponentNameProps): ReactElement {
  // Implementation here
  return <button>{children}</button>;
}
```

---

## Component File Structure

```typescript
// ComponentName.tsx
import { ReactNode, ReactElement } from 'react';
import styles from './ComponentName.module.css';

export interface ComponentNameProps {
  id?: string;
  children: ReactNode;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'default' | 'primary' | 'secondary' | 'danger';
  disabled?: boolean;
  loading?: boolean;
  error?: string | null;
  onClick?: (event: React.MouseEvent<HTMLButtonElement>) => void;
  className?: string;
  'aria-label'?: string;
  'aria-describedby'?: string;
  'data-testid'?: string;
}

/**
 * ComponentName - [Brief description of component]
 *
 * [Detailed description of purpose and usage]
 *
 * @example
 * <ComponentName variant="primary" onClick={handleClick}>
 *   Action Button
 * </ComponentName>
 */
export function ComponentName({
  id,
  children,
  size = 'md',
  variant = 'default',
  disabled = false,
  loading = false,
  error = null,
  onClick,
  className,
  ...a11yProps
}: ComponentNameProps): ReactElement {
  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    if (!disabled && !loading) {
      onClick?.(event);
    }
  };

  return (
    <button
      id={id}
      className={`
        ${styles.root}
        ${styles[size]}
        ${styles[variant]}
        ${disabled ? styles.disabled : ''}
        ${loading ? styles.loading : ''}
        ${error ? styles.error : ''}
        ${className || ''}
      `}
      disabled={disabled || loading}
      onClick={handleClick}
      aria-disabled={disabled || loading}
      aria-busy={loading}
      {...a11yProps}
    >
      {loading ? <Spinner /> : children}
      {error && (
        <span className={styles.errorMessage} role="alert">
          {error}
        </span>
      )}
    </button>
  );
}

export default ComponentName;
```

---

## CSS Module (Responsive Styles)

```css
/* ComponentName.module.css */

.root {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
  font-weight: 500;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  outline: none;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:focus-visible {
    outline: 3px solid var(--color-focus);
    outline-offset: 2px;
  }
}

/* Size variants */
.sm {
  padding: 6px 12px;
  font-size: 12px;
  height: 32px;
}

.md {
  padding: 10px 16px;
  font-size: 14px;
  height: 40px;
}

.lg {
  padding: 14px 20px;
  font-size: 16px;
  height: 48px;
}

/* Color variants */
.default {
  background-color: var(--color-gray-100);
  color: var(--color-gray-900);
  border-color: var(--color-gray-300);
}

.primary {
  background-color: var(--color-blue-600);
  color: white;
  border-color: var(--color-blue-700);
}

.secondary {
  background-color: var(--color-gray-200);
  color: var(--color-gray-800);
}

.danger {
  background-color: var(--color-red-600);
  color: white;
}

/* State variants */
.disabled {
  opacity: 0.6;
  cursor: not-allowed;
  pointer-events: none;
}

.loading {
  pointer-events: none;
  opacity: 0.8;
}

.error {
  border-color: var(--color-red-500);
  background-color: var(--color-red-50);
}

/* Media queries for responsive sizing */
@media (max-width: 640px) {
  .root {
    width: 100%;
  }
}
```
