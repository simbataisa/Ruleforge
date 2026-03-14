# Offline-First Mobile Architecture Patterns

A comprehensive reference for designing and implementing offline-first mobile applications with local-first data persistence, conflict-aware synchronization, and resilient network handling.

---

## Offline-First Core Principles

### 1. Local-First Data

**Principle:** Treat local device storage as the primary source of truth. Network sync is a feature, not the default path.

**Implementation:**
- All app data is stored locally in SQLite (Android) / Core Data (iOS) / Realm (both)
- UI reads from local storage immediately; no network wait
- Write operations: local first, then queue for sync
- Read-then-display pattern: Display cached data while fetching fresher version

**Benefits:**
- Instant UI feedback; no perceived lag
- Works in offline mode without code branches
- Reduced network traffic; only deltas synced
- Improved battery life (fewer radio wake-ups)

**Drawbacks:**
- Complexity in conflict resolution
- Storage space constraints on device
- Data consistency challenges across devices

### 2. Sync as a Feature

**Principle:** Synchronization is a user-facing feature, not an invisible background process.

**Implementation:**
- Expose sync state to UI: synced, syncing, pending, error, conflict
- Allow user control: manual refresh, pause sync, retry failed syncs
- Show sync indicators: spinner during sync, success confirmation, error alerts
- Queue transparency: show pending changes, allow user to view queue

**User Experience Patterns:**
- Pull-to-refresh: Manual sync trigger (iOS standard)
- Sync status badge: Small indicator showing sync state
- Pending changes indicator: "(Waiting to sync)" under edited items
- Conflict resolution UI: Show conflicting versions; let user pick winner

### 3. Conflict Resolution Strategy

**Principle:** Define a predictable, documented conflict resolution approach. Never silently lose data.

**Common Strategies:**
1. **Last-Write-Wins (LWW):** Latest timestamp always wins
   - Simplest; deterministic
   - Can lose concurrent edits
   - Works for low-conflict domains (user profiles, settings)

2. **Server-Wins:** Server version always overwrites client changes
   - Prevents client data from corrupting server state
   - Frustrating for users (their changes lost)
   - Use for financial/audit-critical data

3. **Client-Wins:** Client version always sent to server, then pulled back
   - User's latest intent is preserved
   - Can overwrite concurrent server changes (e.g., other device)
   - Use for user-centric data (notes, drafts, preferences)

4. **CRDT (Conflict-free Replicated Data Types):** Merge both versions mathematically
   - No data loss; both changes preserved
   - Complex to implement; requires special data structures
   - Excellent for collaborative editing (Google Docs style)
   - Libraries: Yjs, Automerge, Δ (Delta)

5. **Manual Resolution:** Show user both versions; let them choose
   - Most control; user can pick best option
   - Cognitive load on user
   - Use when conflicts are expected and important

---

## Data Sync Architectures Comparison

### Full Sync (Batch Download)

**Architecture:** Periodically download entire dataset from server; replace local copy.

```
[Local SQLite] ← [Download ALL] ← [Server]
                  Every N hours / on demand
```

**Pros:**
- Simple implementation; no delta tracking
- Clear consistency (local = server snapshot)
- Works for small datasets (< 1000 items)
- No conflict resolution needed (wholesale replace)

**Cons:**
- High bandwidth; downloads unchanged data repeatedly
- Large initial sync on first app install
- Slow on poor networks
- Cannot do offline edits (no queuing mechanism)
- All-or-nothing: if sync fails, no partial updates

**When to Use:**
- Static reference data (countries, categories, product catalog)
- Small datasets that rarely change (< 1 MB total)
- App initialization (download config on first launch)

---

### Delta Sync (Incremental Changes)

**Architecture:** Only sync items that changed since last sync; client tracks watermark/timestamp.

```
[Local SQLite] ← [Download changes since t0] ← [Server]
                  Version watermark: t0 → t1
```

**Pros:**
- Low bandwidth; only deltas transferred
- Scales to large datasets (millions of items)
- Fast on poor networks
- Supports offline edits with queue
- Incremental: partial success leaves good state

