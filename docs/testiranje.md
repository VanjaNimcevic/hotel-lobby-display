# Kako pokrenuti i testirati aplikaciju

Aplikacija za sada nema vidljiv UI — `MainActivity` samo učitava playlistu i sve
ispisuje u **Logcat**. Zato se sve provere rade kroz Logcat i **Database
Inspector**, ne gledanjem u ekran (ekran je crn, to je očekivano do APV-15/16).

---

## 1. Priprema: Android TV emulator (radi se jednom)

1. U Android Studiju: **Tools → Device Manager** (ili ikona telefona u desnoj
   traci).
2. **Create Device** (Create Virtual Device).
3. Kategorija **TV** → izaberi npr. **Television (1080p)** → **Next**.
4. System Image: izaberi neki **API 28 ili noviji** (projekat je `minSdk 28`).
   Ako nije preuzet, klikni **Download** pored imena, sačekaj, pa **Next**.
5. **Finish**.

Sad u gornjoj traci Android Studija, pored dugmeta Run, u padajućem meniju
uređaja biraš taj TV emulator.

---

## 2. Pokretanje aplikacije

1. U padajućem meniju uređaja izaberi TV emulator.
2. Klikni zeleni trougao **Run 'app'** (ili `Shift+F10`).
3. Emulator se upali (prvi put traje minut-dva), aplikacija se instalira i
   pokrene. Videćeš **crn ekran** — to je u redu.

> Ako Run ne radi zbog Jave iz terminala — to ne utiče na Run iz Android
> Studija, on koristi svoju ugrađenu Javu.

---

## 3. Logcat — otvaranje i filtriranje

1. Dole u Android Studiju klikni tab **Logcat** (ili `View → Tool Windows →
   Logcat`).
2. Proveri da je gore izabran tvoj emulator i proces
   `com.vanja.hotellobbydisplay`.
3. U polje za pretragu (filter) ukucaj jedan od ovih:

   - sve poruke naše aplikacije:
     ```
     package:com.vanja.hotellobbydisplay
     ```
   - samo naši tagovi:
     ```
     tag:MainActivity | tag:PlaylistRepository | tag:HttpTextFetcher | tag:PlaylistJsonParser | tag:AssetFileReader
     ```

4. Da vidiš poruke od početka, ponovo pokreni aplikaciju (Run) dok gledaš
   Logcat, ili klikni ikonicu za brisanje pa Run.

---

## 4. Šta treba da vidiš pri normalnom pokretanju (APV-6 do APV-14)

Ceo lanac se izvrši na startu. Očekivani redosled (podrazumevano, bez pravog
URL-a):

```
W  HttpTextFetcher     GET https://example.com/hotel-lobby/playlist.json returned HTTP 404
I  AssetFileReader     Loaded asset file 'json/sample_playlist.json' (NNNN chars)
I  PlaylistJsonParser  Parsed playlist PlaylistModel{playlistId='hotel-lobby-default', version=1, items=7}
I  PlaylistRepository  Playlist loaded from ASSETS (playlistId=hotel-lobby-default, version=1)
I  PlaylistRepository  Stored playlist 'hotel-lobby-default' with 7 items (6 enabled)
I  MainActivity        Playlist ready: 6 enabled items
I  MainActivity          item -> video-welcome (VIDEO, 0s)
I  MainActivity          item -> image-pool (IMAGE, 10s)
I  MainActivity          item -> text-welcome (TEXT, 8s)
I  MainActivity          item -> banner-breakfast (BANNER, 8s)
I  MainActivity          item -> web-info (WEB_PAGE, 20s)
I  MainActivity          item -> layout-split (LAYOUT, 15s)
```

- **6 enabled** jer je `emergency-fire` stavka `enabled: false`.
- ako vidiš ovo — rade APV-6 (asset čitanje), APV-10 (parsiranje), APV-13
  (repozitorijum + Room), APV-14 (remote pokušaj + fallback).

---

## 5. Test scenariji za APV-14

### Scenario A — podrazumevano: remote ne postoji → fallback na ASSETS

Ništa ne diraš. Pokreni aplikaciju. U Logcat-u:

```
W  HttpTextFetcher     GET https://example.com/hotel-lobby/playlist.json returned HTTP 404
I  PlaylistRepository  Playlist loaded from ASSETS (...)
```

✅ Fallback radi, izvor je jasno logovan.

---

### Scenario B — "nema interneta" → fallback na ASSETS, bez pada

