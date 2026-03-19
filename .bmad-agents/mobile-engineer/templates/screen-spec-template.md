# Mobile Screen Specification Template

## Header Information

| Field | Value |
|-------|-------|
| **Screen Name** | [e.g., "User Profile Screen"] |
| **Feature Epic** | [Link to Epic ID] |
| **Platform** | iOS / Android / Both |
| **Primary Owner** | [Mobile Engineer Name] |
| **Design Link** | [Figma Link] |
| **Last Updated** | [Date] |
| **Design Version** | [Design system version] |
| **Spec Status** | Draft / Review / Approved / Implemented |

---

## Screen Purpose and User Goal

**Purpose:**
[Clear, concise statement of what this screen is for. 2-3 sentences explaining the primary function.]

**User Goal:**
[What does the user want to accomplish on this screen? Frame from the user's perspective, not the system's.]

**Success Criteria:**
- [How the user knows they've successfully completed their goal]
- [What action/outcome confirms success]

---

## Entry Points

Describes all the ways a user can navigate to this screen.

| From Screen | Trigger | Navigation Style | Data Passed | Preconditions |
|------------|---------|------------------|-------------|---------------|
| Home Tab | Tap Profile Icon | Push (iOS) / Replace (Android) | user_id, user_profile_data | User logged in |
| Settings | Tap "View Profile" | Push / Replace | user_id | User logged in |
| Deep Link | App Link `app://user/123` | Deep link handler | user_id in URL | App installed, valid user_id |
| Push Notification | Tap notification | Deep link + data | profile_update_flag | App installed or launch required |

---

## Exit Points

Describes all the ways a user can navigate away from this screen.

| To Screen | Trigger | Navigation Style | Data Passed | Cleanup |
|-----------|---------|------------------|-------------|---------|
| Edit Profile | Tap "Edit" Button | Push / Navigate | Existing profile data | None — context preserved |
| Settings | Tap "Settings" in nav | Pop or Replace | None | None |
| Home | Tap Back/Up | Pop | None | Dismiss any open modals |
| Settings (Logout) | Tap "Logout" | Pop to Root | Logout signal | Clear cached profile data |
| Share Dialog | Tap Share Icon | Modal | profile_data | Dismiss on completion |

---

## UI Layout Description

### Header Section
- **Navigation Bar / App Bar Height:** 56dp (Android) / 44pt (iOS)
- **Elements:** Back button (left), Title "Profile" (center), Settings icon (right)
- **Background:** Primary brand color
- **Text Color:** White / High contrast
- **Safe Area Consideration:** Top safe inset on notched devices

### Body Section (Main Content)

**Profile Header Card**
- Profile picture (120x120pt / 120dp, circular)
- User name (headline, bold)
- Handle / username (subheading)
- Location / bio (caption)
- Edit profile button

**Stats Row**
- Followers count (centered, with label)
- Following count (centered, with label)
- Posts/items count (centered, with label)
- Scrollable if constrained width

**Tabbed Content**
- Posts tab (primary)
- Likes tab (secondary)
- Saved tab (tertiary)
- Tab height: 48dp (Android) / 44pt (iOS)

**Posts List**
- Scrollable list of user's posts
- Post card component (300pt width max recommended)
- Infinite scroll with pagination
- Pull-to-refresh support

### Footer / Action Bar Section
- Message button (primary action)
- Follow/Unfollow button (secondary)
- Share button (tertiary)
- More options menu (three-dot overflow)
- 60dp (Android) / Safe area adjusted (iOS)

### Optional: Floating Action Button (FAB)
- Position: Bottom-right corner with 16dp margin from edges
- Icon: [Compose/Message icon]
- Trigger: Opens message composer
- Platform-specific: Android native FAB, iOS uses action button

---

## Component Inventory

| Component | Type | Data Source | Interaction | Notes |
|-----------|------|-------------|-------------|-------|
| Profile Picture | Image | User API / Cache | Tap to view full photo (modal) | Use placeholder if missing |
| User Name | Text | User API | Non-interactive | Truncate if too long |
| Follow Button | Button | User API (follow status) | Tap to follow/unfollow; shows loading state | Async operation, optimistic UI |
| Stats Numbers | Text + Badge | User API | Tap to filter/navigate | Can tap to see followers list |
| Post List | RecyclerView (A) / LazyColumn (iOS) | Posts API | Scroll, pull-to-refresh, tap item | Pagination at end; infinite scroll |
| Post Card | Compound component | Posts API per item | Tap to open detail; long-press for actions | Include like/comment/share actions |
| Tab Bar | Segmented Control / Tabs | Local state | Tap to switch; smooth scroll animation | Indicator animation |
| Settings Icon | Icon Button | None | Tap to open profile settings | Should navigate to settings screen |
| Share Button | Button | Profile data | Tap to open share sheet | Native share dialog |

---

## Data Requirements

### API Endpoints Consumed

**GET /api/v2/users/{user_id}**
- **Request Parameters:**
  - `user_id` (path parameter): The user's unique identifier
  - `include_stats` (query, optional): Include follower/following counts
  - `include_profile_pic` (query, optional): Include profile picture URL
- **Response Fields Used:**
  - `id`: User ID
  - `name`: Display name
  - `handle`: Username
  - `bio`: User biography
  - `profile_pic_url`: URL to profile picture
  - `follower_count`: Number of followers
  - `following_count`: Number of accounts followed
  - `post_count`: Total posts
  - `is_following`: Boolean, whether current user follows this user
  - `is_blocked`: Boolean, whether blocked
- **Expected Response Time:** < 500ms (P95)
- **Cache Strategy:** Cache for 5 minutes; invalidate on user follow/unfollow

**GET /api/v2/users/{user_id}/posts**
- **Request Parameters:**
  - `user_id` (path): User ID
  - `limit` (query): Number of posts per page (default 20, max 100)
  - `offset` (query): Pagination offset
  - `sort` (query): 'recent' or 'popular'
- **Response Fields:**
  - Array of post objects: `id, content, created_at, likes, comments, shares`
  - `has_more` (boolean): Whether more posts available
- **Cache Strategy:** Cache paginated results; invalidate on new post creation
- **Expected Response Time:** < 600ms (P95)

**POST /api/v2/users/{user_id}/follow** / **DELETE /api/v2/users/{user_id}/follow**
- **Request:** No body (DELETE is unfollow)
- **Response:** `{ status: "success", follower_count: 1234 }`
- **Side Effects:** Real-time subscription via WebSocket should update counts

### Loading State

- **Initial Load:** Show skeleton loader with placeholder blocks (profile pic, name, stats, post cards)
- **Skeleton Duration:** Max 2 seconds before showing error state
- **Skeleton Components:**
  - Profile picture: Rounded gray box (120x120)
  - Name/Handle: Two horizontal bars
  - Stats: Three horizontal bars
  - Post cards: Five full-width card skeletons

### Empty State

- **When:** User has zero posts
- **Display:** Centered icon + message ("No posts yet")
- **Actions:** Button to "Create your first post" or back navigation
- **Illustration:** Use brand-approved empty state icon

### Error State

- **Network Error:** "Unable to load profile. Check your connection." with retry button
- **User Not Found (404):** "Profile not found or has been deleted"
- **Permission Error (403):** "This profile is private" with option to request access
- **Server Error (5xx):** "Something went wrong. Please try again." with retry + contact support
- **Retry Logic:** Exponential backoff (1s, 2s, 4s, 8s max)
- **Error Illustration:** Brand-approved error icon

---

## Platform-Specific Notes

### iOS-Specific Considerations

**Navigation Style:**
- Use UINavigationController with push navigation
- Back button auto-generated (shows previous screen title)
- Navigation bar uses `large` title style if space available
- Support swipe-to-pop gesture (default behavior)

**Safe Area & Layout:**
- Respect top safe inset for notched devices (iPhone X+)
- Respect bottom safe inset for home indicator
- Use `safeAreaLayoutGuide` or SwiftUI's `.ignoresSafeArea()` carefully
- Header extends behind safe area if full-bleed design intended

**Gestures:**
- Swipe from left edge to pop (system default, honor it)
- Long-press on post card to show preview
- Double-tap profile picture to view full-size
- Tap and hold follow button for confirmation sheet on critical unfollow

**VoiceOver (Accessibility):**
- Profile picture: "Profile picture for [User Name]"
- Stats: "[Number] followers, [Number] following"
- Follow button: "Follow [User Name], button" or "Following [User Name], button"
- Post cards: "[Post content summary], posted [time ago]"

---

### Android-Specific Considerations

**Navigation & Back Behavior:**
- Use Fragment for screen content; FragmentManager for backstack
- Back button invokes `popBackStack()`
- App bar title is "[User Name]'s Profile"
- Support Android's predictive back gesture (API 33+)

**Material Design & Layout:**
- Use Material 3 components (Material Design 3.0 spec)
- App bar background uses Material `colorPrimary`
- Floating Action Button follows Material 3 style (not Material 2)
- Respect system window insets (`WindowCompat`, `OnApplyWindowInsetsListener`)
- Edge-to-edge layout: extend content behind system bars where appropriate

**Gestures:**
- Swipe from left to pop (Material back gesture, API 33+)
- Long-press post for context menu (share, delete, report)
- Double-tap profile for zoom (if applicable)

**TalkBack (Accessibility):**
- Profile picture: "Profile image for [User Name]"
- Stats: "You have [number] followers, [number] following"
- Follow button: Announce as "Button, follow [User Name], double-tap to activate"
- Touch target size: Minimum 48dp x 48dp per Material spec
- Post cards: Announce as "Selectable item, [post summary], [time ago]"

---

## Offline Behavior

| Feature | Offline Behavior | Sync Strategy | User Feedback |
|---------|------------------|---------------|---------------|
| Profile Data Display | Show cached profile if available | Queue follow action; sync when online | Show connection banner at top |
| Follow/Unfollow Action | Optimistic UI update; queue action | Send to server on reconnection | Optimistic follow shown; queued indicator |
| Post List | Show cached posts from last session | Queue new post fetch; delta sync on reconnection | Show "Offline" badge on posts |
| Pull-to-Refresh | Disabled with message | Queued; retry on online | Toast: "You're offline" |
| Scroll to Bottom (Pagination) | Works with cached posts | Queue pagination request | Show "Offline" in load more prompt |

**Offline Storage:**
- Profile: Store in SQLite/Realm (iOS: Core Data)
- Posts: Store recent 100 posts locally; older posts require network
- Follow status: Store locally; sync flag tracks pending changes
- TTL: Profile cache expires 1 hour; posts expire 24 hours

---

## Accessibility Notes

### iOS VoiceOver Requirements

- All interactive elements must have descriptive `accessibilityLabel`
- Use `accessibilityHint` for non-obvious actions ("double-tap to follow")
- Profile picture labeled as image with user context
- Post count stats accessible and announced with label
- Forms: Use `accessibilityElement` grouping for related items
- Dynamic Type support: Text scales up to 200% accessibility size
- Contrast ratio: WCAG AA minimum 4.5:1 for text on background

### Android TalkBack Requirements

- All buttons, clickables must be labelled with `contentDescription`
- Labels should be 1-2 short words (e.g., "Follow" not "Tap this button to follow")
- Heading announcements for major sections (profile header, posts list)
- Reading order flows top-to-bottom, left-to-right
- Touch targets: 48dp x 48dp minimum per Material spec
- Contrast ratio: WCAG AA minimum 4.5:1 text to background

### Touch Target Minimums

- **iOS:** 44pt x 44pt (44x44 points = ~11x11 mm)
- **Android:** 48dp x 48dp (48 density-independent pixels = ~9x9 mm)
- Spacing: Minimum 8pt (iOS) / 8dp (Android) between interactive elements
- Profile picture tap target: 120pt / 120dp (well above minimum)

---

## Performance Requirements

### Time to Interactive (TTI)

- **Cold Start TTI:** Profile picture + name + stats visible in < 1.5 seconds
- **Warm Start TTI:** < 500 milliseconds
- **Post list first page:** Visible within 2 seconds
- **Measurement:** Use Perfetto (Android) / Instruments (iOS) Time Profiler

### Image Loading Strategy

- **Profile Picture:**
  - Dimensions: 120x120 pt (iOS) / 120x120 dp (Android)
  - Format: WebP preferred, JPEG fallback
  - Size target: < 30 KB
  - Placeholder: Gray skeleton while loading
  - Caching: Aggressive (cache until profile updated)
  - Library: Glide (Android), SDWebImage or Kingfisher (iOS)

- **Post Thumbnails:**
  - Load at medium resolution (300x200) initially
  - Lazy-load as user scrolls
  - Blur-up or progressive JPEG technique
  - Hard cache: 7 days
  - Soft cache: 1 hour

### Scroll Performance

- **Frame Rate Target:** 60 fps minimum (120 fps preferred on high-refresh devices)
- **Jank Budget:** < 5% frame drops during normal scrolling
- **List Size Limit:** Render window of ~20 visible + 10 buffer items
- **Recycler View (Android):** Use item pooling, disable nested scroll bounce
- **CollectionView (iOS):** Use cell reuse, monitor dequeueReusableCells
- **Measurement Tool:** Android Profiler > Frame metrics, Xcode Core Animation tool

---

## Analytics Events

Track user interactions and key user flows through this screen.

| Event Name | Trigger | Properties | Notes |
|------------|---------|-----------|-------|
| `profile_viewed` | Screen displayed | user_id, profile_owner_id, source (home/deeplink/notification) | Log on `viewDidAppear` / `onResume` |
| `profile_follow_clicked` | Tap follow button | user_id, profile_owner_id, current_follow_status | Before network call |
| `profile_follow_confirmed` | Follow action succeeds | user_id, profile_owner_id | On success response |
| `profile_follow_failed` | Follow action fails | user_id, profile_owner_id, error_code | On error response |
| `profile_post_tapped` | Tap post in list | user_id, post_id, post_index | For engagement tracking |
| `profile_post_liked` | Like post from profile | user_id, post_id | Real-time engagement |
| `profile_share_clicked` | Tap share button | user_id, profile_owner_id | Share intent initiated |
| `profile_settings_opened` | Tap settings icon | user_id | Navigates to settings |
| `profile_scroll_depth` | User scrolls past 50%, 75%, 100% | user_id, scroll_percentage | Engagement depth metric |
| `profile_error_shown` | Error state displayed | error_type (network/404/403/5xx), timestamp | For error monitoring |

---

## Test Scenarios

### Happy Path

1. **Load Profile Successfully**
   - Open app, navigate to profile
   - Profile loads within 1.5s
   - Picture, name, stats, posts all visible
   - No error state shown

2. **Follow User**
   - Tap follow button on another user's profile
   - Optimistic UI updates immediately
   - Server sync succeeds
   - Follow count increments
   - Button state changes to "Following"

3. **Scroll Through Posts**
   - Profile loads with first 20 posts
   - Scroll down at 60 fps
   - When 2 items remain, auto-load next page
   - No jank or frame drops
   - Images load progressively

4. **Return from Deep Link**
   - Tap deep link to profile in notification
   - App launches and navigates to correct profile
   - Back button returns to notification/home
   - Navigation stack is clean

### Edge Cases

1. **Very Long User Name / Bio**
   - Test with 200+ character name
   - Text truncates gracefully with ellipsis
   - Does not push UI elements off-screen
   - VoiceOver announces full text in label

2. **No Posts**
   - Empty state shown with appropriate icon
   - Call-to-action button visible
   - Scrolling doesn't crash; posts list is empty

3. **Low Bandwidth / Slow Network**
   - Profile loads but posts fail
   - Error state appears; user can retry
   - Cached posts show if available
   - Skeleton loader appears for 3+ seconds before error
   - Retry succeeds on reconnetion

4. **Offline Scenario**
   - User opens profile while offline
   - If cached: profile displays with offline badge
   - Follow action queued for later
   - Refresh disabled
   - No crash or hanging UI

### Error Cases

1. **User Not Found (404)**
   - Deep link to deleted user
   - "Profile not found" message shown
   - Back button available to return
   - No crash

2. **Server Error (5xx)**
   - Service temporarily down
   - Generic error message + retry button
   - Retry succeeds after service recovery
   - Error logged for monitoring

3. **Network Timeout**
   - Request takes > 10 seconds
   - Timeout error shown
   - User can retry manually
   - Exponential backoff prevents retry spam

4. **Blocked User**
   - User blocked; profile is private
   - "Cannot view profile" message
   - No follow action available
   - Graceful degradation of UI

5. **Corrupted Local Data**
   - SQLite database corrupted
   - Fallback to network fetch
   - If network fails: generic error
   - No crash; data not lost

---

## Handoff Notes for Engineers

- Design is available in Figma [link]. Inspect colors, spacing, fonts from design tool.
- Platform-specific guidelines (iOS HIG, Android Material) are authoritative; design may differ slightly per platform affordances.
- If design and specification conflict, request clarification in PR review; do not make assumptions.
- All analytics events are required for product insights; do not ship without them.
- Performance targets are not optional; profile timing should be measured in CI/CD pipeline.
- Offline support is critical for user retention; plan implementation early in sprint.
- Accessibility is not a nice-to-have; all WCAG AA criteria must be met before launch.

