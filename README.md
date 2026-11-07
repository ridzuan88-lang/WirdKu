# WirdKu – Koleksi Wirid & Zikir

Native Android (Kotlin, Jetpack Compose, Material 3, Room, DataStore, Media3). Package: `com.wirdku.app`. Offline-first, no login, no ads, no permissions.

> **STATUS:** Source code is written but has NOT yet been compiled or run. The first GitHub Actions build is the first real compile test. The bundled Hizib Bahr file is a PLACEHOLDER (one sample line: Bismillah). No real Hizib Bahr text, translation or audio has been added.

## Structure
- `app/src/main/java/com/wirdku/app/data` – models, JSON loader, Arabic segmentation, Room, DataStore
- `app/src/main/java/com/wirdku/app/ui` – theme, screens, reader, word panel
- `app/src/main/assets/collections/` – collection JSON (`index.json` lists files)
- `app/src/main/assets/audio/` – recordings (`words/`, `sections/`, full files)
- `app/src/main/assets/makhraj/letters.json` – general Malay makhraj notes (all `verified:false`)
- `app/src/main/assets/fonts/` – Noto Naskh Arabic (SIL OFL)

## Add / replace Hizib Bahr content
Edit `assets/collections/hizib_bahr.json`. Each section: `arabic` (with full harakat), optional `rumi`, `translation`, `explanation`, `audio`. Words are auto-segmented on whitespace; IDs are `<sectionId>_w001`, `_w002`, ... Override any word by adding to `words`:
```json
{"id":"section_001_w002","arabic":"اللَّهِ","audio":"audio/words/section_001_w002.mp3","slowAudio":"audio/words/section_001_w002_slow.mp3","makhraj":"...","tajwid":"...","verified":true}
```
Remove `"sourceStatus":"PLACEHOLDER"` once real text is in.

## Add a new collection
Create `assets/collections/<name>.json` (same schema, new `id`), then add the filename to `collections/index.json`. No code changes.

## Audio
Put files under `assets/audio/` and reference by path: full file in `audio` (collection), per section in `sections[].audio`, per word in `words[].audio` / `slowAudio`. MP3/M4A/AAC/WAV. Missing audio shows "Audio perkataan belum tersedia". Only use recordings from qualified reciters; the app never uses TTS. If no `slowAudio` is given, "Dengar Perlahan" plays the normal recording at 0.6x.

## Build APK with GitHub Actions
Push to `main` (or run manually: Actions → Android APK Build → Run workflow → choose debug/release). Then: Actions → latest successful run → Artifacts → **WirdKu-Android-APK** (kept 30 days). Filename: `WirdKu-v1.0.<run>-b<run>-<type>.apk`.

Release signing (optional) – repository secrets: `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`. Without them the release APK is unsigned and cannot be installed. Never commit keystores.

## Install
Download and unzip the artifact, copy the APK to the phone, allow "install unknown apps" for your file manager, open the APK.

## Content verification checklist
- [ ] Arabic text compared letter by letter with the source, harakat intact
- [ ] Section order and divisions match source
- [ ] Translation aligned to the right section
- [ ] Makhraj/tajwid notes reviewed by a qualified teacher, then `verified:true`
- [ ] Each recording checked against its word/section ID
