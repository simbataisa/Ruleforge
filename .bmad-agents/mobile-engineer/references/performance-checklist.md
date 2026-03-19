# Mobile Performance Checklist and Reference

A comprehensive reference for measuring, optimizing, and maintaining high performance across iOS and Android mobile applications.

---

## Startup Performance

### Goals

- **Cold Start (app not in memory):** < 400ms to first frame (iOS), < 500ms (Android)
- **Warm Start (app in memory, backgrounded):** < 200ms to first frame
- **First Contentful Paint (FCP):** < 1.5 seconds
- **Time to Interactive (TTI):** < 3 seconds

### Splash Screen Best Practices

**What is a Splash Screen?**
A branded screen shown while app initializes. Can be native (instant) or custom (1-2 second fade).

**iOS (Native Splash):**
```
Storyboard Launch Screen
├── Company logo (centered)
├── App name or tagline
└── Transition to app automatically

Timing: Instantaneous; part of system launch
```

**Guidelines:**
- Use static image; no loading animations during native splash
- Match app's color scheme and branding
- Show after 1-2 seconds automatically (no long splash screens)
- Do NOT use splash to initialize heavy work

**Android (SplashScreen API):**
```kotlin
// In AndroidManifest.xml
<activity
  android:name=".MainActivity"
  android:theme="@style/Theme.App.SplashScreen">
  ...
</activity>

// In styles.xml
<style name="Theme.App.SplashScreen" parent="Theme.SplashScreen">
  <item name="windowSplashScreenBackground">@color/brand_color</item>
  <item name="windowSplashScreenAnimatedIcon">@drawable/logo</item>
  <item name="postSplashScreenTheme">@style/Theme.App</item>
</style>
```

### Deferred Initialization

**Principle:** Initialize only what's needed to show first screen. Defer heavy operations.

**App Startup Sequence (Correct Order):**

```
PHASE 1: System Launch (handled by OS)
  └─ 100ms

PHASE 2: App Process Start
  ├─ Parse DEX/dylib (50ms)
  ├─ Create Application object (20ms)
  └─ Run Application.onCreate() (30ms)
      └─ Total: ~100ms

PHASE 3: Activity Creation (what matters)
  ├─ onCreate()
  │  ├─ Inflate layouts (50ms) — do NOT inflate entire backstack
  │  ├─ Initialize UI (30ms) — only visible screen
  │  └─ Start essential services (20ms)
  ├─ onStart() (10ms)
  └─ onResume() (10ms)
      └─ First frame visible: ~120ms

PHASE 4: Post-Launch (Background Tasks)
  ├─ Analytics initialization (async)
  ├─ Crash reporting (async)
  ├─ Sync manager (async)
  ├─ Deep learning models (async)
  └─ All expensive work (deferred)
```

