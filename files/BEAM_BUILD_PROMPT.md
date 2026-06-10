# Build Prompt — Beam (cross-platform PDF presenter)

> `Beam` is a working name; if a final name is set, rename consistently across modules, package ids, app ids, README and the web app in a single dedicated commit.

You are building a complete, production-grade application from scratch. Follow every section below. Where a detail is unspecified, choose the simplest option consistent with the principles here and record the decision in the README.

---

## 1. Mission & product feel

Beam turns any exported PDF (Keynote, PowerPoint, Canva, Figma) into a flawless fullscreen presentation, controlled from a phone. No broken fonts, no layout surprises — the PDF renders exactly as designed. **Open → Present → Done.**

The product feel is **LocalSend**: local-first, no accounts, no cloud storage of user content, devices discover and talk to each other directly over the LAN, privacy by default. The only thing that may ever touch a server is the WebRTC signaling handshake for the optional browser remote — never slide content or annotations.

---

## 2. Platforms & roles

- **Desktop (Compose Multiplatform / JVM — macOS, Windows, Linux): presenter + host.** Renders the deck fullscreen on the external display, runs a presenter view, hosts the LAN WebSocket server, shows the connection QR / IP, paints the live ink overlay, and **owns the single source of truth**: current slide, timer, and speaker notes.
- **Mobile (Compose Multiplatform — Android + iOS): the remote.** Connects by scanning the QR or entering the host IP manually. Picks a deck, navigates, optionally controls the timer, optionally draws on the slide, and reads speaker notes in the presenter view.
- **Web (Node.js + Next.js on Vercel): landing + browser remote.** Public landing/download page first. A browser-based remote (later milestone) connects to the desktop via **WebRTC DataChannel**; a Vercel function brokers **signaling only** (SDP/ICE exchange keyed by the session code), backed by a TTL KV store (Vercel KV / Upstash) because Vercel serverless cannot hold a persistent WebSocket. After signaling, all data is P2P.

---

## 3. Tech stack (use only real, documented tooling — never invent a library, API, or CLI flag)

- Kotlin Multiplatform + Compose Multiplatform (Android, iOS, Desktop are stable targets).
- Ktor — server engine **CIO** on desktop; client engines **OkHttp** (Android) and **Darwin** (iOS); `WebSockets` plugin on both.
- `kotlinx.serialization` (JSON) for the wire protocol; `kotlinx.coroutines` / `Flow` for streams.
- Koin for dependency injection (multiplatform), or constructor injection where DI is overkill.
- PDF rendering via `expect`/`actual`: **Android** `android.graphics.pdf.PdfRenderer`; **iOS** PDFKit / `CGPDFDocument`; **Desktop** Apache PDFBox (render page → image).
- QR: render on desktop with a Compose Multiplatform QR composable (QRose). Scan on mobile behind an `expect`/`actual` (Android CameraX + ML Kit Barcode; iOS `AVCaptureMetadataOutput`).
- Gradle with a version catalog (`libs.versions.toml`); pin to current stable releases — do not hardcode versions you cannot verify.
- Web: Next.js (App Router) + TypeScript on Vercel; WebRTC DataChannel; a Vercel function for signaling.

If any required version, API, or flag is uncertain, look it up and use the documented form — do not guess.

---

## 4. Architecture principles (non-negotiable)

- **DRY:** the wire protocol is defined exactly once, in `:shared` (see `BeamProtocol.kt`). Both the desktop server and every client depend on that single definition. No duplicated message shapes, no parallel hand-rolled DTOs. The web app mirrors the same message names — keep them in one documented source.
- **SOLID:**
  - *SRP* — each module and class has one reason to change (rendering ≠ transport ≠ UI ≠ session state).
  - *OCP/DIP* — UI and session logic depend on abstractions (`PresenterClient`, `PresenterServer`, `PdfRenderer`), never on a concrete engine. Swapping Ktor for another transport must not touch UI.
  - *ISP* — small, focused interfaces (the transport contracts already split client vs server).
  - *LSP* — every `actual` honours the `expect` contract without surprises.
- **Clean code:** intention-revealing names, small functions, no dead code, no commented-out blocks left behind, no premature abstraction. Pure domain logic in `commonMain`, platform specifics isolated in `actual`.
- **Single source of truth:** the host owns slide index, timer and notes; clients render pushed state. A client that drops and reconnects loses nothing.

---

## 5. Module layout

```
:shared             domain models + wire protocol (BeamProtocol.kt) + BeamJson
:transport          PresenterClient / PresenterServer interfaces; Ktor actuals
:pdf                PdfRenderer expect/actual (Android/iOS/Desktop)
:feature-presenter  desktop presenter UI: fullscreen, presenter view, QR, ink overlay
:feature-remote     mobile remote UI: connect, deck picker, nav, notes, timer, drawing
:composeApp         per-platform entry points (androidApp, iosApp, desktopApp wiring)
web/                Next.js app (landing + browser remote + signaling function)
```

Keep `commonMain` free of platform imports. Platform code lives only in `androidMain` / `iosMain` / `jvmMain` / `wasmJsMain` as applicable.

---

## 6. Wire protocol

Use `BeamProtocol.kt` as the canonical contract (provided). Summary:

- Transport: JSON over WebSocket; polymorphic sealed hierarchies discriminated by `"type"` via `BeamJson`.
- **Client → Host:** `Hello(clientName, protocolVersion, pin?)`, `SelectDeck`, `Nav(NEXT/PREV/FIRST/LAST)`, `GoTo(index)`, `StrokeStart/StrokePoint/StrokeEnd`, `ClearInk`, `TimerCmd(START/PAUSE/RESET)`, `Ping`.
- **Host → Client:** `HelloAck(sessionName, hostVersion, decks)`, `HelloReject(reason)`, `DeckSelected`, `SlideChanged(index, total, notes?)`, `TimerState(elapsedMs, running)`, `Pong`, `HostError`.
- **Ink is normalized** (`NormPoint` in `0f..1f` relative to the slide content rect) and streamed (`start → point* → end`) so it draws live and maps to any projector resolution.
- **Notes ride on `SlideChanged`** — a plain PDF carries no speaker notes, so notes come from a host-side sidecar (e.g. a paired `.json`/markdown the user supplies). Define and document this sidecar format.

---

## 7. Connection & security

- Desktop binds the server, derives its LAN IPv4, and renders `HostEndpoint.toUri()` as a QR plus the raw `ip:port` as text.
- Mobile connects by scanning the QR **or** entering the host manually (parse via `HostEndpoint.parse`).
- A short **PIN** is shown on the presenter screen and verified in the handshake — without it, anyone on the same Wi-Fi could hijack the slides. Reject on PIN mismatch or protocol-version mismatch with `HelloReject`.
- On a handshake, always verify the peer is actually a Beam host (the `Hello`/`HelloAck` exchange) — never trust a bare open port.
- iOS: declare local-network usage; expect the system local-network permission prompt on first connect.

---

## 8. Core flows (acceptance criteria)

1. Desktop loads a PDF, shows QR + IP, enters fullscreen on the chosen display.
2. Phone connects (QR or IP + PIN), receives the deck list, selects a deck, presses Start.
3. Next/Prev/GoTo on the phone changes the slide on the projector with no perceptible lag.
4. Timer is optional: when shown, START/PAUSE/RESET drive a host-owned clock; the value survives a phone reconnect.
5. Drawing is optional: when enabled, finger strokes on the phone appear live on the projected slide at the correct position regardless of resolution; Clear wipes them.
6. Speaker notes for the current slide are visible on the phone's presenter view.
7. Disconnect/reconnect restores current slide, timer and notes from the host.

---

## 9. Web layer (Node.js / Vercel)

- **M-first:** public landing page (what Beam is, features, download links, "how to connect"). Static-friendly, fast, accessible.
- **Browser remote (later):** WebRTC DataChannel to the desktop, speaking the same message names as the Kotlin protocol. Vercel function does **signaling only** — exchange offer/answer/ICE candidates keyed by the session code, stored in a TTL KV (Vercel KV / Upstash); no slide content or strokes ever traverse the server. No persistent WebSocket on Vercel serverless.
- No accounts, no user-content persistence server-side.

---

## 10. Engineering workflow (hard requirements)

- **Micro-commits.** One logical change per commit, atomic, build-green at every commit. Conventional Commits style (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`). Scaffolding, each module, each feature, and each test arrives in its own small commit — not one giant drop.
- **No self-attribution.** Commit messages and code comments must read as human-authored. Do **not** mention any AI assistant, "generated by", "co-authored-by" an AI, or similar anywhere in history, comments, or docs.
- **Keep the README current as you go.** The README is part of every change that affects setup, architecture, or usage — update it in the same commit (or an adjacent `docs:` commit), never as an afterthought.
- **Tests where they earn their place:** protocol JSON round-trip for every message type, `HostEndpoint.parse` ↔ `toUri` round-trip, transport handshake (PIN accept/reject, version mismatch), normalized-coordinate mapping.

---

## 11. README requirements (keep updated throughout)

The README must always reflect reality and contain: a one-line pitch and the LocalSend-style philosophy; the platform/role table (desktop host, mobile remote, web landing/remote); the module map; build & run instructions per platform; how to connect (QR / IP / PIN); the sidecar notes format; and the local-first / privacy stance (what does and does not leave the device).

---

## 12. Suggested milestone order (each milestone = several micro-commits)

1. Repo scaffold, version catalog, module skeletons, empty README.
2. `:shared` protocol + `BeamJson` + serialization round-trip tests.
3. `:transport` Ktor server/client actuals + handshake + PIN + connection state machine.
4. `:pdf` rendering actuals (Android, iOS, Desktop).
5. Desktop presenter: load PDF, fullscreen on external display, QR/IP, presenter view.
6. Mobile remote: connect (QR + manual), deck picker, navigation, notes view.
7. Live ink overlay (normalized strokes → desktop Canvas).
8. Host-owned timer + reconnect-survives-state.
9. Web: Next.js landing on Vercel.
10. Web: browser remote via WebRTC + Vercel signaling function.
11. Polish: error states, empty states, permissions UX, accessibility, README pass.

---

## 13. Non-goals

No slideshow *editor* (Beam presents, it does not author). No accounts or login. No cloud storage of decks, notes, or strokes. No telemetry. No dependence on internet connectivity for the core present-from-phone flow.
