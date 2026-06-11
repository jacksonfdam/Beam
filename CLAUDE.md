# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What Beam is

Beam projects an exported PDF fullscreen from a computer and turns a phone (or browser) into the
remote — slides, live ink, spotlight, speaker notes, and a shared timer. **Local-first**: no accounts, no cloud storage of user
content, no telemetry. Devices discover and talk to each other directly over the LAN. The *only*
thing that may ever touch a server is the WebRTC signaling handshake for the optional browser
remote — never slide content, notes, or ink. This constraint is load-bearing: do not introduce
account systems, analytics, or server-side storage of decks/notes/strokes.

## Monorepo layout

```
files/    BeamProtocol.kt — the wire-protocol spec copy (canonical lives in :core). Read-only reference.
mobile/   Kotlin Multiplatform — desktop presenter/host + Android/iOS remote. Gradle.
web/      Next.js — landing page + browser remote + WebRTC signaling. npm.
```

`mobile/` and `web/` are independent builds with their own toolchains and their own READMEs/CLAUDE
context. There is no top-level build that ties them together.

## The wire protocol is defined twice and MUST stay in sync

This is the single most important cross-cutting fact in the repo:

- **Canonical:** `mobile/core/src/commonMain/kotlin/com/jacksonfdam/beam/protocol/` (Kotlin, package
  `com.jacksonfdam.beam.protocol`). `files/BeamProtocol.kt` is the spec copy.
- **Mirror:** `web/lib/protocol.ts` (TypeScript).

Both describe the same JSON-over-the-wire messages: polymorphic sealed hierarchies discriminated by
a `"type"` key (Kotlin `@SerialName` ↔ TS `type` field), `ignoreUnknownKeys` for forward-compat, ink
as normalized `NormPoint` (x/y in `0f..1f` relative to slide content, not screen). **Any change to a
message type, enum value (e.g. `NavAction.NEXT`), or field name must be applied in both files in the
same change**, or native and browser remotes silently diverge.

Message families: Client→Host (`hello` w/ PIN, `select_deck`, `nav`, `goto`, `stroke_*`, `clear_ink`,
`timer`, `set_mode`, `set_interacting`, `spotlight`, `ping`) and Host→Client (`hello_ack`,
`hello_reject`, `deck_selected`, `slide_changed` w/ notes, `timer_state`, `slide_image` base64 PNG,
`mode_changed`, `error`).

## Two transports, one authoritative host

The **desktop app is the host and the single source of truth** for all state (current slide, timer,
notes, ink strokes, present mode). Remotes send *intents*; the host applies them and broadcasts the
resulting `HostMessage` to every connected remote. On reconnect, the host replays current
slide/image/timer/mode — so a dropped remote loses nothing.

- **Native remotes (Android/iOS):** Ktor **WebSocket** on the LAN, path `/cue`, default port `53317`,
  authenticated by a PIN handshake (wrong PIN → `hello_reject`). This is the primary, fully-working path.
- **Browser remote (web `/remote`):** **WebRTC DataChannel**, peer-to-peer after a signaling handshake
  brokered by the Next.js `/api/signal/*` routes. The browser is the offerer.

> **Known gap:** browser↔desktop pairing does not complete end-to-end yet — the desktop has no WebRTC
> peer that answers the signaling handshake (it currently speaks only the native WebSocket). The
> landing page, `/remote` UI, and signaling endpoints all work and are tested; full in-browser pairing
> does not. Don't assume the browser remote can drive a live deck until that desktop peer exists.

## Architecture — mobile (Kotlin Multiplatform)

Dependency direction (respect it — UI never imports Ktor directly):

```
Compose UI  →  RemoteController / HostSession  →  PresenterClient/PresenterServer (abstractions in :core)
                                                          ↑ implemented by
                                                   Ktor*  (concrete, in :transport)
```

Modules (`settings.gradle.kts`): `:core` (protocol + transport interfaces + notes sidecar),
`:transport` (Ktor `KtorPresenterClient` multiplatform + `KtorPresenterServer` JVM/CIO + handshake),
`:pdf` (`PdfDocument` expect/actual: Desktop PDFBox, Android `PdfRenderer`, iOS CoreGraphics),
`:app:shared` (shared Compose remote UI), `:app:desktopApp` (presenter/host), `:app:androidApp`,
`:app:iosApp` (Xcode), `:server` (Ktor scaffold, currently a stub).