**What to Initialize in onResume():**
- Show cached UI (from last session)
- Start UI animations
- Request permissions if needed
- Fetch fresh data (async, don't block)

**What to Defer (Schedule Async):**
- Crash reporting SDK (Sentry, Firebase)
- Analytics SDK (Mixpanel, Amplitude)
- Remote config download
- Deep learning model loading
- Ad SDK initialization
- WebSocket connections

**Implementation (Android):**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContentView(R.layout.activity_main)

  // Essential: Show cached UI
  showCachedUser()
  showCachedPosts()

  // Defer heavy work
  Handler(Looper.getMainLooper()).postDelayed({
    initializeCrashReporting()
    loadDeepLearningModels()
    warmUpWebSocket()
  }, 1000) // 1 second after app shows
}
```

**Implementation (iOS):**
```swift
override func viewDidLoad() {
  super.viewDidLoad()

  // Essential
  loadCachedProfileImage()
  showCachedFeed()

  // Defer
  DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
    self.initializeCrashReporting()
    self.startBackgroundSync()
    self.preloadDeepLearningModels()
  }
}
```

### Measurement: Cold Start Profiling

**Android Profiler:**
```
1. Open Android Profiler (Profiler tab)
2. Start new session on device
3. Kill app completely
4. Press record; tap app icon
5. Wait 3 seconds; press stop
6. Timeline shows: Process creation, Activity creation, first frame
7. Look for "Display#firstDrawFrame" marker
```

**iOS Instruments:**
```
1. Xcode → Product → Profile
2. Select "App Launch" template
3. Choose device; press Record
4. Instruments auto-stops at first frame
5. Look for main() → applicationDidFinishLaunching:
6. Total time = cold start time (target: < 400ms)
```

**What to Look For:**
- Main thread blocking (should be < 50ms per frame, 60fps = 16.67ms)
- Excessive disk I/O (SQLite queries on main thread)
- Large memory allocations (GC pause visible on timeline)
- Image decoding (should be off main thread)

---

## Rendering Performance

### Frame Rate Targets

| Device Type | Target FPS | Frame Budget | Platform |
|-------------|-----------|--------------|----------|
| Standard phone (60Hz display) | 60 fps | 16.67 ms per frame | iOS, Android |
| High-refresh phone (120Hz display) | 120 fps | 8.33 ms per frame | iOS 13+, Android 10+ |
| Tablet (60Hz) | 60 fps | 16.67 ms per frame | Both |

**What Happens If You Miss Budget:**
- Miss 1 frame (16.67ms): User doesn't notice
- Miss 2+ frames: Visible stutter / jank
- Miss 10+ frames: Obvious lag, poor UX
- Miss 50+ frames: Animation stutters across screen

### Avoiding Jank: Android

**Problem 1: Overdraw**

Overdraw = drawing same pixel multiple times in one frame. Wastes GPU cycles.

**Example (BAD):**
```kotlin
// Activity has white background
// Fragment has white background
// ListView rows have white background
// = 3 layers of white
```

**Detection:**
```
Settings → Developer Options → Debug GPU Overdraw
├─ Blue: 1x overdraw (acceptable)
├─ Green: 2x overdraw (caution)
├─ Red: 3x+ overdraw (problem)
└─ Dark red: 4x+ overdraw (severe)
```

**Fix:**
```kotlin
// Remove redundant backgrounds
<fragment
  android:id="@+id/fragment"
  android:name="com.example.MyFragment"
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  android:background="@null" /> <!-- Remove if Activity already has background -->
```

**Problem 2: Expensive Layout Passes**

Layout inflation is O(n) where n = layout depth. Deep hierarchies are slow.

**Bad Layout (Slow):**
```xml
<FrameLayout>
  <FrameLayout>
    <LinearLayout>
      <FrameLayout>
        <FrameLayout>
          <TextView /> <!-- 5 layers deep! -->
        </FrameLayout>
      </FrameLayout>
    </LinearLayout>
  </FrameLayout>
</FrameLayout>
```

**Good Layout (Fast):**
```xml
<FrameLayout>
  <LinearLayout>
    <TextView /> <!-- 2 layers deep -->
  </LinearLayout>
</FrameLayout>
```

**Detection:** Android Layout Inspector → measure layout inflation time

**Problem 3: RecyclerView Performance**

```kotlin
// Bad: Heavy onBindViewHolder
override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
  val item = data[position]

  // These are SLOW on every scroll:
  holder.image.setImageDrawable(loadImageFromDisk(item.imagePath)) // Disk I/O!
  holder.text.text = expensiveStringFormatting(item.name) // CPU work!
  holder.shimmer.startShimmerAnimation() // Animation every frame!

  // This is called 60 times per second during fast scroll!
}

