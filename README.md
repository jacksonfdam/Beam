# Beam

Turn any exported PDF (Keynote, PowerPoint, Canva, Figma) into a flawless
fullscreen presentation, controlled from your phone. No broken fonts, no layout
surprises — the PDF renders exactly as designed. **Open → Present → Done.**

The product stance is **local-first, LocalSend-style**: no accounts, no cloud
storage of user content, no telemetry. Devices discover and talk to each other
directly over the LAN. The only thing that may ever touch a server is the
WebRTC signaling handshake for the optional browser remote — never slide
content, notes, or ink.

## Repository layout

```
files/    The canonical build prompts and BeamProtocol.kt (the wire-protocol spec)
mobile/   Kotlin Multiplatform apps — desktop presenter/host + Android/iOS remote
web/      Next.js — browser remote + WebRTC signaling (+ the in-app landing)
landing/  Standalone static marketing site for Vercel (multilingual, demo videos)
```

Each subfolder has its own README with details; this file is the project
overview and the runbook.

## Platforms & roles

| Target | Role |
| --- | --- |
| Desktop (Compose Multiplatform / JVM) | **Presenter + host.** Renders the deck fullscreen on the external display, runs the presenter view, hosts the LAN WebSocket server, shows the QR / IP / PIN, paints the live ink overlay, and owns the single source of truth (current slide, timer, notes). |
| Android + iOS (Compose Multiplatform) | **The remote.** Connects by QR or manual IP + PIN, picks a deck, navigates, controls the timer, draws on the slide, and reads speaker notes. |
| Web (Next.js on Vercel) | **Landing page + browser remote.** Public download/info page; a browser-based remote that pairs over WebRTC (a Vercel function brokers signaling only). |

## Module map (mobile)

```
:core         domain models + wire protocol (com.jacksonfdam.beam.protocol) + BeamJson + transport interfaces + notes sidecar
:transport    Ktor PresenterClient (multiplatform) + PresenterServer (JVM/CIO) + handshake/PIN
:pdf          PdfDocument expect/actual rendering (Desktop PDFBox, Android PdfRenderer, iOS CoreGraphics)
:app:shared   shared Compose remote UI (pairing, deck picker, controls, notes, timer, drawing)
:app:desktopApp  desktop presenter/host entry point
:app:androidApp  Android remote entry point
:app:iosApp      iOS remote entry point (Xcode)
:server          Ktor scaffold (JVM)
```

The wire protocol is defined **once** in `:core` and mirrored in
`web/lib/protocol.ts` for the browser remote — the two must change together.
Transport, PDF rendering, and UI depend on the abstractions in `:core`
(`PresenterClient`, `PresenterServer`), never on a concrete engine.

---

# Runbook — how to run & test

Two things work end to end today: the **web landing page**, and the **desktop
host ↔ native Android/iOS remote over Wi‑Fi**. The browser remote has a caveat
(see the end).

## Prerequisites

- JDK 17+ (AGP 9 requires it; the code targets JVM 11), Android SDK with
  `mobile/local.properties` pointing `sdk.dir`, and Xcode for iOS.
- Node 20+ for the web layer.
- For the native flow: the desktop (host) and the phone (remote) **on the same
  Wi‑Fi network**. Default port `53317`.

## Web (`web/`)

```bash
cd web
npm install
npm run dev        # http://localhost:3000  (landing at /, remote at /remote)
npm run build      # production build
npm run lint       # eslint
npm test           # vitest — protocol + signaling tests
```

Signaling uses an in-memory store by default. For production on Vercel set
`KV_REST_API_URL` / `KV_REST_API_TOKEN` (or the `UPSTASH_*` pair) and
`SIGNAL_ALLOWED_ORIGIN`.

## Desktop (host / presenter)

```bash
cd mobile
./gradlew :app:desktopApp:run           # or :app:desktopApp:hotRun --auto
```

The control window shows a **QR + IP:port + PIN**; a second fullscreen window
opens on the external display when present. Click **Open a PDF** to load a deck
— it appears on the projector. For speaker notes, place a `<deck>.notes.json`
next to the PDF:

```json
{ "version": 1, "notes": { "0": "Open warm", "3": "Pause for the demo" } }
```

## Android (remote)

```bash
cd mobile
./gradlew :app:androidApp:installDebug   # to a connected device/emulator
# or :assembleDebug and install the APK manually
```

## iOS (remote)

Open `mobile/app/iosApp` in Xcode and run on a device/simulator. On the first
connection iOS prompts for local-network permission.

## End-to-end test (native — the primary path)

