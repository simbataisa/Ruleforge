# UI Specification Template - Engineering Handoff Document

## Specification Metadata

| Field                        | Value                                                   |
| ---------------------------- | ------------------------------------------------------- |
| **Feature Name**             | User Profile Management and Settings                    |
| **Epic**                     | Account Management Platform                             |
| **Designer**                 | sarah@example.com (Product Designer)                    |
| **Developer(s)**             | james@example.com, maria@example.com                    |
| **Figma Link**               | https://www.figma.com/file/abc123/user-profile-redesign |
| **Last Updated**             | 2024-03-14 14:30 UTC                                    |
| **Status**                   | Approved (Ready for handoff)                            |
| **Design System Version**    | DS 2.1                                                  |
| **Accessibility Compliance** | WCAG 2.1 AA (verified)                                  |

---

## Design Intent Summary

### What Experience Are We Creating?

We're creating a modern, intuitive profile management experience that allows users to view and edit their account settings in one cohesive place. Previously, account settings were scattered across different pages (profile on page A, security on page B, notifications on page C). This redesign consolidates everything into a tabbed interface where users can find all their settings on one page.

### Why Are We Creating It?

**Problems we're solving:**

1. User confusion: Users cannot find where to change settings (received 300+ support tickets about this)
2. Information overload: Mixing profile info, security settings, and preferences made pages overwhelming
3. Inconsistent experience: Each setting area had different design patterns (no consistency)
4. Mobile experience broken: Settings didn't work well on phones (not responsive)

### Success Metrics

After launch, we'll measure:

- **Task completion rate:** Users can change profile info within 2 minutes (currently: 3-4 min with old flow)
- **Support tickets:** Reduce "How do I change my settings?" tickets by 80%
- **Feature adoption:** 40% of users visit settings page within 30 days
- **User satisfaction:** NPS for settings experience >8/10

---

## User Flow Overview

### Primary Flow: User Updates Profile Information

```
1. User logged in → Clicks "Settings" in header menu
   ↓
2. Settings page loads → User sees 4 tabs (Profile, Security, Notifications, Billing)
   ↓
3. User clicks "Profile" tab (default selected)
   ↓
4. User sees profile form with: First Name, Last Name, Email, Phone, Avatar
   ↓
5. User edits field (e.g., changes first name from "John" to "Jonah")
   ↓
6. Input validation runs (no special characters, max 50 chars) → Shows error if invalid
   ↓
7. User clicks "Save Changes" button
   ↓
8. API request sent to backend (POST /api/profile)
   ↓
9. If success (2xx):
      - Toast notification: "Profile updated successfully"
      - Fields reload with new values
      - Tab icon shows checkmark (profile complete)

   If error (4xx/5xx):
      - Error toast: "Failed to save profile. Please try again."
      - Form keeps user's edits (don't lose data)
      - User can retry
```

### Alternative Flows

**Flow 2: Change Password**

1. User clicks "Security" tab
2. Clicks "Change Password"
3. Modal opens with: Current Password, New Password, Confirm Password
4. User enters values and clicks "Update Password"
5. On success: Modal closes, password changed
6. On error: Shows error message in modal

**Flow 3: Manage Notification Preferences**

1. User clicks "Notifications" tab
2. Sees toggles for: Email for new messages, Email for payments, SMS alerts, etc.
3. User toggles a preference
4. Change saved immediately (no "Save" button needed)
5. Brief success message: "Preference updated"

---

## Screen Inventory