// Good: Lightweight binding
override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
  val item = data[position]

  // These are FAST:
  holder.image.setImageUrl(item.imageUrl) // Image library handles async
  holder.text.text = item.name // Simple assignment
  // Don't start animation; it's already running
}
```

### Avoiding Jank: iOS

**Problem 1: Offscreen Rendering**

Rendering content outside visible bounds, then clipping = wasted GPU work.

**Example (BAD):**
```swift
// Creates shadow by rasterizing, then clips
view.layer.cornerRadius = 10
view.layer.masksToBounds = true
view.layer.shadowColor = UIColor.black.cgColor
view.layer.shadowOpacity = 0.5
// ^ Renders shadow, then clips it = inefficient
```

**Fix: Use CAShapeLayer for shadows:**
```swift
let shadowPath = UIBezierPath(
  roundedRect: view.bounds,
  cornerRadius: 10
)
view.layer.shadowPath = shadowPath // GPU uses this path, doesn't render outside
view.layer.shadowColor = UIColor.black.cgColor
view.layer.shadowOpacity = 0.5
```

**Problem 2: Rasterization Overhead**

Rasterization converts vector drawing to bitmap. Useful sometimes, but overuse kills performance.

**Bad:**
```swift
// Rasterizing on every scroll? That's wrong.
override func scrollViewDidScroll(_ scrollView: UIScrollView) {
  complexView.layer.shouldRasterize = true // Rasterizes EVERY scroll frame!
}
```

**Good:**
```swift
// Rasterize only complex static hierarchies
override func viewDidLoad() {
  super.viewDidLoad()
  complexStaticView.layer.shouldRasterize = true // Once, not repeatedly
}
```

**Problem 3: UICollectionView & UITableView**

```swift
// Bad: Creating views in cellForRow
override func collectionView(_ collectionView: UICollectionView,
                           cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
  let cell = UICollectionViewCell()

  // These are SLOW:
  cell.addSubview(UIImageView()) // Creating view every scroll!
  cell.addSubview(UILabel()) // Allocating every frame!

  return cell
}

// Good: Reuse cells with pre-built views
override func collectionView(_ collectionView: UICollectionView,
                           cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
  let cell = collectionView.dequeueReusableCell(
    withReuseIdentifier: "cell",
    for: indexPath
  ) as! ImageCell

  // Just bind data; views already exist
  cell.imageView.image = UIImage(url: items[indexPath.item].imageUrl)
  cell.titleLabel.text = items[indexPath.item].title

  return cell
}
```

### Scroll Performance: Compose & SwiftUI

**Compose (Android):**
```kotlin
// Lazy column handles off-screen item disposal automatically
LazyColumn {
  items(largeList.size) { index ->
    PostCard(largeList[index]) // Only visible + buffer items rendered
  }
}
```

**SwiftUI (iOS):**
```swift
// LazyVStack does not recycle (watchout!)
// Use ScrollView + LazyVStack for long lists
ScrollView {
  LazyVStack {
    ForEach(items) { item in
      PostCard(item) // iOS 14+: views not discarded off-screen
    }
  }
}

// Better: Use List (optimized)
List(items) { item in
  PostCard(item) // Recycles views
}
```

**Compose vs SwiftUI:** Compose auto-recycles; SwiftUI's LazyVStack does not. Use List in SwiftUI for performance.

---

## Memory Management

### Memory Leak Patterns

**Pattern 1: Retain Cycles (Strong Reference Loops)**

```swift
// BAD: Closure captures self, self holds closure
class ViewController {
  var callback: (() -> Void)?

  func setupCallback() {
    callback = {
      self.doSomething() // Retain cycle!
      // ViewController → callback → self (circular)
    }
  }
}

// GOOD: Use [weak self]
func setupCallback() {
  callback = { [weak self] in
    self?.doSomething() // self is released when ViewController deinit
  }
}
```

**Pattern 2: Timer Memory Leaks**

```swift
// BAD: Timer holds strong reference to self
class ViewController {
  var timer: Timer?

  override func viewDidLoad() {
    super.viewDidLoad()
    timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
      self.updateUI() // Retain cycle!
    }
  }

  deinit {
    // Timer still holds self; deinit never called!
  }
}

// GOOD: Invalidate timer on deinit
deinit {
  timer?.invalidate()
  timer = nil
}
```

**Pattern 3: Delegate Memory Leaks**

```swift
// BAD: Delegate holds strong reference
class ViewController: UITableViewDelegate {
  let tableView = UITableView()

