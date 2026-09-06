# Hotel Lobby Display

An Android TV digital-signage app for a hotel lobby. It plays a looping playlist
of full-screen content — video, images, text, scrolling banners and web pages —
on a TV or set-top box, keeps working when the network drops, and records what
it played.

Built in plain Java with standard Android components. No dependency-injection
framework, no reactive libraries.

---

## What it does

- **Full-screen playlist loop.** One item on screen at a time, cross-faded, then
  the next. When the list ends it starts again.
- **Six content types:** `VIDEO` (Media3/ExoPlayer), `IMAGE` (Glide, with a
  slow Ken Burns zoom), `TEXT` (full-screen message), `BANNER` (text pinned
  top/center/bottom), `WEB_PAGE` (WebView), `LAYOUT` (basic split screen — equal
  regions, one video/image/text each; see [Known issues](docs/known-issues.md)
  for what the template does and does not control).
- **Scheduling.** Each item can be limited to a date range, certain weekdays and
  a time-of-day window (including windows that cross midnight).
- **Priority and emergency override.** A higher `priority` sorts an item to the
  front of the loop. An `isEmergency` item takes over the screen completely for
  as long as it is scheduled, then normal playback resumes where it left off.
- **Offline playback.** The last playlist is cached in a local database and
  video/image files are downloaded to internal storage, so a reboot with no
  network still shows content.
- **Playback log.** Every item start/finish/error is written to the database with
  a timestamp and whether it played from a local file or the network.

---

## Requirements