**Cons:**
- Requires server-side change tracking (timestamps, version vectors)
- Clock skew issues (client & server clocks must be loosely synced)
- More complex: need watermark management
- Conflict possible if edit made offline, also edited on server

**Implementation Patterns:**

1. **Timestamp-based:**
   - Server: track `updated_at` on all rows
   - Client: store `last_sync_timestamp`
   - Query: `GET /items?since=2024-03-01T10:30:00Z`
   - Vulnerable to clock skew; use server time for watermark

2. **Version Vector:**
   - Server: assign monotonic version ID per change
   - Client: store `last_seen_version = 12345`
   - Query: `GET /items?version_gt=12345`
   - More reliable than timestamps; immune to clock skew

**When to Use:**
- Dynamic datasets with frequent changes (user feeds, messages)
- Medium to large scale (1000 - 1M items)
- Acceptable conflict resolution overhead
- Mobile-first apps (bandwidth-conscious)

---

### Event Sourcing (Immutable Log)

**Architecture:** Store immutable events; replay to get current state. Sync events from server.

```
[Local Event Log] ← [Stream events from server] ← [Server Event Log]
         ↓
   [Replay engine]
         ↓
  [Current State Cache]
```

**Pros:**
- Complete audit trail; all changes visible
- Deterministic replay; easy to debug inconsistencies
- Natural fit for offline: replay local events first, then server events
- Strong consistency model; no surprise overwrites
- Conflict resolution explicit: merge or replay differently

**Cons:**
- Complex architecture; requires event schema design
- Storage: event log grows unbounded
- Replay cost: may replay thousands of events on each sync
- Requires all changes to emit events
- Need event versioning strategy for schema evolution

**Implementation Example:**

```
Event Structure:
{
  id: "uuid",
  aggregate_id: "item-123",
  event_type: "ItemCreated" | "ItemUpdated" | "ItemDeleted",
  timestamp: "2024-03-01T10:30:00Z",
  user_id: "user-456",
  payload: { title, description, ... },
  version: 1
}

Sync Flow:
1. Client has events 1-50 (last_synced_version: 50)
2. Server returns events 51-75 (new from server)
3. Client applies events 51-75 to rebuild state
4. Client adds local event 76 (offline edit)
5. Next sync: send event 76 to server
```

**When to Use:**
- Complex domains with rich history (financial transactions, project updates)
- Collaborative features (multiple users editing same item)
- Audit/compliance requirements (regulatory record-keeping)
- Apps that need undo/redo or version history
- High-stakes data (medical, legal, financial records)

---

## Local Storage Options Comparison

| Storage Solution | Platform | Use Case | Pros | Cons |
|------------------|----------|----------|------|------|
| **SQLite** | Both | Relational data, queries | ACID, efficient, well-tested, standard SQL | Storage size overhead, requires migrations |
| **Room** | Android | Relational data w/ type safety | Built on SQLite, compile-time SQL checking, Kotlin support | Android only, Room-specific learning curve |
| **Core Data** | iOS | Relational data, iCloud sync | Native iOS integration, iCloud CloudKit support | Complex API, migration challenges, slower than SQLite |
| **Realm** | Both | Document store, nested objects | Fast reads, intuitive API, automatic migrations, both platforms | Proprietary format, vendor lock-in, commercial licensing |
| **SharedPreferences** | Android | Small key-value data | Simple, fast, Android standard | Limited to primitives, not suitable for structured data |
| **UserDefaults** | iOS | Small key-value data | Simple, iCloud sync, standard iOS | Size limit (1 MB), not suitable for large datasets |
| **Secure Storage** | Both | Credentials, tokens, secrets | Hardware-backed encryption, protected from root access | Slow (not for high-volume reads), API differs iOS/Android |
| **File System** | Both | Media, large blobs, caches | Flexible, unlimited size, standard APIs | No transactions, no query language, manual cleanup |

### Recommended Combinations

**For Social/Messaging Apps:**
- Primary: SQLite (Android) / Realm (both) for messages, users, posts
- Cache: File system for images/media
- Secrets: Secure storage for auth tokens