  override func viewDidLoad() {
    tableView.delegate = self // tableView → self → tableView (cycle)
  }
}

// GOOD: Apple's UITableViewDelegate is declared `weak` in UITableView
// This is handled correctly by system; only problematic for custom delegates

// If you have custom delegate:
class MyViewController: MyCustomDelegate {
  weak var delegate: MyDelegateProtocol?
  // ^ Mark delegate as weak
}
```

### Image Caching Strategy

**Problem:** Loading large images every time = memory spike + slow scrolling

**Solution: Tiered Caching**
```
User requests image
  ↓
[L1] Memory cache (fast, limited size)
  ├─ Hit: Use immediately
  └─ Miss: Check L2
  ↓
[L2] Disk cache (slow, large size)
  ├─ Hit: Load into memory; use
  └─ Miss: Fetch from network
  ↓
[L3] Network (slowest, unlimited)
  ├─ Download
  ├─ Store in L2 (disk)
  ├─ Load into L1 (memory)
  └─ Use
```

**Android Implementation (Glide):**
```kotlin
Glide.with(context)
  .load(imageUrl)
  .override(800, 600) // Exact size; don't load 4K
  .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // L2 cache
  .skipMemoryCache(false) // L1 cache enabled
  .into(imageView)
```

**iOS Implementation (Kingfisher):**
```swift
imageView.kf.setImage(
  with: URL(string: imageUrl),
  placeholder: UIImage(named: "placeholder"),
  options: [
    .transition(.fade(0.2)),
    .cacheMemoryOnly, // Memory cache
    .forceRefresh // Always fetch fresh
  ]
)
```

**Memory Cache Size:**
- Aggressive: 25% of available RAM (fast, risky)
- Conservative: 10-15% of available RAM (slower, safer)
- Example: 2GB RAM device = ~200-300 MB image cache

### Bitmap Recycling (Android)

**Problem:** Large bitmaps (display-size images) allocate on heap. GC pause causes jank.

**Solution: Bitmap Pooling**
```kotlin
// Reuse bitmap allocation instead of allocating new
val options = BitmapFactory.Options()
options.inBitmap = previousBitmap // Reuse this allocation
val newBitmap = BitmapFactory.decodeFile(filePath, options)
```

**Using Glide (Recommended):** Glide handles bitmap pooling automatically.

### Autorelease Pools (iOS)

**Problem:** In loops, temporary objects allocated but not released until loop ends.

```swift
// BAD: Loop allocates 1000 temporary strings; all kept until loop done
for i in 0..<1000 {
  let tempString = String(format: "Item %d", i)
  process(tempString)
  // tempString released when autorelease pool drained (loop end)
  // = 1000 strings in memory at once
}

// GOOD: Use explicit autorelease pool
for i in 0..<1000 {
  autoreleasepool {
    let tempString = String(format: "Item %d", i)
    process(tempString)
  } // tempString released here
}
```

**When Needed:**
- Processing large arrays
- Nested loops with temporary objects
- Image processing (many intermediate bitmaps)

---

## Network Performance

### Request Batching

**Problem:** Making N individual requests for N items is slow.

```
Bad:   GET /items/1, GET /items/2, ..., GET /items/100 (100 requests)
       Time: ~5-10 seconds (each request ~50-100ms)

Good:  GET /items?ids=1,2,...,100 (1 request)
       Time: ~200-300ms
```

**Implementation:**
```kotlin
// Bad
suspend fun fetchUser(id: String): User = api.getUser(id)

// Good
suspend fun fetchUsers(ids: List<String>): List<User> = api.getUsers(ids)