- Android Studio (recent stable) with the Android SDK
- JDK 11 or newer (Android Studio's bundled JBR works)
- An Android TV emulator or device, **API 28+**

Key versions (see `gradle/libs.versions.toml`): Android Gradle Plugin 9.3.2,
Gradle 9.5.0, `compileSdk`/`targetSdk` 37, `minSdk` 28, Java 11.

Libraries: AndroidX Leanback 1.2.0, Glide 4.11.0, Media3 1.5.1, Room 2.6.1,
Gson 2.11.0, WorkManager 2.9.1.

---

## Build and run

From Android Studio: open the project, pick an **Android TV** emulator, press
Run.

From the command line:

```bash
# Windows
gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

If `JAVA_HOME` is not set, point it at a JDK first, e.g.:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

Install and launch on a running device/emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.vanja.hotellobbydisplay 1
```

---

## Configuration

### The playlist

Content is defined by a JSON playlist. The app looks for it in this order:

1. **Remote** — `PlaylistRepository.PLAYLIST_URL`. This is currently a
   placeholder (`https://example.com/hotel-lobby/playlist.json`), so the remote
   step always fails and the app falls through to the bundled copy. Point it at a
   real endpoint to serve playlists over the network.
2. **Bundled asset** — `app/src/main/assets/json/sample_playlist.json`.
3. **Database** — the last playlist that was stored, used only when offline or
   when 1 and 2 both fail.

Once loaded, the playlist is written to Room and becomes the "active" playlist.
**Editing the bundled JSON does not take effect until the next launch that
re-loads it** — clear the app's data or reinstall after changing it.

The full schema (all fields, scheduling, priority, layout) is documented in
[`docs/playlist-format.md`](docs/playlist-format.md).

### Demo media

`sample_playlist.json` points its video and image items at files in the
[`demo-media/`](demo-media/) folder, served through
`raw.githubusercontent.com`. Those URLs only resolve while the GitHub repo is
**public**. Swap in any public media URLs to change the demo.

### Permissions

`INTERNET` and `ACCESS_NETWORK_STATE` (declared in the manifest, both normal
permissions — nothing to grant at runtime).

---

## Architecture

### Data flow

```
MainActivity
  └─ PlaylistRepository.loadInitialPlaylist()      [background thread]
       ├─ NetworkMonitor.isOnline()
       ├─ fetch: remote URL → bundled asset → database
       ├─ PlaylistJsonParser (Gson): JSON → model objects
       ├─ map model → Room entities
       ├─ store in ONE transaction: replace items, mark playlist active
       └─ enqueue MediaDownloadWorker (WorkManager)
  ↓  callback on the main thread with the enabled items
PlaybackController.setItems() → builds TimelineScheduler
PlaybackController.start()
  └─ loop:
       ├─ TimelineScheduler.getNextItem()   (schedule + priority + emergency)
       ├─ resolve a downloaded local file if there is one
       ├─ if offline and the item needs the network → skip
       ├─ show exactly one renderer, fade it in
       ├─ PlaybackLogger writes START / FINISH / ERROR to the database
       └─ on finish/error → schedule the next item
```

### Packages

| Package | Responsibility |
|---|---|
| `` (root) | `MainActivity` — the only screen. Wires the views to `PlaybackController`, drives start/stop from the lifecycle, keeps the UI full-screen. No data or rendering logic. |
| `data` | `PlaylistRepository` (singleton, the one source of playlist data), `PlaylistJsonParser`, `PlaybackLogger`, `MediaCacheManager`, `MediaDownloadWorker`. |
| `data.local` | Room: `AppDatabase` plus entities and DAOs for `playlists`, `playlist_items`, `media_cache`, `playback_logs`. |
| `model` | Plain Gson POJOs matching the playlist JSON (`PlaylistModel`, `PlaylistItemModel`, `ScheduleModel`, `MetadataModel`, `LayoutModel`, `RegionModel`). |
| `playback` | `PlaybackController` (owns the renderers, runs the loop), `TimelineScheduler` (picks the next item), `DebugOverlay` (optional on-screen status text). |
| `player` | One renderer per content type: `VideoRenderer` (Media3), `ImageRenderer` (Glide), `TextRenderer`, `WebRenderer` (WebView), `LayoutRenderer` (split screen, builds its regions in code). Each exposes `play(...)` with a small listener for finish/error. |
| `util` | `AssetFileReader`, `HttpTextFetcher`, `HttpFileDownloader` (all `HttpURLConnection`), `NetworkMonitor`. |

### Threading

- All repository work runs on a single background `Executor`; results are posted
  back to the main thread with a `Handler`. Room queries never touch the main
  thread.
- The playback loop runs on the main thread and schedules the next step with a
  `Handler.postDelayed`. Renderers are UI components, so they must.
- Media downloads run in a WorkManager `Worker`, off the app process's control.

### Storage

- **Room database** `hotel_lobby.db`, schema version 2. Migrations are
  destructive (`fallbackToDestructiveMigration()`): a version bump wipes the
  tables and the playlist reloads itself on the next launch.
- **Media cache** in `filesDir/media_cache/`. `MediaDownloadWorker` downloads
  every enabled `VIDEO`/`IMAGE` URL (write to `.tmp`, then rename).
  `PlaybackController` prefers a cached file over the network URL. File names are
  `Integer.toHexString(url.hashCode())` plus the original extension.

### Scheduling and priority (`TimelineScheduler`)

Each `getNextItem()` call:

1. Filters to items that are `enabled` **and** within their schedule now
   (`startAt`/`endAt` as UTC instants; `daysOfWeek` and `startTime`/`endTime`
   against the device's local date/time; a `startTime` later than `endTime`
   means the window crosses midnight).
2. If any eligible item has `isEmergency`, returns it — every cycle, without
   advancing the normal cursor.
3. Otherwise sorts the eligible items by `priority` descending, then
   `orderIndex` ascending, and returns the next one, looping back to the start.

---

## Documentation

| File | Contents |
|---|---|
| [`docs/playlist-format.md`](docs/playlist-format.md) | Full playlist JSON schema with examples. |
| [`docs/testiranje.md`](docs/testiranje.md) | Manual test guide, one section per task (how to verify each feature in the emulator). |
| [`docs/known-issues.md`](docs/known-issues.md) | Current limitations and trade-offs. |
| [`docs/odbrana/`](docs/odbrana/) | Per-task write-ups (what / how / why), in Serbian. |

### Turning on the debug overlay

Set `DebugOverlay.ENABLED = true` in
`app/src/main/java/com/vanja/hotellobbydisplay/playback/DebugOverlay.java` and
rebuild. A small box in the top-left corner then shows the current item id,
type, source (LOCAL/REMOTE), playlist source and network state.

### Reading the playback log

```bash
adb shell "run-as com.vanja.hotellobbydisplay sqlite3 databases/hotel_lobby.db \
  'SELECT itemId, status, source, datetime(startedAt/1000,\"unixepoch\") FROM playback_logs ORDER BY id DESC LIMIT 20;'"
```

---

## Project status

The app was built task by task (Jira APV-4 … APV-30), all tasks complete. APV-30
(split-screen `LAYOUT` rendering) is intentionally a basic version — see
[Known issues](docs/known-issues.md).