Najlakši način (ne diraš mrežu emulatora): privremeno u
`PlaylistRepository.java` promeni `PLAYLIST_URL` u host koji ne postoji:

```java
private static final String PLAYLIST_URL = "https://nepostojeci-host.invalid/playlist.json";
```

`.invalid` je rezervisan domen koji uvek padne na DNS-u — isto kao da nema
interneta. Pokreni:

```
W  HttpTextFetcher     GET https://nepostojeci-host.invalid/playlist.json failed: java.net.UnknownHostException: ...
I  PlaylistRepository  Playlist loaded from ASSETS (...)
I  MainActivity        Playlist ready: 6 enabled items
```

✅ Aplikacija **ne puca**, nastavlja sa lokalnim fajlom.

(Pravi način — isključiti mrežu emulatoru: u bočnoj traci emulatora `...`
Extended controls → **Cellular** → *Data status: Denied*, i ugasi Wi-Fi. Ili
preko terminala: `adb shell svc wifi disable` i `adb shell svc data disable`,
kasnije `enable` da vratiš.)

Posle testa **vrati `PLAYLIST_URL` na staru vrednost.**

---

### Scenario C — pravi URL → izvor REMOTE

Potreban je URL koji vraća **validan** playlist JSON. Najbrže preko GitHub Gist-a:

1. Idi na <https://gist.github.com>, uloguj se.
2. Ime fajla: `playlist.json`.
3. Sadržaj: iskopiraj **ceo** `app/src/main/assets/json/sample_playlist.json`,
   ali promeni `"version": 1` u **`"version": 2`** (da razlikuješ remote od
   asset verzije u logu).
4. **Create secret gist**.
5. Klikni dugme **Raw** → iz adrese browsera kopiraj URL (izgleda kao
   `https://gist.githubusercontent.com/<user>/<hash>/raw/.../playlist.json`).
6. Nalepi ga u `PLAYLIST_URL` u `PlaylistRepository.java`.
7. Pokreni aplikaciju (uz uključen internet). U Logcat-u:

```
I  HttpTextFetcher     GET https://gist.githubusercontent.com/.../playlist.json ok (NNNN chars)
I  PlaylistRepository  Playlist loaded from REMOTE (playlistId=hotel-lobby-default, version=2)
```

✅ `version=2` dokazuje da je playlista došla sa **interneta**, ne iz asseta.

Posle testa **vrati `PLAYLIST_URL` na staru vrednost.**

---

## 5b. Test APV-15 — video reprodukcija

Playlista ima jednu `VIDEO` stavku (`video-welcome`, kratak test klip, 10 s,
~1 MB — namerno kratak da testiranje ide brzo).

**Normalno:**
1. Pokreni na TV emulatoru uz uključen internet.
2. Posle par sekundi (dok se video bafuje) trebalo bi da se **vidi video preko
   celog ekrana**, bez ikakvih kontrola.
3. Logcat `tag:VideoRenderer | tag:MainActivity`:
   ```
   I  MainActivity   VIDEO items: 1
   I  MainActivity   VIDEO 1/1 -> video-welcome
   I  VideoRenderer  Playing video: https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4
   ... (10 s kasnije) ...
   I  VideoRenderer  Video ended: https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4
   I  MainActivity   VIDEO 1/1 -> video-welcome        (kreće ponovo - petlja)
   ```

> Napomena: originalni URL iz APV-5
> (`commondatastorage.googleapis.com/gtv-videos-bucket/...`) je u međuvremenu
> počeo da vraća HTTP 403, pa je prvo zamenjen W3C-jevim "Sintel" trailer-om
> (~4 MB, ~50 s), a zatim ovim kraćim 10 s / 1 MB klipom da testiranje ide brže.

**Test greške (video se ne učita):**
1. U `app/src/main/assets/json/sample_playlist.json` privremeno promeni `url`
   stavke `video-welcome` u `https://example.com/nema.mp4`.
2. Deinstaliraj aplikaciju sa emulatora (da se baza osveži) pa Run ponovo.
3. Logcat:
   ```
   E  VideoRenderer  Video error for https://example.com/nema.mp4: ERROR_CODE_IO_BAD_HTTP_STATUS
   E  MainActivity   Skipping VIDEO video-welcome after error: ...
   ... (pauza 3 s) ...
   I  VideoRenderer  Playing video: https://example.com/nema.mp4   (novi pokušaj)
   ```
   Aplikacija **ne puca**. Posle svake greške čeka se 3 s (`RETRY_DELAY_MS`) da
   se mreža ne zatrpava kad je stavka trajno loša.