// Usage
val userIds = listOf("1", "2", "3")
val users = fetchUsers(userIds) // 1 request, not 3
```

### Response Compression

**HTTP/2 Gzip Compression:**

```
Request Header:  Accept-Encoding: gzip, deflate, br
Response Header: Content-Encoding: gzip
```

**Effect:**
- JSON response 10 KB → 2 KB compressed (80% reduction)
- Typical benefit: 70% size reduction for text
- Gzip is default on most servers; check if enabled

**How to Verify (iOS):**
```swift
let session = URLSession(configuration: .default)
// URLSession auto-adds "Accept-Encoding: gzip"
// And decompresses responses automatically
```

**How to Verify (Android):**
```kotlin
val client = OkHttpClient.Builder()
  .addInterceptor { chain ->
    chain.proceed(chain.request()
      .newBuilder()
      .header("Accept-Encoding", "gzip")
      .build()
    )
  }
  .build()
```

### HTTP/2 Multiplexing

**HTTP/1.1 Problem:** Multiple requests block each other. Request 1 must complete before request 2 starts.

```
Request 1: [======200ms======]
Request 2:                     [======200ms======]
Request 3:                                         [======200ms======]
Total:                                                              ~600ms
```

**HTTP/2 Solution:** Multiple requests share one connection; interleaved.

```
Request 1: [=====100ms====]
Request 2: [=====100ms====]  (starts immediately, not after 1)
Request 3: [=====100ms====]
Total:                    ~200ms (if server fast enough)
```

**Automatic Support:**
- iOS 9+: URLSession uses HTTP/2 automatically
- Android 5.0+: OkHttp uses HTTP/2 automatically
- Ensure server supports HTTP/2 (check headers)

### Offline Caching Headers

**Goal:** Cache responses so repeated requests don't hit network.

**HTTP Headers:**
```
Response:
  Cache-Control: max-age=3600      // Cache 1 hour
  Cache-Control: no-cache          // Revalidate before using
  ETag: "abc123"                   // Version ID
  Last-Modified: Mar 1, 2024

On next request:
  If-None-Match: "abc123"          // If same version?
  If-Modified-Since: Mar 1, 2024

Server response:
  304 Not Modified                 // Use cached; save bandwidth
  OR
  200 OK + new content             // Something changed
```

**Implementation (Android):**
```kotlin
val cache = Cache(cacheDir, 10 * 1024 * 1024) // 10 MB cache
val client = OkHttpClient.Builder()
  .cache(cache)
  .addNetworkInterceptor { chain ->
    val response = chain.proceed(chain.request())
    response.newBuilder()
      .header("Cache-Control", "max-age=3600") // 1 hour
      .build()
  }
  .build()
```

### Image Optimization

**Format Selection:**
- **JPEG:** Photos, gradients (smaller file, lossy)
- **PNG:** Icons, graphics (larger file, lossless)
- **WebP:** Supported on Android 4.0+, iOS 14+ (25-35% smaller than JPEG/PNG)
- **HEIC/HEIF:** iOS 11+ (20% smaller than JPEG, excellent quality)

**Size Optimization:**
```
Original image: 4000 x 3000 pixels, 2 MB
Display size: 400 x 300 pixels

Best: Deliver 400 x 300 WebP = ~30 KB
Bad:  Deliver 2 MB, let device scale = wasted bandwidth + processing
```

**Progressive Loading:**
```
User scrolls; image in viewport
  ↓
Load low-res preview (50 x 50, 2 KB)
  ↓ (display immediately; user sees blurry)
Load medium-res (200 x 200, 15 KB)
  ↓
Load high-res (1000 x 1000, 150 KB) (if user taps)
```

**Lazy Loading:**
```
Don't load images below viewport
Only load as user scrolls into view
```

---

## Battery Optimization

### Doze Mode (Android 6.0+)

**What:** System suspends app background activities when device idle + plugged out + screen off.

**Restrictions in Doze:**
- Background services paused
- Network access throttled
- Location updates stop
- Alarms/timers stop (unless high-priority)
- WorkManager jobs paused

**Workaround:**
```kotlin
// For high-priority work, request SCHEDULE_EXACT_ALARM
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