**For Financial/Enterprise Apps:**
- Primary: Room (Android) / Core Data (iOS) for relational data
- Audit: Event log table in same database
- Secrets: Secure storage for API keys

**For Offline-Heavy Apps:**
- Primary: Realm for ease of migrations and multi-platform support
- Sync Queue: Separate SQLite table for pending changes
- Media: File system with manifest table
- Secrets: Secure storage for credentials

---

## Conflict Resolution Patterns

### Pattern 1: Last-Write-Wins with Timestamp

**When:** Low-conflict domains (user settings, profile updates, single editor)

**Implementation:**
```
Local change:        "John Doe" at 14:30:00
Server change:       "Jane Doe" at 14:29:00  (someone changed on web)

Conflict:            Both changed user name
Resolution:          Compare timestamps; 14:30:00 > 14:29:00
Winner:              Local version ("John Doe") synced to server
```

**Gotchas:**
- Clock skew: Ensure client time is synced (NTP or server-provided time)
- Concurrent edits at same millisecond: Use device ID as tiebreaker
- Timezone: Store times in UTC always

### Pattern 2: Server-Authoritative Merge

**When:** Financial data, audit records, server-controlled state (e.g., order status)

**Implementation:**
```
Local change:        Order quantity changed 5 → 10 (offline)
Server state:        Order cancelled (status = CANCELLED)

Sync attempt:        POST local change to server
Server response:     409 Conflict (order cancelled)
Resolution:          Server wins; local change discarded
Client displays:     "Order was cancelled before your change could sync"
```

**Pattern:**
1. Client posts change to server
2. Server validates against current state
3. If conflict: 409 response with server's current state
4. Client discards local change; pulls server state
5. Show toast explaining what happened

### Pattern 3: Operational Transformation (OT)

**When:** Collaborative editing (multiple users editing same doc)

**Implementation:**
```
Server state: "Hello World" (version 5)

Client A offline: Delete "H" → "ello World" (v5+local_seq_1)
Client B online:  Insert "!!" at end → "Hello World!!" (v6)

Sync flow:
1. Client A reconnects, posts change "delete(0)"
2. Server: Apply OT to transform A's op against B's op
3. Result: "ello World!!" (both edits preserved)
```

**Challenge:** Implement OT correctly is hard. Use library (Yjs, Automerge) instead of rolling custom.

### Pattern 4: CRDT-Based Merge

**When:** Conflict-free replication needed (no manual merge, no data loss)

**Data Structure Example (Last-Write-Wins Register):**
```
Register = {
  value: "John Doe",
  timestamp: 1609459200000,
  replica_id: "device-abc"
}

Merge rule:
if (other.timestamp > this.timestamp) {
  this = other
} else if (other.timestamp == this.timestamp && other.replica_id > this.replica_id) {
  this = other
} else {
  keep this
}

Result: Deterministic merge; both sides converge to same value
```

**Libraries:** Yjs (JavaScript), Automerge (multi-language), RGA (conflict-free lists)

### Pattern 5: Manual Resolution UI

**When:** High-value data where user needs to decide (collaborative docs, conflicting edits)

**UI Pattern:**
```
┌─────────────────────────────────────┐
│ Sync Conflict                       │
├─────────────────────────────────────┤
│ "Project Roadmap" has conflicting   │
│ changes on this device and another. │
│                                     │
│ Your version:     Mar 1, 10:30 AM   │
│ Latest version:   Mar 1, 10:45 AM   │
│                                     │
│ [View Your Version]                 │
│ [View Latest Version]               │
│ [Use Your Version]  [Use Latest]    │
└─────────────────────────────────────┘
```

**Implementation:**
1. Detect conflict (server rejection)
2. Show both versions side-by-side or in dialog
3. Highlight differences (diff algorithm)
4. User picks version or manually merges
5. Local queue updated with user's choice
6. Next sync sends resolved version

---

## Network State Detection

### iOS Implementation

**Reachability Framework:**
```swift
import Network

let monitor = NWPathMonitor()
monitor.pathUpdateHandler = { path in
  if path.status == .satisfied {
    print("Network available")
    // Trigger sync
  } else {
    print("Network unavailable")
    // Disable network operations
  }
}
```