4. Vrati `sample_playlist.json`: `git checkout -- app/src/main/assets/json/sample_playlist.json`.

**Test lifecycle (oslobađanje plejera):**
1. Dok video ide, pritisni **Home** dugme na emulatoru (ili ikonicu kuće u
   bočnoj traci).
2. Vrati se u aplikaciju (Recents / ponovo je otvori).
3. U Logcat-u nema greške ("player released"/re-create), video se ponovo
   pokrene od početka.

---

## 5c. Test APV-16 — slika sa trajanjem

Playlista sad ima **dve** stavke koje se puštaju u krug: `video-welcome`
(VIDEO) i `image-pool` (IMAGE, `durationSec: 10`, sa `picsum.photos`).

1. Pokreni aplikaciju (deinstaliraj prvo ako baza ima staru playlistu).
2. Logcat `tag:ImageRenderer | tag:MainActivity`:
   ```
   I  MainActivity   ITEM 2/2 -> image-pool (IMAGE)
   I  ImageRenderer  Showing image for 10s: https://picsum.photos/1920/1080
   ... (10 s) ...
   I  MainActivity   ITEM 1/2 -> video-welcome (VIDEO)
   ```
3. Na ekranu: posle videa treba da se pojavi **slika preko celog ekrana**
   tačno 10 sekundi, pa se vrati na video.

**Test greške:** privremeno promeni `url` stavke `image-pool` u
`https://picsum.photos/nema.jpg`, deinstaliraj pa Run:
```
E  ImageRenderer  Image failed to load: https://picsum.photos/nema.jpg
E  MainActivity   Skipping IMAGE image-pool after error: ...
```
Aplikacija ne puca, ide dalje posle 3 s. Vrati URL nazad
(`git checkout -- app/src/main/assets/json/sample_playlist.json`).

---

## 5d. Test APV-17 — tekst i baner

Playlista sad ima 4 stavke u krugu: VIDEO, IMAGE, TEXT (`text-welcome`, 8s,
u sredini), BANNER (`banner-breakfast`, 8s, `position: bottom`).

1. Deinstaliraj app pa Run (baza da se osveži).
2. Logcat `tag:TextRenderer | tag:MainActivity`:
   ```
   I  MainActivity   ITEM 3/4 -> text-welcome (TEXT)
   I  TextRenderer   Showing text for 8s (position=null)
   ... (8 s) ...
   I  MainActivity   ITEM 4/4 -> banner-breakfast (BANNER)
   I  TextRenderer   Showing text for 8s (position=bottom)
   ```
3. Na ekranu: `text-welcome` tekst se pojavi **u sredini**, `banner-breakfast`
   **pri dnu** ekrana, oba na crnoj pozadini, beo veliki tekst.

**Test nepoznate pozicije:** u `sample_playlist.json` privremeno promeni
`"bannerPosition": "bottom"` (kod `banner-breakfast`) u
`"bannerPosition": "nesto-cudno"`. Deinstaliraj pa Run — tekst i dalje ide u
**sredinu** (bezbedan fallback), bez greške u Logcat-u. Vrati JSON nazad.

---

## 5e. Test APV-18 — web stranica

Playlista sad ima 5 stavki u krugu, uklj. `web-info`
(`https://www.wikipedia.org`, `durationSec: 20`, `javascriptEnabled: true`).

1. Deinstaliraj app pa Run (baza da se osveži).
2. Logcat `tag:WebRenderer | tag:MainActivity`:
   ```
   I  MainActivity   ITEM 5/5 -> web-info (WEB_PAGE)
   I  WebRenderer    Showing web page for 20s: https://www.wikipedia.org (javascript=true)
   ```
3. Na ekranu: Wikipedia naslovna preko celog ekrana 20 s, pa se vrati na
   video. (Prvo je probano `example.com` — radilo je ispravno, ali stranica je
   toliko minimalna, belo/skoro prazno, da je na TV ekranu delovalo kao da se
   ništa nije desilo. Wikipedia je vizuelno jasnija za demonstraciju.)