// Or use FCM (high-priority messages wake app)
val message = RemoteMessage.Builder("topic")
  .setPriority(RemoteMessage.PRIORITY_HIGH)
  .build()
```

### Background App Refresh (iOS)

**What:** iOS pauses background fetch, silent push notifications in low-power mode or when disabled in Settings.

**User Controls:**
- Settings → General → Background App Refresh (per-app toggle)
- Settings → Battery → Low Power Mode (disables background work)

**Workaround:**
```swift
// Request minimum interval, iOS decides actual timing
UIApplication.shared.setMinimumBackgroundFetchInterval(
  UIApplication.backgroundFetchIntervalMinimum
)

// Or use PushKit for VOIP calls (high-priority wakeup)
```

### Location Accuracy Tiers

**Problem:** GPS drains battery quickly. Accuracy impacts battery drain.

| Accuracy | Battery Cost | Precision | Use Case |
|----------|-------------|----------|----------|
| **GPS only** | ~25mAh/min | ~5 meters | Navigation |
| **GPS + cell triangulation** | ~5 mAh/min | ~20 meters | Background tracking |
| **Cell triangulation only** | ~1 mAh/min | ~100 meters | Check-in apps |
| **WiFi triangulation** | ~0.5 mAh/min | ~50 meters | Indoor localization |

**Implementation (Android):**
```kotlin
val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60000)
  .setMaxUpdateDelayMillis(120000)
  .build()

// vs.

val highAccuracy = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
  .build()
```

**Implementation (iOS):**
```swift
// Accurate; battery drain
locationManager.desiredAccuracy = kCLLocationAccuracyBest

// vs.

// Less accurate; low battery impact
locationManager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
```

### Wakelock Best Practices

**WakeLock Problem (Android):** Prevents device from sleeping; drains battery rapidly.

```kotlin
// Bad: Hold wakelock for extended time
val wakeLock = powerManager.newWakeLock(
  PowerManager.PARTIAL_WAKE_LOCK,
  "app:sync"
)
wakeLock.acquire() // Device stays awake; CPU running
// ... do work
// Forgot to release!
```

**Good: Release immediately when done**
```kotlin
try {
  wakeLock.acquire(30 * 1000) // Max 30 seconds; auto-release
  // Do work
} finally {
  wakeLock.release()
}
```

### Sync Interval Optimization

**Problem:** Syncing every 1 minute burns battery; syncing every 24 hours misses data.

**Solution: Adaptive Interval**
```
If no new data in last 3 syncs:
  Increase interval from 5m → 15m → 60m

If new data detected:
  Reset to 5m (user cares about this)
```

**Implementation (Android WorkManager):**
```kotlin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
  15, TimeUnit.MINUTES // Initial interval
).build()

// Later, based on data freshness:
val workerTag = "sync"
WorkManager.getInstance(context).cancelAllWorkByTag(workerTag)

val newInterval = if (hasNewData) {
  5 // Minute interval
} else {
  60 // No new data; check less often
}