**Considerations:**
- Reachability ≠ internet (may have WiFi but no ISP connection)
- Cellular cost: differentiate WiFi vs cellular data
- Don't assume network available based on reachability alone
- Always include timeout on network requests

### Android Implementation

**Network Callback:**
```kotlin
val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
  override fun onAvailable(network: Network) {
    Log.d("Network", "Available")
    // Trigger sync
  }

  override fun onLost(network: Network) {
    Log.d("Network", "Lost")
    // Pause network operations
  }
})
```

**Best Practices:**
- Check both connection type (WiFi/cellular) and actual connectivity
- Respect user's data saver preference (ConnectivityManager.isActiveNetworkMetered)
- Don't restart sync just because reachability changes (wait for 5s debounce)

### Handling Flaky Connections

**Exponential Backoff Strategy:**
```
Attempt 1: Immediate
Attempt 2: 1 second wait
Attempt 3: 2 seconds
Attempt 4: 4 seconds
Attempt 5: 8 seconds
Max: 10 retries before user intervention needed
```

**Implementation:**
```swift
func syncWithRetry(attempt: Int = 0) {
  let maxAttempts = 10
  let baseDelay = 1.0 // seconds

  performSync { result in
    switch result {
    case .success:
      // Clear retry queue; update UI
      break
    case .failure(let error):
      if attempt < maxAttempts {
        let delay = baseDelay * pow(2.0, Double(attempt))
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
          syncWithRetry(attempt: attempt + 1)
        }
      } else {
        // Show error to user; stop retrying
      }
    }
  }
}
```

---

## Background Sync Patterns

### iOS Background Tasks API (iOS 13+)

**Pattern:**
```swift
import BackgroundTasks

// In app delegate
func scheduleBackgroundSync() {
  let request = BGProcessingTaskRequest(identifier: "com.app.sync")
  request.requiresNetworkConnectivity = true
  request.requiresExternalPower = false // Allow on battery

  do {
    try BGTaskScheduler.shared.submit(request)
  } catch {
    print("Failed to schedule background task: \(error)")
  }
}

// Register handler
BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.app.sync") { task in
  handleBackgroundSync(task: task as! BGProcessingTask)
}

func handleBackgroundSync(task: BGProcessingTask) {
  let queue = DispatchQueue(label: "sync")

  task.expirationHandler = {
    queue.async {
      // Clean up in-flight sync
      self.syncQueue.cancelAllOperations()
    }
  }

  queue.async {
    self.performSync { success in
      task.setTaskCompleted(success: success)
      // Schedule next sync
      self.scheduleBackgroundSync()
    }
  }
}
```

**Constraints:**
- Runs 1-4 times per day in normal operation
- Device must be on WiFi and plugged in (unless requiresExternalPower = false)
- User may disable in Settings > General > Background App Refresh
- Max execution time: 30 seconds
- Cannot guarantee exact timing

### Android WorkManager

**Pattern:**
```kotlin
import androidx.work.*

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    return@withContext try {
      performSync() // Blocking call
      Result.success()
    } catch (e: Exception) {
      if (runAttemptCount < 3) {
        Result.retry()
      } else {
        Result.failure()
      }
    }
  }
}

// Schedule periodic sync (15 minutes minimum)
PeriodicWorkRequestBuilder<SyncWorker>(
  15, TimeUnit.MINUTES
).setConstraints(
  Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresDeviceIdle(false)
    .setRequiresBatteryNotLow(true)
    .build()
).build().also {
  WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "sync",
    ExistingPeriodicWorkPolicy.KEEP,
    it
  )
}
```

**Advantages:**
- Batched: Android schedules work efficiently across apps
- Respects Doze mode and Data Saver
- Automatic retry with exponential backoff
- Can run on WiFi or cellular

**Constraints:**
- Minimum 15-minute interval
- May be delayed if battery low or device idle
- User can disable via Battery Optimization settings

### Battery Optimization Tips