Platform seams use `expect/actual`: PDF rendering, QR scanning (Android GMS Code Scanner / iOS
AVFoundation / manual-entry fallback), `ConnectionStore` (Android SharedPreferences; Noop elsewhere),
`localIpAddress()` (host advertises, remote returns null), and the HTTP engine (OkHttp/Darwin/CIO).
State is `MutableStateFlow` snapshots updated via immutable reducers.

**Speaker-notes sidecar:** a PDF has no notes, so the host pairs `<deck>.pdf` with `<deck>.notes.json`
(`DeckNotes`, zero-based slide-index keys) and pushes the current note on `slide_changed`. Host-only;
never leaves the device.

```json
{ "version": 1, "notes": { "0": "Open warm", "3": "Pause for the demo" } }
```

## Architecture — web (Next.js, App Router)

- `app/page.tsx` — static landing page. `app/remote/page.tsx` — client-side browser remote.
- `lib/beam-remote.ts` — the WebRTC peer (offerer): `RTCPeerConnection` + `cue` DataChannel + SDP/ICE
  polling. `components/remote/useBeamRemote.ts` drives the UI state machine.
- `app/api/signal/{offer,answer,ice}/route.ts` — the signaling broker. It stores **only** ephemeral
  SDP/ICE keyed by session code with a ~3-minute TTL, rate-limited per client IP. Backend is
  `lib/signaling-store.ts`: Redis REST (Vercel KV / Upstash) in prod, in-memory `Map` in dev. Once the
  DataChannel opens, polling stops and everything is peer-to-peer.

The session code doubles as the Beam PIN. STUN-only (no TURN), so cross-network use is out of scope.

## Commands

### web/ (Node 20+)
```bash
cd web
npm install
npm run dev          # http://localhost:3000 — landing at /, remote at /remote
npm run build
npm run lint         # eslint .
npm test             # vitest run — protocol mirror + signaling validation/store
npm run test:watch
npx vitest run lib/protocol.test.ts            # a single test file
npx vitest run -t "round-trips a Nav message"  # a single test by name
```
Tests are unit-only (Node env, network mocked at the fetch/API layer): `protocol.test.ts`,
`signaling-store.test.ts`, `signaling-http.test.ts`.

For production signaling on Vercel, set `KV_REST_API_URL` + `KV_REST_API_TOKEN` (or the `UPSTASH_*`
pair) and `SIGNAL_ALLOWED_ORIGIN`. Without them the in-memory store is used (dev only).

### mobile/ (JDK 17+, Android SDK via local.properties, Xcode for iOS)
```bash
cd mobile
./gradlew :app:desktopApp:run            # desktop presenter/host (or :hotRun --auto for UI iteration)
./gradlew :app:androidApp:installDebug   # Android remote to a connected device/emulator
./gradlew :server:run                    # Ktor scaffold
# iOS: open mobile/app/iosApp in Xcode and run

./gradlew check                          # everything
./gradlew :core:jvmTest                  # protocol round-trip + HostEndpoint + DeckNotes
./gradlew :transport:jvmTest             # handshake: correct PIN / bad PIN / version mismatch
./gradlew :transport:jvmTest --tests "*HandshakeTest"                 # a single test class
./gradlew :transport:jvmTest --tests "*HandshakeTest.acceptsCorrectPin"  # a single test method
```

Gradle gotcha: configuration caching is on. If Gradle serves a stale file or errors on a line you've
already fixed, run `./gradlew --stop` and re-run, or add `--no-configuration-cache`.

### Native end-to-end (the primary working path)
Run `:app:desktopApp:run`, click **Open a PDF**; on a phone on the **same Wi-Fi** open Beam, enter the
IP + PIN shown on the desktop, pick the deck. Verify next/prev/goto, host-owned timer, live ink on the
projector, speaker notes on the phone, reconnect restores state, wrong PIN rejected.
