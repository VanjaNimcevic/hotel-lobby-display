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
