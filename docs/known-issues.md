# Known issues and limitations

Current as of APV-30. Nothing here crashes the app; they are trade-offs or
unfinished optional work.

## Configuration

- **`PLAYLIST_URL` is a placeholder** (`https://example.com/hotel-lobby/playlist.json`)
  in `PlaylistRepository`. Until it points at a real endpoint, every online run
  falls back to the bundled `assets/json/sample_playlist.json`.
- **Playlist changes need a reinstall.** The playlist is cached in Room and only
  reloaded from assets/remote on the next launch that reaches
  `loadInitialPlaylist`. Editing `sample_playlist.json` (including toggling the
  emergency / priority demo items) requires uninstalling the app or clearing its
  data.

## Database

- **Schema upgrades are destructive.** `AppDatabase` uses
  `fallbackToDestructiveMigration()` and no `Migration` classes. Bumping the DB
  version (last done for `playback_logs.source` at v2) wipes all tables on the
  first launch after the upgrade. The playlist reloads itself; downloaded media
  files stay on disk but lose their `media_cache` rows.

## Playback

- **`LAYOUT` rendering is basic (APV-30).** `LayoutRenderer` splits the screen
  into equal regions (horizontal, or stacked when the template name contains
  TOP/BOTTOM/STACK) and shows one VIDEO/IMAGE/TEXT per region. The `template`
  string is otherwise ignored - no 70/30 splits, no per-region positioning or
  styling. Region media is loaded straight from its URL: `MediaDownloadWorker`
  only caches top-level item URLs, so a LAYOUT with a remote video is skipped
  while offline (like any other network-only item).
- **Local-file check runs on the main thread.** `MediaCacheManager.isCached`
  does `File.exists()` / `File.length()` from `PlaybackController.playCurrent`.
  These are fast stat calls (once per item, seconds apart) but StrictMode would
  flag them.
- **Cache file names use `String.hashCode()`.** Two different URLs producing the
  same hash would collide. Extremely unlikely for a handful of media URLs; a
  real hash (SHA-256) would remove the risk.

## Scheduling

- **Uses the device clock and its default time zone.** `startAt` / `endAt` are
  parsed as UTC `Instant`s; `daysOfWeek` and `startTime` / `endTime` use the
  device's local date/time. There is no per-playlist time-zone field.

## Platform

- **Pre-API-30 fullscreen path is deprecated.** `hideSystemUI()` falls back to
  `setSystemUiVisibility` on API 28-29. It works but the API is deprecated.
