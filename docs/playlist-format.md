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

## Emergency example

The sample includes an item `emergency-fire` with `isEmergency: true` and
`enabled: false`. To test emergency behaviour later, set its `enabled` to `true`
and restart the app — it should interrupt the normal playlist.