1. Run the desktop (`:app:desktopApp:run`) and click **Open a PDF**.
2. On the phone (same Wi‑Fi), open Beam → the pairing screen.
3. Enter the **IP** shown on the desktop and the **PIN** → **Connect**.
4. Pick the deck → you land on the controls.
5. Verify the acceptance criteria:
   - **Next / Prev / Go-to** changes the projected slide with no perceptible lag.
   - **Timer** Start/Pause/Reset drives the host-owned clock.
   - **Draw on the slide** → strokes appear live on the projector at the right spot.
   - **Speaker notes** for the current slide show on the phone.
   - **Reconnect**: drop and reconnect → slide, timer, and notes are restored
     (the host is the source of truth).
   - **Wrong PIN** is rejected.

## Automated tests

```bash
cd mobile
./gradlew :core:jvmTest          # protocol round-trip + HostEndpoint + DeckNotes
./gradlew :transport:jvmTest     # handshake: correct PIN / bad PIN / version mismatch
./gradlew check                  # everything

cd ../web
npm test                         # TS protocol mirror + signaling validation/store
```

## Browser remote (`/remote`) — current caveat

The landing page, the `/remote` UI, and the signaling functions are built and
testable, **but browser↔desktop pairing does not complete yet**: the desktop
still needs a WebRTC peer that answers the signaling handshake (section 7 of the
web build prompt). Today the desktop speaks only the LAN WebSocket the **native**
remote uses. So: landing ✅, signaling endpoints ✅, full in-browser pairing ❌
until that desktop WebRTC peer exists.

## Internationalization (EN, PT-BR, ES, SV)

The UI ships in English, Brazilian Portuguese, Spanish, and Swedish (`sv` — the
language; `se` is Sweden's country code). Each surface has its own mechanism, and
all expose a language switcher that persists the choice:

- **Native app (`:app:shared`)** — strings live in
  `com/jacksonfdam/beam/i18n/Strings.kt` (`BeamStrings` data class + one bundle per
  `Language`). `LocalStrings` is a Compose `CompositionLocal` provided by
  `ProvideStrings { … }` at each window root; read `LocalStrings.current.<key>` in
  composables. `LanguageState.current` is the active language (in-memory, defaults
  to English) and `LanguageSelector()` switches it for the desktop host and the
  Android/iOS remote at once. **Add a language:** add a value to the `Language`
  enum and a matching `BeamStrings` bundle.
- **Web (`web/`)** — `web/lib/i18n.tsx` holds the typed message bundles, the
  `I18nProvider` (client; persists to `localStorage`, seeds from the browser
  language), the `useI18n()` hook (`t.<area>.<key>`), and `<LanguageSwitcher />`.
  The English bundle defines the shape; the others are typed `Dict` so a missing
  key fails the build. **Add a language:** add it to `LOCALES`/`LOCALE_LABELS` and
  a bundle matching `Dict`.
- **Landing (`landing/`)** — the `I18N` object inside `index.html`; add a locale
  key plus entries in `LOCALES`/`LABELS`.

Persisting the native app's language choice across launches (via `ConnectionStore`)
and detecting the system locale are small follow-ups; today it defaults to English
and is switched in-app.

## Landing site (`landing/`)

A standalone static marketing page (single `index.html`, no build step),
multilingual, with slots for demo videos. Deploy to Vercel with **Root Directory =
`landing`** and Framework Preset **Other**, or `cd landing && vercel --prod`. Drop
clips into `landing/videos/` (see `landing/README.md` for the expected filenames);
each card falls back to a labelled placeholder until its file exists. This is
separate from the in-app landing served by the Next.js `web/` project.

## Troubleshooting

- Gradle serving a stale file / error on a line you already fixed: `./gradlew --stop`
  then re-run, or add `--no-configuration-cache`.
- Android won't connect: confirm the same Wi‑Fi, the correct LAN IP (ignore a VPN
  IP if listed), and that the network doesn't isolate clients (AP isolation).

---

## Build status

- [x] Web: landing page, signaling functions, `lib/protocol.ts`, `/remote` UI (verified with build/lint/tests).
- [x] Mobile M2 — `:core` wire protocol + tests.
- [x] Mobile M3 — `:transport` Ktor client/server + handshake + tests.
- [x] Mobile M4 — `:pdf` rendering (Desktop/Android/iOS) + notes sidecar.
- [x] Mobile M5 — desktop presenter (load PDF, fullscreen projector, QR/IP/PIN, presenter view, ink).
- [x] Mobile M6–8 — mobile remote (connect, deck picker, navigation, notes, timer, drawing).
- [x] Internationalization — EN / PT-BR / ES / SV across the native app and the web (with switchers).
- [x] Standalone multilingual landing site (`landing/`) with demo-video slots.
- [ ] Web browser remote ↔ desktop (needs a WebRTC peer on the desktop host).
- [ ] Polish: QR camera scanning on mobile, error/empty states, accessibility pass.

## Privacy

No accounts, no cloud storage of decks/notes/strokes, no telemetry. On the native
flow everything stays on your LAN, peer-to-peer between your devices. The browser
remote uses a server only to exchange the initial WebRTC handshake (a session
code and connection details) with a short TTL — never slide content.