**Test greške:** u `sample_playlist.json` privremeno promeni `url` stavke
`web-info` u `https://ne-postoji-nikako.invalid/`, deinstaliraj pa Run:
```
E  WebRenderer    WebView error for https://ne-postoji-nikako.invalid/: net::ERR_NAME_NOT_RESOLVED
E  MainActivity   Skipping WEB_PAGE web-info after error: ...
```
Aplikacija ne puca, ide dalje posle 3 s. Vrati URL nazad.

---

## 5f. Test APV-19 + APV-20 — pravi raspored i kontroler

Od ovog taska playlistu vodi `PlaybackController` preko pravog
`TimelineScheduler`-a, ne fiksna lista. Logcat tag se promenio:
`tag:PlaybackController` (umesto starih `tag:MainActivity` poruka za
start/kraj stavke).

**Normalna rotacija (ništa ne diraš):**
1. Deinstaliraj app pa Run.
2. Logcat `tag:PlaybackController`:
   ```
   I  PlaybackController  Scheduler ready with 5 item(s)
   I  PlaybackController  START item=video-welcome type=VIDEO
   I  PlaybackController  FINISH item=video-welcome
   I  PlaybackController  START item=image-pool type=IMAGE
   I  PlaybackController  FINISH item=image-pool
   I  PlaybackController  START item=text-welcome type=TEXT
   ...
   I  PlaybackController  START item=layout-split type=LAYOUT
   I  PlaybackController  Skipping unsupported type: LAYOUT
   I  PlaybackController  START item=video-welcome type=VIDEO   (krug se ponavlja)
   ```
   `layout-split` se **pojavi u logu** (scheduler ga izabere) ali odmah
   **preskoči** (kontroler nema render za LAYOUT) — to je dokaz da "bezbedno
   preskoči nepodržan tip" stvarno radi, ne samo u teoriji.

**Test vremenskog prozora (TEXT van `startTime`/`endTime`):**
1. `text-welcome` ima `startTime: "06:00"`, `endTime: "23:00"`.
2. Ako je sistemsko vreme emulatora van tog prozora, ta stavka se **neće
   pojaviti** u rotaciji (`getNextItem()` je preskoči).
3. Da testiraš bez čekanja pravog vremena: na emulatoru promeni sistemski sat
   (Settings → System → Date & time → isključi automatsko, postavi npr.
   02:00) i restartuj app.

**Test emergency stavke:**
1. U `sample_playlist.json` promeni `emergency-fire.enabled` sa `false` na
   `true`.
2. Deinstaliraj app pa Run.
3. Logcat: `emergency-fire` se pojavljuje **pre** normalnih stavki, i to na
   svakom sledećem ciklusu (dok je `enabled: true`) jer emergency/priority
   provera u scheduleru ide pre normalne rotacije:
   ```
   I  PlaybackController  START item=emergency-fire type=TEXT
   I  PlaybackController  FINISH item=emergency-fire
   I  PlaybackController  START item=emergency-fire type=TEXT   (opet ona)
   ```
4. Vrati `enabled` na `false` i `sample_playlist.json` na original
   (`git checkout -- app/src/main/assets/json/sample_playlist.json`).

**Test greške (isto kao ranije, sad kroz kontroler):**
- privremeno pokvari neki URL → `tag:PlaybackController`:
  ```
  E  PlaybackController  ERROR item=video-welcome: Source error
  ```
  pa nova stavka posle 3s.

---

## 5g. Test APV-21 — playback logovi u bazi

1. Pokreni app (deinstaliraj prvo ako baza ima staru šemu).
2. Pusti ga da odigra bar 3-4 stavke.
3. Logcat `tag:PlaybackLogger`:
   ```
   I  PlaybackLogger  STARTED item=video-welcome (row 1)
   I  PlaybackLogger  COMPLETED item=video-welcome (row 1)
   I  PlaybackLogger  STARTED item=image-pool (row 2)
   I  PlaybackLogger  COMPLETED item=image-pool (row 2)
   ```
4. **Database Inspector** → `hotel_lobby.db` → tabela `playback_logs`:
   - broj redova = broju odigranih stavki
   - svaki red ima `itemId`, `startedAt`, `finishedAt` (veće od `startedAt`),
     `status = COMPLETED`, `errorMessage = null`
5. **Test greške:** privremeno pokvari neki URL (npr. `video-welcome`) →
   deinstaliraj pa Run → taj red u bazi dobije `status = ERROR` i popunjen
   `errorMessage`. Logcat: `E PlaybackController ERROR item=... : ...` pa
   `I PlaybackLogger ERROR item=... (row N)`.