val newRequest = PeriodicWorkRequestBuilder<SyncWorker>(
  newInterval.toLong(), TimeUnit.MINUTES
).addTag(workerTag).build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
  "sync", ExistingPeriodicWorkPolicy.REPLACE, newRequest
)
```

---

## App Size Optimization

### R8 / ProGuard (Android)

**What:** Removes unused code, shrinks method names, optimizes bytecode.

**Effect:** 30-50% size reduction typical.

**Configuration (build.gradle):**
```gradle
android {
  buildTypes {
    release {
      minifyEnabled true
      shrinkResources true // Also removes unused resources
      proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
  }
}

// proguard-rules.pro
-keep public class com.example.MyClass { *; }
-keepclassmembers class com.example.** {
  public <methods>;
}
```

**Gotchas:**
- Reflection breaks (class names are renamed)
- Keep rules for libraries using reflection (ORM, Gson, etc.)
- Test thoroughly on release build (minified vs debug different behavior)

### Bitcode (iOS)

**What:** Intermediate representation; App Store recompiles for different architectures.

**Effect:** Smaller download (15-20% reduction), but slower on users' devices.

**Configuration (Xcode):**
```
Build Settings → Enable Bitcode → Yes
```

**Gotchas:**
- Some libraries don't support bitcode
- Can break debugging on release builds
- Slighly slower app due to recompilation

### Dynamic Feature Modules (Android)

**Concept:** Split large features into separate modules; download on-demand.

```
Base app: 20 MB
Feature A (dynamic): 8 MB
Feature B (dynamic): 6 MB

User installs base (20 MB)
User navigates to Feature A
  ↓
System downloads Feature A (8 MB)
  ↓
Feature A available
```

**Configuration:**
```gradle
// base app gradle
android {
  dynamicFeatures = [":feature_a", ":feature_b"]
}

// feature_a AndroidManifest.xml
<manifest ...>
  <dist:module dist:instant="false" dist:onDemand="true" />
</manifest>
```

**Load Feature:**
```kotlin
val playCore = PlayCoreManager()
playCore.requestInstall(moduleName = "feature_a") { result ->
  if (result == InstallStatus.INSTALLED) {
    navigateToFeatureA()
  }
}
```

### Asset Optimization

**Catalog Images (iOS):**
```
Instead of shipping images as files:
├─ icon@1x.png
├─ icon@2x.png
├─ icon@3x.png

Use Asset Catalog:
├─ Assets.xcassets
  └─ Icon.imageset
    ├─ icon.png (1x)
    ├─ icon@2x.png (2x)
    └─ icon@3x.png (3x)

System ships only necessary resolution
```

**Vector Drawables (Android):**
```xml
<!-- Instead of PNGs at every density, use SVG -->
<vector android:viewportWidth="24" android:viewportHeight="24" ...>
  <path android:pathData="M6,4h12v2H6z" />
</vector>

<!-- Result: One file, scales to all densities -->
```

---

## Profiling Tools

### Android: Perfetto

**Record systtem-wide performance trace:**

```bash
# On device
adb shell perfetto \
  -c - --out=/data/misc/perfetto-traces/trace.pbtx \
  << 'EOF'
    write_into_file: true
    buffers { size_kb: 100000 }
    data_sources { config { name: "linux.ftrace" } }
    duration_ms: 10000
EOF

# Pull trace
adb pull /data/misc/perfetto-traces/trace.pbtx

# View at perfetto.dev/ui
```

### Android: CPU Profiler

**Xcode → Profiler → CPU:**
1. Start profiling
2. Interact with app
3. Stop profiling
4. Analyze CPU time per method
5. Look for hot spots (methods taking >5% CPU)

### Android: Memory Profiler

**Android Profiler → Memory:**
1. Record memory timeline
2. Force GC (button)
3. Scroll/interact to trigger memory spikes
4. Identify allocations
5. Look for increasing baseline (leak indicator)

### Android: Layout Inspector

**Inspect layout hierarchy and measure inflation times:**
```
Android Studio → Layout Inspector
1. Select app window
2. View hierarchy tree
3. Click element to see bounds, margins, padding
4. Right-click → "Profile Layout" to measure inflation
```

### iOS: Instruments

**Xcode → Product → Profile:**

**Time Profiler:**
- Measures CPU time per function
- Identify hot functions
- Total app time breakdown

**Allocations:**
- Track memory allocations over time
- Identify growth (potential leaks)
- Filter by type (view, image, string)

**Leaks:**
- Automatic leak detection
- Shows unreleased objects
- Backtrace to allocation site

**Core Animation:**
- Visualize rendering
- Color blended layers (slow = red)
- Measure FPS
- Identify offscreen rendering

### iOS: Xcode Metrics

**Xcode 13+ → Metrics:**
- App Launch time (cold/warm)
- Disk write time
- Hang time (main thread blocks)
- Scrolling/animation FPS
- Memory
- Battery usage

---

## Performance Testing

### Automated Performance Assertions (Android)

**Measure activity launch time:**
```kotlin
@RunWith(AndroidJUnit4::class)
class PerformanceTest {
  @get:Rule
  val activityScenarioRule = activityScenarioRule<MainActivity>()

  @Test
  fun testActivityLaunchTime() {
    val scenario = activityScenarioRule.scenario
    val startTime = SystemClock.uptimeMillis()

    // Activity launches
    scenario.onActivity { activity ->
      val launchTime = SystemClock.uptimeMillis() - startTime
      assertThat(launchTime).isLessThan(500) // < 500ms
    }
  }
}
```

**Measure scrolling FPS:**
```kotlin
@Test
fun testRecyclerViewScrollPerformance() {
  val scenario = activityScenarioRule.scenario
  val fpsMetric = FrameTimingMetric()

  scenario.onActivity { activity ->
    activity.recyclerView.scrollToPosition(500)
  }

  assertThat(fpsMetric.jankyFrameCount).isLessThan(10) // < 10 jank frames
}
```

### Automated Performance Assertions (iOS)

**Measure app launch:**
```swift
func testAppLaunchTime() {
  let options = XCUIApplicationLaunchOptions()
  options.arguments = ["-com.apple.CoreData.SQLiteDebugLog 0"]

  let app = XCUIApplication()
  let start = Date()
  app.launch()
  let launchTime = Date().timeIntervalSince(start)

  XCTAssertLessThan(launchTime, 0.4) // < 400ms
}
```

**Measure scroll FPS:**
```swift
func testScrollPerformance() {
  let app = XCUIApplication()
  app.launch()

  let list = app.tables.firstMatch
  list.swipeDown() // Scroll down

  // Instruments measures FPS during swipe
  // XCTest measures completion time
  XCTAssertEqual(list.waitForExistence(timeout: 2), true)
}
```

### Firebase Performance Monitoring

**Track real-world performance in production:**

```kotlin
// Android
val trace = Firebase.performance.newTrace("image_load")
trace.start()

// Load image
imageView.kf.setImage(with: url)

trace.stop()
// Logged to Firebase console
```

```swift
// iOS
let trace = Performance.startTrace(name: "image_load")

// Load image
imageView.kf.setImage(with: url)

trace?.stop()
```

---

## Performance Checklist

### Startup (Every Release)
- [ ] Cold start < 400ms (iOS), < 500ms (Android)
- [ ] Splash screen shows instantly
- [ ] Heavy initialization deferred to background
- [ ] No disk I/O on main thread during launch

### Rendering (Every Release)
- [ ] 60 FPS minimum; 120 FPS if device supports
- [ ] No jank during list scroll
- [ ] Android: Debug GPU Overdraw mostly blue
- [ ] iOS: Core Animation shows green (not red)
- [ ] Images load progressively (placeholder → full)

### Memory (Every Release)
- [ ] No memory leaks (Instruments Leaks tool clean)
- [ ] Image cache sized appropriately (10-25% of RAM)
- [ ] No excessive allocations during normal use
- [ ] Baseline memory not growing over time

### Network (Every Release)
- [ ] Gzip compression enabled
- [ ] API responses < 100 KB (typical)
- [ ] Timeout after 10 seconds
- [ ] Exponential backoff on failures
- [ ] Batching requests (not 1-per-item)

### Battery (Every Release)
- [ ] Sync interval >= 5 minutes
- [ ] Location accuracy appropriate for use case
- [ ] No wakelocks held unnecessarily
- [ ] Background tasks limited (< 30 seconds)

### App Size (Every Release)
- [ ] APK < 100 MB (Android)
- [ ] IPA < 50 MB (iOS, before compression)
- [ ] R8/ProGuard enabled (Android)
- [ ] Unused resources removed

### Profiling (Before Launch)
- [ ] CPU Profiler shows no hotspots (single function < 10%)
- [ ] Memory Profiler shows stable baseline
- [ ] Layout inflation < 50ms
- [ ] Image decoding off main thread