| Screen ID    | Screen Name              | Entry Point                   | Figma Frame                                                    | Status   | Notes                                   |
| ------------ | ------------------------ | ----------------------------- | -------------------------------------------------------------- | -------- | --------------------------------------- |
| **PROF-001** | Profile Settings         | /settings or /account/profile | [Profile Tab](https://figma.com/file/abc123?node-id=142)       | Approved | Main settings page, default tab         |
| **PROF-002** | Security Settings        | /settings/security            | [Security Tab](https://figma.com/file/abc123?node-id=143)      | Approved | Change password, 2FA, sessions          |
| **PROF-003** | Notification Preferences | /settings/notifications       | [Notifications Tab](https://figma.com/file/abc123?node-id=144) | Approved | Email/SMS notification toggles          |
| **PROF-004** | Billing & Subscription   | /settings/billing             | [Billing Tab](https://figma.com/file/abc123?node-id=145)       | Approved | Subscription status, payment methods    |
| **PROF-005** | Change Password Modal    | (modal within PROF-002)       | [Change Password](https://figma.com/file/abc123?node-id=150)   | Approved | Opened from Security tab                |
| **PROF-006** | Confirm Action Modal     | (modal, reusable)             | [Confirm Modal](https://figma.com/file/abc123?node-id=151)     | Approved | Used for delete account, logout, etc    |
| **PROF-007** | Loading State            | (state within pages)          | [Loading](https://figma.com/file/abc123?node-id=160)           | Approved | Skeleton loaders while fetching data    |
| **PROF-008** | Empty State              | (state within pages)          | [Empty](https://figma.com/file/abc123?node-id=161)             | Approved | If no payment methods, no sessions, etc |

---

## Per-Screen Specification

### Screen PROF-001: Profile Settings Tab

#### Screen Purpose and User Goal

Users should be able to view and edit their personal information (first name, last name, email, phone, avatar) in one place. Users should understand their changes are saved and see confirmation.

#### Layout Grid

```
Desktop (1920px):
  - Grid: 12 columns (80px per column + 20px gutter)
  - Max content width: 1280px (centered on page)
  - Margin: 40px (top), 40px (bottom), 60px (left/right on desktop)

Tablet (768px):
  - Grid: 8 columns
  - Margin: 20px (top), 20px (bottom), 20px (left/right)

Mobile (375px):
  - Grid: 4 columns (single column layout)
  - Margin: 16px (all sides)
  - Full width form (no max-width constraint)
```

#### Components Used

| Component              | Variant       | Props/State                             | Source        |
| ---------------------- | ------------- | --------------------------------------- | ------------- |
| **Container**          | Card          | Elevation: 1, Padding: 32px             | Design System |
| **Heading**            | h2            | Text: "Profile", Color: text-primary    | Design System |
| **Form Group**         | Text Input    | Label: above input, margin-bottom: 24px | Design System |
| **Form Group**         | Avatar Upload | File input + preview, support drag-drop | Custom (new)  |
| **Button**             | Primary       | Text: "Save Changes", type: submit      | Design System |
| **Button**             | Secondary     | Text: "Cancel", type: reset             | Design System |
| **Error Message**      | Text          | Color: error, Icon: alert triangle      | Design System |
| **Toast Notification** | Success       | Text: "Profile saved", duration: 4s     | Design System |

#### Content Specifications

| Field          | Label             | Placeholder                 | Character Limit | Required | Validation                                |
| -------------- | ----------------- | --------------------------- | --------------- | -------- | ----------------------------------------- |
| **First Name** | "First Name"      | "Enter first name"          | 50 chars        | Yes      | Alphanumeric + space, no special chars    |
| **Last Name**  | "Last Name"       | "Enter last name"           | 50 chars        | Yes      | Alphanumeric + space, no special chars    |
| **Email**      | "Email Address"   | "user@example.com"          | 100 chars       | Yes      | Valid email format (RFC 5322)             |
| **Phone**      | "Phone Number"    | "+1 (555) 000-0000"         | 20 chars        | No       | E.164 format or local format accepted     |
| **Bio**        | "About You"       | "Tell us about yourself..." | 500 chars       | No       | Any characters, display char count        |
| **Avatar**     | "Profile Picture" | "Click to upload"           | 5MB max         | No       | PNG, JPG, WebP formats (auto-crop to 1:1) |

#### Interaction Specifications

| Trigger                      | Action                                                  | Animation/Transition             | Duration       |
| ---------------------------- | ------------------------------------------------------- | -------------------------------- | -------------- |
| **User focuses input field** | Field border changes to blue, cursor active             | Outline color fade-in            | 100ms          |
| **User types in field**      | Input validation runs, error message appears if invalid | Error text fade-in               | 200ms          |
| **User clicks "Save"**       | Button becomes disabled, shows spinner                  | Spinner rotation                 | Until response |
| **Save succeeds**            | Toast notification slides in from top                   | Slide-down + fade-in             | 300ms entrance |
| **User leaves page**         | If form dirty (unsaved changes), show confirm dialog    | Modal fade-in                    | 200ms          |
| **Avatar drag-over**         | Upload area highlights with blue dashed border          | Border color + background change | 100ms          |

#### States Table

| State                 | Figma Frame                                                     | Description                                                          | When to Show                                  |
| --------------------- | --------------------------------------------------------------- | -------------------------------------------------------------------- | --------------------------------------------- |
| **Default**           | [Profile-Default](https://figma.com/file/abc123?node-id=171)    | Form with populated values, no errors                                | Page initially loads with user's current data |
| **Editing**           | [Profile-Editing](https://figma.com/file/abc123?node-id=172)    | Form with unsaved changes (save button enabled), maybe error message | User has modified a field, not submitted      |
| **Saving**            | [Profile-Saving](https://figma.com/file/abc123?node-id=173)     | Button shows spinner, form fields disabled                           | API request in flight, waiting for response   |
| **Success**           | [Profile-Success](https://figma.com/file/abc123?node-id=174)    | Toast notification shows "Saved", form resets                        | API returns 200 OK                            |
| **Error**             | [Profile-Error](https://figma.com/file/abc123?node-id=175)      | Error message shows below field (red text + icon), form re-enabled   | API returns 4xx or 5xx error                  |
| **Validation Error**  | [Profile-Validation](https://figma.com/file/abc123?node-id=176) | Field border red, error text below field                             | User typed invalid input, left field          |
| **Loading (Initial)** | [Profile-Loading](https://figma.com/file/abc123?node-id=177)    | Skeleton placeholders for form fields                                | Page first loaded, fetching user data         |
| **Empty (No Data)**   | [Profile-Empty](https://figma.com/file/abc123?node-id=178)      | Fields empty, placeholder text visible                               | New user has not filled in profile (rare)     |

#### Responsive Behavior

| Breakpoint               | Layout Change                                      | Visual Change                                        |
| ------------------------ | -------------------------------------------------- | ---------------------------------------------------- |
| **Desktop (1920px)**     | Two-column grid for form fields if space allows    | Avatar preview on right side (160px × 160px)         |
| **Tablet (768px)**       | Single column for form fields                      | Avatar preview 120px × 120px, centered above form    |
| **Mobile (375px)**       | Full-width single column, buttons stack vertically | Avatar preview 100px × 100px, stacked buttons        |
| **Small Mobile (320px)** | Reduce padding, reduce font sizes                  | Padding: 12px (was 16px), Body text: 14px (was 16px) |

---

## Design Tokens Used

### Colours

| Token Name            | Hex Value | Usage                        | Light Mode | Dark Mode |
| --------------------- | --------- | ---------------------------- | ---------- | --------- |
| **primary-500**       | #2563EB   | Button primary, input focus  | #2563EB    | #60A5FA   |
| **primary-700**       | #1D4ED8   | Button hover state           | #1D4ED8    | #3B82F6   |
| **error-500**         | #EF4444   | Error text, error border     | #EF4444    | #F87171   |
| **success-500**       | #10B981   | Success states, checkmark    | #10B981    | #34D399   |
| **text-primary**      | #1F2937   | Body text, labels            | #1F2937    | #F3F4F6   |
| **text-secondary**    | #6B7280   | Helper text, placeholders    | #6B7280    | #D1D5DB   |
| **background-subtle** | #F9FAFB   | Form backgrounds, light card | #F9FAFB    | #1F2937   |
| **border-default**    | #E5E7EB   | Form borders, dividers       | #E5E7EB    | #374151   |

### Typography

| Token         | Value                                            | Usage                      |
| ------------- | ------------------------------------------------ | -------------------------- |
| **heading-3** | Font: Inter, Size: 24px, Weight: 600, Line: 1.5  | Screen title ("Profile")   |
| **body-lg**   | Font: Inter, Size: 16px, Weight: 400, Line: 1.5  | Form input text, body copy |
| **body-sm**   | Font: Inter, Size: 14px, Weight: 400, Line: 1.5  | Helper text, labels        |
| **label**     | Font: Inter, Size: 14px, Weight: 500, Line: 1.25 | Form field labels          |
| **caption**   | Font: Inter, Size: 12px, Weight: 400, Line: 1.25 | Char counter, hints        |

### Spacing

| Token        | Value | Usage                                  |
| ------------ | ----- | -------------------------------------- |
| **space-4**  | 4px   | Micro spacing (icon margin, etc)       |
| **space-8**  | 8px   | Small spacing (input padding, etc)     |
| **space-12** | 12px  | Medium spacing (between elements)      |
| **space-16** | 16px  | Standard spacing (between form groups) |
| **space-24** | 24px  | Large spacing (between sections)       |
| **space-32** | 32px  | XL spacing (padding inside cards)      |
| **space-40** | 40px  | Margin between major sections          |

### Elevations (Shadows)

| Token           | CSS Value                   | Usage                             |
| --------------- | --------------------------- | --------------------------------- |
| **elevation-0** | none                        | Flat, no shadow                   |
| **elevation-1** | 0 1px 2px rgba(0,0,0,0.05)  | Subtle cards, inputs              |
| **elevation-2** | 0 4px 6px rgba(0,0,0,0.07)  | Hovered cards, important elements |
| **elevation-3** | 0 10px 15px rgba(0,0,0,0.1) | Modals, dropdowns                 |

---

## Animation and Motion Specification

### Motion Tokens

| Motion Event          | Easing                            | Duration | Use Case                             |
| --------------------- | --------------------------------- | -------- | ------------------------------------ |
| **Micro-interaction** | cubic-bezier(0.4, 0, 0.2, 1)      | 100ms    | Hover states, quick feedback         |
| **Short transition**  | cubic-bezier(0.4, 0, 0.2, 1)      | 200ms    | Field focus, tooltip show            |
| **Medium transition** | cubic-bezier(0.4, 0, 0.2, 1)      | 300ms    | Modal entrance, toast appear         |
| **Long transition**   | cubic-bezier(0.4, 0, 0.2, 1)      | 500ms    | Page transition, major layout change |
| **Spring**            | cubic-bezier(0.34, 1.56, 0.64, 1) | 600ms    | Celebratory animation, emphasis      |

### Specific Animations

| Animation      | Elements                          | Timing                                       | Effect                           |
| -------------- | --------------------------------- | -------------------------------------------- | -------------------------------- |
| **Fade In**    | Toast notification, error message | 200ms ease-out                               | Opacity 0 → 1                    |
| **Slide Down** | Toast from top of screen          | 300ms ease-out                               | Transform: translateY(-20px) → 0 |
| **Spin**       | Loading spinner on button         | Infinite 1s linear                           | Rotate 360deg continuously       |
| **Pulse**      | Saving indicator                  | 2s ease-in-out                               | Opacity pulse between 1 and 0.5  |
| **Bounce**     | Validation error appearance       | 300ms cubic-bezier(0.68, -0.55, 0.265, 1.55) | Scale 0.95 → 1                   |

---

## Accessibility Requirements

### Colour Contrast

All text must meet WCAG AA minimum (4.5:1 for normal text, 3:1 for large text):

| Element     | Foreground               | Background                  | Ratio  | Status          |
| ----------- | ------------------------ | --------------------------- | ------ | --------------- |
| Body text   | text-primary (#1F2937)   | white (#FFFFFF)             | 13.2:1 | ✓ Pass AA + AAA |
| Labels      | text-secondary (#6B7280) | white (#FFFFFF)             | 7.1:1  | ✓ Pass AA + AAA |
| Placeholder | text-secondary (#6B7280) | background-subtle (#F9FAFB) | 6.8:1  | ✓ Pass AA + AAA |
| Error text  | error-500 (#EF4444)      | white (#FFFFFF)             | 5.3:1  | ✓ Pass AA       |
| Link        | primary-500 (#2563EB)    | white (#FFFFFF)             | 6.9:1  | ✓ Pass AA + AAA |

**Verified using:** WebAIM Contrast Checker (https://webaim.org/resources/contrastchecker/)

### Focus Order

Form inputs should be focusable in logical order (left-to-right, top-to-bottom):

```
1. First Name input
2. Last Name input
3. Email input
4. Phone input
5. Bio textarea
6. Avatar upload button
7. Save Changes button
8. Cancel button
```

Users can navigate this order with Tab key. Can reverse with Shift+Tab.

### ARIA Labels for Custom Components

| Component           | ARIA Label                                 | Why Needed                                            |
| ------------------- | ------------------------------------------ | ----------------------------------------------------- |
| **Avatar upload**   | `aria-label="Upload profile picture"`      | Icon-only button, needs text label for screen readers |
| **Error message**   | `role="alert" aria-live="polite"`          | Screen readers announce errors when they appear       |
| **Toast**           | `role="status" aria-live="polite"`         | Screen readers announce success message               |
| **Loading spinner** | `aria-hidden="true"`                       | Spinner is decorative, hide from screen readers       |
| **Tab panel**       | `role="tabpanel" aria-labelledby="tab-id"` | Associate panel with its tab button                   |

### Keyboard Navigation

- All interactive elements (buttons, inputs, links) must be keyboard accessible
- Tab order follows visual order (left-to-right, top-to-bottom)
- Escape key closes modals
- Enter key submits forms
- Focus indicator (blue outline) visible on all focused elements
- Focus outline: 2px solid, offset 2px (meets WCAG AAA)

### Screen Reader Testing

- Form labels must be associated with inputs using `<label for="input-id">` or `aria-labelledby`
- Required fields marked with `aria-required="true"`
- Error messages linked to inputs with `aria-describedby`
- Headings use semantic heading tags (h1, h2, h3) in logical order
- No empty headings or headings used for styling

---

## Assets and Icons

### Icon Specifications

| Icon Name          | Format | Size Variants    | Usage                         | Figma Link                                        |
| ------------------ | ------ | ---------------- | ----------------------------- | ------------------------------------------------- |
| **Alert Triangle** | SVG    | 16px, 20px, 24px | Error indicators              | [Link](https://figma.com/file/abc123?node-id=300) |
| **Check Circle**   | SVG    | 16px, 20px, 24px | Success states                | [Link](https://figma.com/file/abc123?node-id=301) |
| **Upload Cloud**   | SVG    | 24px, 32px       | Avatar upload trigger         | [Link](https://figma.com/file/abc123?node-id=302) |
| **Eye**            | SVG    | 20px             | Show/hide password toggle     | [Link](https://figma.com/file/abc123?node-id=303) |
| **Lock**           | SVG    | 20px             | Security icon in Security tab | [Link](https://figma.com/file/abc123?node-id=304) |

### Icon Export Specifications

- Format: SVG (scalable, crisp at any size)
- Color: Use CSS variable for color (not hardcoded)
- Stroke width: 2px (for consistency)
- Padding: 4px internal padding in icon viewBox
- Multiple weights: Regular (2px stroke) for most, Bold (3px) for emphasis

### Avatar Asset

- Format: Image file (PNG, JPG, WebP)
- Size variants: 40px (small), 80px (medium), 160px (large), 300px (XL)
- Auto-crop to 1:1 aspect ratio (square)
- File size: <200KB after compression
- Upload endpoint: `/api/profile/avatar`

---

## Engineering Notes

### Known Implementation Challenges

**Challenge 1: Avatar Cropping and Upload**

- Figma shows simple upload, but implementation needs client-side cropping
- Recommended: Use Croppie.js or similar library for client-side image manipulation
- Upload to server as base64 in request body, or use FormData with multipart/form-data
- Store in S3 or CDN, not in database directly

**Challenge 2: Form State Management**

- Form has unsaved changes (dirty state)
- If user navigates away with unsaved changes, show confirmation dialog
- If save fails, keep user's edits (don't reset form)
- Suggested: Use React Hook Form or Formik for state management

**Challenge 3: Concurrent Edit Detection**

- What if user edits profile in two browser tabs?
- Last write wins (or warn user if tab's data stale)?
- Consider: Read user data with version/timestamp, check if changed before save
- On conflict: Show error "Your profile was updated elsewhere, refresh to see latest"

**Challenge 4: Phone Number Validation and Formatting**

- Phone number field needs to handle multiple formats: +1-555-000-0000, (555) 000-0000, 5550000000
- Suggested: Use libphonenumber-js library for validation
- Store as E.164 format in database, display in user's locale format on UI

### Suggested Implementation Approach

**Frontend Stack:**

- Use React for UI (components: Form, Input, Button, Select)
- Use React Hook Form for form state management
- Use Axios or Fetch API for API calls
- Use Zustand or Redux for global state (user profile data)
- Use Tailwind CSS or CSS Modules for styling

**API Endpoints Needed:**

- `GET /api/profile` — Fetch current user profile
- `PATCH /api/profile` — Update profile (first name, last name, email, phone, bio)
- `POST /api/profile/avatar` — Upload avatar image
- `POST /api/password/change` — Change password
- `PATCH /api/notifications` — Update notification preferences

**Database Schema:**

```sql
CREATE TABLE user_profiles (
  user_id UUID PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL,
  email VARCHAR(100) NOT NULL,
  phone VARCHAR(20),
  bio VARCHAR(500),
  avatar_url VARCHAR(255),
  avatar_updated_at TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## Design Review Sign-Off Table

| Role                    | Name           | Email              | Status   | Date       | Notes                               |
| ----------------------- | -------------- | ------------------ | -------- | ---------- | ----------------------------------- |
| **Lead Designer**       | Sarah Chen     | sarah@company.com  | Approved | 2024-03-14 | Design complete, ready for dev      |
| **Design System Owner** | Marcus Wong    | marcus@company.com | Approved | 2024-03-13 | Uses DS 2.1 correctly, no conflicts |
| **Product Manager**     | Rachel Kim     | rachel@company.com | Approved | 2024-03-12 | Aligns with feature requirements    |
| **Accessibility Lead**  | Jamie Lee      | jamie@company.com  | Approved | 2024-03-13 | WCAG 2.1 AA verified                |
| **Engineering Lead**    | Alex Rodriguez | alex@company.com   | Approved | 2024-03-14 | Feasible to implement, no blockers  |

**Ready for Development:** YES (All sign-offs complete)

---

## Additional Notes

### Future Enhancements (Not in Scope)

- Two-factor authentication (2FA) setup UI
- Linked devices and session management with "Sign out from all devices"
- Data export feature (GDPR compliance)
- Account deletion with confirmation flow
- Billing history and invoice download

### Related Design Documents

- Design System documentation: https://design.company.com/docs
- Notification preferences detailed spec: (separate document)
- Security tab detailed spec: (separate document)
- Icon library: https://design.company.com/icons

### Questions for Developers

1. How should we handle avatar upload if image is >5MB? Show error before or after upload attempt?
2. Should form auto-save changes as user types (like Google Docs), or only on explicit "Save" click?
3. If user changes email, do we need email verification flow? Or trust user?
4. Should we show a "Changes saved at HH:MM" timestamp below form?

### Questions for Product

1. Should users be able to have multiple email addresses on file (primary + backup)?
2. Should we show subscription status on profile, or only on Billing tab?
3. Should avatar be editable without full profile form (separate upload button)?