1. **Batch requests:** Sync multiple items in one API call, not per-item
2. **Delta-only:** Only sync changes, not full dataset
3. **Compress:** Use gzip compression on requests/responses
4. **Intervals:** Don't sync more than 30 minutes; user won't expect real-time
5. **WiFi-only for large:** Defer large syncs until WiFi available
6. **Backoff:** Exponential backoff prevents constant wake-ups
7. **Monitoring:** Track battery impact in testing (battery-drain profiler)

---

## Optimistic UI Pattern

**Goal:** Show immediate feedback to user even though server hasn't confirmed change yet.

**Flow:**

```
User taps "Like" button
    ↓
[IMMEDIATE] Update local SQLite + UI shows heart filled
    ↓
[QUEUED] Sync worker picks up change
    ↓
[SENDING] POST /posts/123/like to server
    ↓
Server responds 200 OK
    ↓
[SUCCESS] Heart stays filled; sync complete
    OR
Server responds 409 Conflict
    ↓
[ROLLBACK] Undo local change; show error toast
    ↓
"Failed to like. Try again?"
```

**Implementation (iOS):**
```swift
func likePost(_ postId: String) {
  // 1. Optimistic update
  var post = localDB.getPost(postId)
  post.isLiked = true
  post.likesCount += 1
  localDB.save(post)
  updateUI() // UI updates immediately

  // 2. Queue sync
  let change = SyncChange(type: .likePost, postId: postId)
  syncQueue.append(change)

  // 3. Async server call
  apiService.likePost(postId) { result in
    switch result {
    case .success:
      // Server confirmed; sync complete
      syncQueue.remove(change)
      break
    case .failure:
      // Rollback
      post.isLiked = false
      post.likesCount -= 1
      localDB.save(post)
      updateUI()
      showError("Failed to like")
    }
  }
}
```