6. **LAYOUT ne pravi red:** `layout-split` se pojavljuje u
   `tag:PlaybackController` logu ("Skipping unsupported type: LAYOUT") ali
   **ne** dobija red u `playback_logs` — pošto se render nikad stvarno ne
   pokrene.

---

## 5h. Test APV-22 — MediaCacheManager

Ovaj task samo pravi manager i keš direktorijum; download i punjenje tabele su
APV-23.

1. Pokreni app. Logcat `tag:MediaCacheManager`:
   ```
   I  MediaCacheManager  Media cache dir: /data/data/com.vanja.hotellobbydisplay/files/media_cache
   ```
2. **Device Explorer** (View → Tool Windows → Device Explorer) →
   `data/data/com.vanja.hotellobbydisplay/files/` → treba da postoji folder
   `media_cache` (prazan za sada).
3. Build treba da prođe sa `BUILD SUCCESSFUL` (Room validira i novi
   `deleteByUrl` upit u `MediaCacheDao`).

---

## 5i. Test APV-23 — pozadinsko preuzimanje medija

`PlaylistRepository` posle snimanja playliste pokrene `MediaDownloadWorker`
(WorkManager) koji skine `VIDEO` i `IMAGE` fajlove.

1. **Deinstaliraj** app (da keš/baza krenu čisti) pa Run, uz internet.
2. Logcat `tag:MediaDownloadWorker | tag:HttpFileDownloader | tag:MediaCacheManager`:
   ```
   I  PlaylistRepository   Enqueued media download work
   I  HttpFileDownloader   Downloaded https://.../Big_Buck_Bunny_360_10s_1MB.mp4 (991017 bytes)
   I  MediaCacheManager    Cached https://.../Big_Buck_Bunny_360_10s_1MB.mp4 (991017 bytes)
   I  HttpFileDownloader   Downloaded https://picsum.photos/1920/1080 (NNNNN bytes)
   I  MediaDownloadWorker  Download pass done: 2 downloaded, 0 already cached, 0 failed
   ```
3. **Device Explorer** → `data/data/com.vanja.hotellobbydisplay/files/media_cache/`
   → 2 fajla (heks ime + `.mp4` / `.jpg`).
4. **Database Inspector** → `media_cache` → 2 reda, `status = COMPLETED`,
   `localFilePath` popunjen, `fileSizeBytes > 0`.
5. **"Ne preuzima opet":** pokreni app **ponovo** (bez deinstalacije) →
   `Download pass done: 0 downloaded, 2 already cached, 0 failed`.
6. **Neuspeh jednog fajla:** privremeno u `sample_playlist.json` promeni `url`
   stavke `image-pool` u nepostojeći, deinstaliraj pa Run →
   `W MediaDownloadWorker Failed to download ...`, red `media_cache` za taj URL
   dobije `status = FAILED`, ali **video se svejedno preuzme**. Posao ide u
   `retry` (vidljivo u Background Task Inspector-u).
7. **Background Task Inspector:** App Inspection → Background Task Inspector →
   `media-download` posao i njegovo stanje (ENQUEUED / RUNNING / SUCCEEDED /
   RETRY).

---

## 6. Provera da su podaci stvarno u bazi (Room)

1. Dok aplikacija radi na emulatoru: **View → Tool Windows → App Inspection**.
2. Tab **Database Inspector** → izaberi proces `com.vanja.hotellobbydisplay`.
3. Otvori bazu **`hotel_lobby.db`**:
   - **`playlists`** — 1 red: `playlistId = hotel-lobby-default`, `version`,
     `active = 1`, `fetchedAt` (broj).
   - **`playlist_items`** — 7 redova. Pogledaj kolone: `type`, `url`, `text`,
     `enabled`, `scheduleDaysOfWeek` (npr. `1,2,3,4,5,6,7`), `layoutJson`
     (popunjen samo za `layout-split`).
4. Dupli klik na tabelu da vidiš sadržaj; ima i dugme za osvežavanje.

---

## 7. Čišćenje posle testova

- Vrati `PLAYLIST_URL` na originalnu vrednost ako si je menjao.
- Ako si menjao `sample_playlist.json` (za neki test), `git checkout --
  app/src/main/assets/json/sample_playlist.json` da vratiš original.
- Da obrišeš bazu i kreneš čisto: na emulatoru dugo drži aplikaciju →
  deinstaliraj, ili `adb uninstall com.vanja.hotellobbydisplay`, pa Run ponovo.
