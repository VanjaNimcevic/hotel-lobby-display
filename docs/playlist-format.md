# Playlist JSON format

The app plays a **playlist**: a list of content items shown one after another in a
loop on a hotel lobby TV. The playlist comes from a JSON file.

- Bundled sample: `app/src/main/assets/json/sample_playlist.json`
- Later (APV-14) the app will try to download the same JSON from a remote URL and
  fall back to this bundled file if there is no internet.

All example media URLs in the sample are public test files. Replace them with real
CDN URLs when you have them — each item has a single `url` field, nothing else to change.

---

## Top level

| Field        | Type   | Meaning                                                        |
|--------------|--------|---------------------------------------------------------------|
| `playlistId` | string | Unique id of this playlist. **Required.**                    |
| `version`    | number | Increases every time the playlist changes. **Required.**     |
| `updatedAt`  | string | ISO-8601 timestamp of the last change. Optional.            |
| `items`      | array  | The content items, in play order. **Required.**             |

---

## Item

| Field         | Type              | Meaning                                                                 |
|---------------|-------------------|----------------------------------------------------------------------|
| `id`          | string            | Unique id of the item.                                              |
| `type`        | string            | One of `VIDEO`, `IMAGE`, `TEXT`, `BANNER`, `WEB_PAGE`, `LAYOUT`.    |
| `text`        | string or null    | The text to show. Used by `TEXT` and `BANNER`. `\n` = new line.    |
| `url`         | string or null    | Media/page address. Used by `VIDEO`, `IMAGE`, `WEB_PAGE`.          |
| `durationSec` | number            | How long to show the item, in seconds. `0` for `VIDEO` means "play the whole video". |
| `orderIndex`  | number            | Play order, small number first.                                    |
| `enabled`     | boolean           | `false` = skip this item completely.                               |
| `priority`    | number            | Higher number wins when choosing what to play next. Normal items use `0`. |
| `isEmergency` | boolean           | `true` = interrupt normal playback while this item is enabled and in schedule (APV-26). |
| `schedule`    | object or null    | When the item is allowed to play. `null` = always allowed.         |
| `metadata`    | object or null    | Extra per-type options, see below.                                 |
| `layout`      | object or null    | Only for `type: LAYOUT`, see below.                                |

### `schedule`

| Field         | Type            | Meaning                                                       |
|---------------|-----------------|-----------------------------------------------------------|
| `startAt`     | string or null  | ISO-8601. Item does not play before this moment.         |
| `endAt`       | string or null  | ISO-8601. Item does not play after this moment.          |
| `daysOfWeek`  | array of number | Allowed weekdays. `1` = Monday ... `7` = Sunday.         |
| `startTime`   | string          | `"HH:mm"`. Earliest time of day the item may play.       |
| `endTime`     | string          | `"HH:mm"`. Latest time of day the item may play.         |

### `metadata`

| Field               | Type    | Used by     | Meaning                                             |
|---------------------|---------|-------------|--------------------------------------------------|
| `bannerPosition`    | string  | `BANNER`    | `top`, `center` or `bottom`.                     |
| `javascriptEnabled` | boolean | `WEB_PAGE`  | Turn JavaScript on in the WebView.              |
| `scaleType`         | string  | `IMAGE`     | `fitCenter` (no crop) or `centerCrop` (fill).   |

### `layout` (LAYOUT items only)

| Field      | Type   | Meaning                                                    |
|------------|--------|--------------------------------------------------------|
| `template` | string | Layout template name, e.g. `VIDEO_LEFT_TEXT_RIGHT`.   |
| `regions`  | array  | The parts of the split screen.                        |

Each region has `type` (`VIDEO` or `TEXT`), plus `url` or `text` like a normal item.

> LAYOUT is a bonus feature (APV-30). If it is not implemented, the app must skip
> LAYOUT items safely.

---

## Priority and emergency (APV-26)

Every item has `priority` (number, default `0`) and `isEmergency` (boolean).
`TimelineScheduler` uses them like this:

**Emergency** (`isEmergency: true`)
- While an emergency item is **eligible** — that is, `enabled: true` **and**
  inside its `schedule` (`startAt`/`endAt`, `daysOfWeek`, `startTime`/`endTime`) —
  it is the **only** thing that plays. Every playback cycle returns it again.
- Nothing else (normal items, priority items) plays during that time.
- The moment it stops being eligible (disabled, or its `endAt` passes), the
  normal rotation **resumes from where it left off** — the emergency does not
  advance or reset the normal rotation cursor.
- If several emergency items are eligible at once, the one with the highest
  `priority` wins.

**Priority** (`priority > 0`, `isEmergency: false`)
- The item stays part of the normal rotation but sorts to the **front** of each
  loop: rotation order is `priority` descending, then `orderIndex` ascending.
- It plays once per loop near the start — it does **not** take over the screen
  the way an emergency item does.

**Normal** (`priority: 0`, `isEmergency: false`)
- Plain rotation by `orderIndex`, looping forever.

### Trying it out

The sample includes two normally-disabled items:

| Item              | Setting                          | Flip `enabled` to `true` to see... |
|-------------------|---------------------------------|-----------------------------------|
| `emergency-fire`  | `isEmergency: true`, has a `schedule` covering all of 2026 | the emergency text takes over the whole screen; disable it again and normal playback resumes |
| `priority-notice` | `priority: 5`, `orderIndex: 99` | it plays **first** each loop despite the high `orderIndex`, but the rest of the playlist still plays |

To see `startAt`/`endAt` being respected, change `emergency-fire`'s
`schedule.endAt` to a past date — it then stays inactive even with
`enabled: true`.