**Key Points:**
- Update local storage and UI first (instant feedback)
- Send to server async (don't block UI)
- On failure: rollback and show error
- Show "syncing" indicator if sync takes > 1 second
- Don't block user from other actions while syncing

**Anti-Patterns:**
- Waiting for server before updating UI (defeats purpose)
- Not showing sync state (user thinks it worked, actually failed)
- Not rolling back on failure (data inconsistency)
- Allowing new edits while rollback in progress (complex merge)

---

## Sync State Machine

**States:**

```
            [SYNCED]
               ↑
               │ (local changes made)
               ↓
            [PENDING] ← user makes change offline
               ↓
           [SYNCING] ← background worker picks up
               ↓
         [SYNCED] or [ERROR] or [CONFLICT]
               ↓ (retry)
            [SYNCING]
```

**State Definitions:**

| State | Meaning | UI Indicator | User Can Edit | Can Sync |
|-------|---------|--------------|---------------|----------|
| **SYNCED** | All local changes confirmed on server | None (green dot optional) | Yes | No (nothing to sync) |
| **PENDING** | Local changes queued; waiting for sync | Clock/hourglass icon | Yes | Yes (on reconnect) |
| **SYNCING** | Actively sending to server | Spinner | No (disabled) | No (already in progress) |
| **ERROR** | Sync failed (network/server error); retry queued | Red X icon | Yes | Yes (auto-retry soon) |
| **CONFLICT** | Change rejected by server; manual resolution needed | Conflict badge | No (locked) | No (needs user input) |

**Transitions Table:**

| From | To | Trigger | Action |
|------|----|---------| --------|
| SYNCED | PENDING | User edits item locally | Update local DB; add to queue |
| PENDING | SYNCING | Background worker starts | Begin POST/PATCH to server |
| SYNCING | SYNCED | Server returns 2xx | Update sync timestamp; dequeue |
| SYNCING | ERROR | Server returns 5xx or network timeout | Log error; schedule retry (exponential backoff) |
| SYNCING | CONFLICT | Server returns 409 Conflict | Store conflict metadata; show conflict UI |
| ERROR | SYNCING | Retry timer fired | Attempt sync again |
| ERROR | PENDING | User makes new edit | Add to queue |
| CONFLICT | PENDING | User resolves conflict | Merge or choose version; re-queue |

**Implementation (Pseudocode):**
```
struct SyncItem {
  id: UUID
  state: SyncState
  localChanges: Data
  lastError: Error?
  retryCount: Int
  conflictMetadata: Conflict?
}

enum SyncState {
  case synced
  case pending
  case syncing
  case error
  case conflict
}

func transition(item: SyncItem, trigger: Trigger) -> SyncItem {
  switch (item.state, trigger) {
  case (.synced, .userEdit):
    return item.copy(state: .pending)
  case (.pending, .syncStart):
    return item.copy(state: .syncing)
  case (.syncing, .serverSuccess):
    return item.copy(state: .synced)
  case (.syncing, .serverError):
    return item.copy(state: .error, retryCount: item.retryCount + 1)
  case (.syncing, .serverConflict(let conflict)):
    return item.copy(state: .conflict, conflictMetadata: conflict)
  // ... more cases
  default:
    return item // No transition
  }
}
```

---

## Queue-Based Mutation Pattern

**Architecture:** All mutations go through a queue; background worker drains queue → sends to server.

**Benefits:**
- Offline edits preserved; sent when network returns
- User sees immediate feedback (optimistic)
- Serialized mutations prevent lost updates
- Easy to persist queue (survive app crash)

**Flow Diagram (Text):**
```
User Action (edit, like, delete)
         ↓
[LOCAL STORAGE UPDATE] (immediate; optimistic UI)
         ↓
[QUEUE ENTRY CREATED]
  {
    id: uuid,
    type: "updateProfile",
    payload: { name: "New Name" },
    timestamp: now(),
    retries: 0
  }
         ↓
[BACKGROUND WORKER TRIGGERED]
  (via WorkManager / BGProcessingTask)
         ↓
[NETWORK CHECK]
  if offline → wait for network
  if online → proceed
         ↓
[DEQUEUE NEXT ITEM]
         ↓
[SEND TO SERVER]
  POST /api/mutations
  body: { type, payload }
         ↓
[AWAIT RESPONSE]
  200 OK → mark complete; dequeue
  400 Bad Request → log error; dequeue (don't retry)
  409 Conflict → store conflict metadata; pause queue (user resolution)
  500 Server Error → schedule retry; exponential backoff
  Network Timeout → retry with backoff
         ↓
[UPDATE LOCAL STATE]
  on success: clear optimistic flag
  on error: keep for retry
         ↓
[REPEAT UNTIL QUEUE EMPTY]
```

**Database Schema (SQLite):**
```sql
CREATE TABLE sync_queue (
  id TEXT PRIMARY KEY,
  mutation_type TEXT NOT NULL,
  payload TEXT NOT NULL, -- JSON
  created_at INTEGER,
  retry_count INTEGER DEFAULT 0,
  last_error TEXT,
  status TEXT DEFAULT 'pending', -- pending|syncing|completed|failed|conflict
  conflict_metadata TEXT, -- JSON if conflict
  FOREIGN KEY (target_id) REFERENCES items(id)
);

CREATE INDEX idx_status ON sync_queue(status);
CREATE INDEX idx_created_at ON sync_queue(created_at);
```

**Implementation Considerations:**

1. **Queue Ordering:** FIFO (first-in-first-out) for causality; if Item A depends on Item B, process B first
2. **Batch Processing:** Group mutations of same type to reduce API calls
3. **Idempotency:** Each mutation has stable ID; server should be idempotent (same ID sent twice = safe)
4. **Partial Failure:** If mutation 3 of 5 fails, continue with 4 & 5; don't block queue
5. **Dead Letter:** After N retries (e.g., 5), move to dead-letter queue; alert user

---

## Encryption and Security for Local Storage

### iOS: Data Protection Classes

**Concept:** iOS encrypts data at rest using device-specific keys. Data Protection classes determine when decryption is available.

| Class | Accessibility | Use Case |
|-------|---|----------|
| **Complete** | Only when device unlocked | Sensitive: passwords, auth tokens, private messages |
| **Complete Unless Open** | After first unlock; stays open until device locked | App data that should be accessible while using device |
| **Complete Until First User Auth** | After device first unlock | App data that can be encrypted until first unlock |
| **None** | Always accessible | Cache, temporary files, non-sensitive data |

**Implementation (Swift):**
```swift
import Security

func saveSecureData(_ data: Data, key: String) {
  let query: [String: Any] = [
    kSecClass as String: kSecClassGenericPassword,
    kSecAttrAccount as String: key,
    kSecValueData as String: data,
    kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
  ]

  SecItemAdd(query as CFDictionary, nil)
}

func retrieveSecureData(key: String) -> Data? {
  let query: [String: Any] = [
    kSecClass as String: kSecClassGenericPassword,
    kSecAttrAccount as String: key,
    kSecReturnData as String: true
  ]

  var result: AnyObject?
  SecItemCopyMatching(query as CFDictionary, &result)
  return result as? Data
}
```

### Android: Keystore and EncryptedSharedPreferences

**AndroidKeyStore:** Hardware-backed encryption on capable devices.

```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

val masterKey = MasterKey.Builder(context)
  .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
  .build()

val securePrefs = EncryptedSharedPreferences.create(
  context,
  "secret_shared_prefs",
  masterKey,
  EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
  EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Use like normal SharedPreferences
securePrefs.edit()
  .putString("auth_token", token)
  .apply()
```

### SQLCipher: Encrypted Database (Cross-Platform)

**For apps with high security requirements (medical, financial):**

```kotlin
// Android Room + SQLCipher
val db = Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
  .openHelperFactory(
    SupportSQLiteOpenHelperFactory(passphrase = "password".toByteArray())
  )
  .build()
```

**Key Management:**
- Never hardcode passphrases in code
- Derive from user's PIN/password (PBKDF2, scrypt)
- Rotate keys on user password change
- Clear from memory after use (byte arrays)

---

## Testing Offline Scenarios

### Network Mocking (iOS)

**URLProtocol Stubbing:**
```swift
class URLProtocolMock: URLProtocol {
  static var responseData: Data?
  static var responseError: Error?

  override class func canInit(with request: URLRequest) -> Bool {
    return true
  }

  override class func canonicalRequest(for request: URLRequest) -> URLRequest {
    return request
  }

  override func startLoading() {
    if let error = Self.responseError {
      client?.urlProtocol(self, didFailWithError: error)
    } else if let data = Self.responseData {
      client?.urlProtocol(self, didLoad: data)
      client?.urlProtocolDidFinishLoading(self)
    }
  }

  override func stopLoading() {}
}

// In test
URLProtocol.registerClass(URLProtocolMock.self)
URLProtocolMock.responseData = mockJSON.data(using: .utf8)
```

### Network Mocking (Android)

**OkHttp MockInterceptor:**
```kotlin
class MockInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = when {
      chain.request().url.toString().contains("/users") ->
        Response.Builder()
          .code(200)
          .message("OK")
          .body(ResponseBody.create(null, mockUserJson))
          .build()
      else -> chain.proceed(chain.request())
    }
    return response
  }
}

// In test setup
val mockClient = OkHttpClient.Builder()
  .addInterceptor(MockInterceptor())
  .build()
```

### Airplane Mode Testing

**Manual Testing:**
1. Enable Airplane Mode on device
2. Open app; verify cached data shows
3. Make edit (offline); verify queued
4. Disable Airplane Mode
5. Verify sync triggers; change syncs successfully
6. Check server state matches

**Automated Testing:**
- Cannot directly enable airplane mode in tests
- Instead: mock NetworkManager.isNetworkAvailable() = false
- Simulate reconnection by changing to true + triggering sync

### Conflict Simulation

**Test Pattern:**
```swift
func testConflictResolution() {
  // 1. Client has item version 1
  let item = Item(id: "1", name: "Original", version: 1)
  localDB.save(item)

  // 2. Simulate offline edit
  item.name = "Edited Locally"
  localDB.save(item)
  syncQueue.append(item)

  // 3. Simulate server-side change (version 2)
  // Mock server returns 409 with server state
  let mockResponse = """
    {
      "status": "conflict",
      "client_version": 1,
      "server_version": 2,
      "server_state": { "id": "1", "name": "Edited Elsewhere", "version": 2 }
    }
  """

  // 4. Perform sync
  syncManager.sync()

  // 5. Assert conflict state
  XCTAssertEqual(syncManager.getState(itemId: "1"), .conflict)

  // 6. User resolves (chooses server version)
  syncManager.resolveConflict(itemId: "1", useServerVersion: true)

  // 7. Assert resolved
  let resolved = localDB.getItem("1")
  XCTAssertEqual(resolved.name, "Edited Elsewhere")
  XCTAssertEqual(syncManager.getState(itemId: "1"), .pending)
}
```

---

## Performance Considerations

### Incremental Sync Strategy

**Problem:** If user has 10,000 messages, syncing all 10,000 every session is slow.

**Solution: Paginated Delta Sync**
```
Session 1: Sync messages 0-100 (version_gt: 0)
Session 2: Sync messages 100-200 (version_gt: 100)
Session 3: Sync messages 200-300 (version_gt: 200)
...
Session 100: Sync messages 9900-10000

Then mark sync complete for this session.
```

**Implementation:**
```swift
let pageSize = 100

func syncMessagesIncremental(fromVersion: Int = 0) async {
  var currentVersion = fromVersion

  while true {
    let response = try await apiService.getMessages(
      sinceVersion: currentVersion,
      limit: pageSize
    )

    // Save to local DB
    localDB.saveMessages(response.messages)

    if response.messages.count < pageSize {
      // Last page; sync complete
      userDefaults.set(response.maxVersion, forKey: "lastSyncVersion")
      break
    }

    currentVersion = response.messages.last?.version ?? currentVersion

    // Yield to main thread to keep UI responsive
    await Task.yield()
  }
}
```

### Bandwidth Budget

**Goal:** Minimize data transferred on cellular networks.

**Strategies:**

1. **Response Compression:** gzip compression (typically 70% reduction)
   ```
   Request Header: Accept-Encoding: gzip
   Response Header: Content-Encoding: gzip
   ```

2. **Selective Fields:** Only request fields you need
   ```
   GET /users/123?fields=name,email,profile_pic_url
   ```

3. **Image Optimization:**
   - WebP format (25-35% smaller than JPEG)
   - Progressive loading (low-res placeholder first)
   - Lazy load images below fold
   - Adaptive sizing (send 1x vs 2x vs 3x based on device)

4. **Incremental Images:**
   ```
   First load: thumbnail (50x50, 5 KB)
   In background: full-res (1000x1000, 150 KB) only if user taps
   ```

5. **Batch API Calls:**
   ```
   Bad:  GET /items/1, GET /items/2, GET /items/3 (3 requests)
   Good: GET /items?ids=1,2,3 (1 request)
   ```

### Sync Batching

**Flush sync queue periodically, not per-item:**
```
Item 1 queued at t=0
Item 2 queued at t=0.5s
Item 3 queued at t=1.2s

At t=1.5s: Send all 3 in one request (1 network call vs 3)
```

**Implementation:**
```swift
func queueChange(_ change: SyncChange) {
  syncQueue.append(change)

  // Debounce: wait 1s before sending
  syncDebounceTimer?.invalidate()
  syncDebounceTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: false) { _ in
    self.flushSyncQueue()
  }
}

func flushSyncQueue() {
  let changes = syncQueue.removeAll()
  apiService.sendMutations(changes)
}
```

---

## Summary Checklist

- [ ] Define conflict resolution strategy for your domain
- [ ] Choose sync architecture: full, delta, or event sourcing
- [ ] Pick local storage: SQLite, Realm, Core Data, or Room
- [ ] Implement network state detection (iOS Network framework, Android ConnectivityManager)
- [ ] Add exponential backoff for retries
- [ ] Design and test sync state machine
- [ ] Implement optimistic UI with rollback
- [ ] Set up background sync (iOS BGProcessingTask, Android WorkManager)
- [ ] Test offline scenarios (airplane mode, network mocking)
- [ ] Simulate conflict cases in test suite
- [ ] Measure battery impact during heavy sync
- [ ] Monitor sync latency and error rates in production
- [ ] Document sync architecture for your team
- [ ] Set up encryption for sensitive data (tokens, passwords)

