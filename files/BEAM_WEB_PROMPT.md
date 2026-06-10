# Build Prompt — Beam Web (`web/`)

This is the web layer of **Beam**, the local-first cross-platform PDF presenter. Read the main build prompt (`BEAM_BUILD_PROMPT.md`) and the wire protocol (`BeamProtocol.kt`) first — they are canonical. This document expands section 9 of the main prompt and governs everything under `web/`.

Same engineering rules apply: DRY, SOLID, clean code; micro-commits (atomic, build-green, Conventional Commits); **no self-attribution anywhere** in history, comments, or docs; keep the README current in the same change.

---

## 1. Role of the web layer

Two responsibilities, shipped in this order:

1. **Landing page** — what Beam is, its features, download links, and how to connect. The public face.
2. **Browser remote** — a phone-or-laptop browser acting as the remote control for a Beam desktop host, for people who don't want to install the native app.

The product stance is **local-first, LocalSend-style**: no accounts, no user content stored server-side, no telemetry. The server's *only* job for the remote is brokering the WebRTC signaling handshake. Slide content, navigation, notes, and ink travel **peer-to-peer over the DataChannel** and never touch Vercel.

---

## 2. Stack

- Next.js (App Router) + TypeScript, deployed on Vercel.
- Styling: Tailwind CSS (or CSS Modules) — pick one and stay consistent.
- WebRTC (`RTCPeerConnection` + `RTCDataChannel`) for the remote.
- Signaling: Vercel Functions (route handlers under `app/api/...`) backed by a TTL key-value store (**Vercel KV / Upstash Redis**) — Vercel serverless cannot hold a persistent WebSocket, so signaling is a short HTTP exchange with expiring keys.
- No database for user content. No auth provider.

Use only real, documented APIs. If a Vercel/Next API detail is uncertain, check the current docs rather than guessing.

---

## 3. Landing page (`/`)

Build first; it can ship before the remote exists.

- Hero: one-line pitch ("Turn any exported PDF into a flawless fullscreen presentation"), the `Open → Present → Done` motif, primary download CTAs (App Store / macOS / Windows / Linux as available; placeholders allowed but clearly marked TODO).
- Features section reflecting the real product: multi-screen & external display support, phone-as-remote (navigation, timer, live drawing, speaker notes), exact-as-designed rendering, fully local / private.
- "How to connect" — the QR/IP + PIN flow, in plain language.
- Footer: privacy stance (what does and does not leave the device), links.
- Requirements: responsive, accessible (semantic HTML, keyboard-navigable, sufficient contrast), fast (good Lighthouse scores), correct Open Graph / meta tags for link previews.

---

## 4. Browser remote (`/remote`)

A browser peer that pairs with a desktop host and then speaks the Beam protocol over a DataChannel.

**Pairing flow**

1. Desktop host displays a **session code** (the same short code/PIN shown on the presenter screen).
2. In `/remote`, the user enters that session code (primary path). Optional enhancement: scan the QR via `getUserMedia` + a JS QR decoder — manual entry must always work as the fallback.
3. The browser creates an `RTCPeerConnection`, opens a `DataChannel`, generates an SDP **offer**, and POSTs it to signaling keyed by the session code.
4. The desktop host fetches the offer, answers, posts the answer back; both sides exchange ICE candidates through signaling.
5. DataChannel opens → signaling keys can expire. From here everything is P2P.

**On the open DataChannel**, speak the exact same JSON messages as the native clients (same `"type"` discriminator, same field names as `BeamProtocol.kt`):

- Send: `hello` (with `pin`), `select_deck`, `nav`, `goto`, `stroke_start`/`stroke_point`/`stroke_end`, `clear_ink`, `timer`, `ping`.
- Receive and render: `hello_ack` (decks), `hello_reject`, `deck_selected`, `slide_changed` (show notes for the presenter view), `timer_state`, `pong`, `host_error`.

**Remote UI:** deck picker, prev/next + go-to, current-slide / total indicator, speaker-notes pane fed by `slide_changed.notes`, optional timer controls, optional drawing surface that emits normalized `NormPoint`s (`0..1` relative to the slide rect) exactly like the native remote.

---

## 5. Protocol mirror (DRY across languages)

`BeamProtocol.kt` is the single source of truth. In `web/`, keep one TypeScript module (e.g. `lib/protocol.ts`) that mirrors the message types, with a header comment naming `BeamProtocol.kt` as canonical and a note that the two must change together. Do not scatter message shapes across components. Prefer a single discriminated-union type per direction (`ClientMessage`, `HostMessage`) matching the Kotlin sealed hierarchies.

---

## 6. Signaling API (Vercel Functions)

Stateless HTTP, keyed by `sessionCode`, values in a TTL store (~2–5 min expiry). Payloads carry **only** SDP and ICE — never content.

- `POST /api/signal/offer` — body `{ sessionCode, sdp }`; store under the code.
- `GET  /api/signal/offer?code=…` — host retrieves the pending offer.
- `POST /api/signal/answer` — body `{ sessionCode, sdp }`.
- `GET  /api/signal/answer?code=…` — browser retrieves the answer.
- `POST /api/signal/ice` — body `{ sessionCode, role: "offerer"|"answerer", candidate }`; append.
- `GET  /api/signal/ice?code=…&role=…` — drain candidates for the opposite role.

Requirements: short polling on the client (e.g. ~1s) until the DataChannel opens, then stop; rate-limit and size-limit every endpoint; validate inputs; CORS locked to the deployed origin; keys auto-expire so nothing lingers.

**STUN/TURN:** when both peers are on the same LAN, host ICE candidates connect directly — a public STUN server is enough and no media relay is needed. Cross-network use would require TURN; treat TURN as out of scope / optional and document the limitation.

---

## 7. Desktop-side integration note (affects the native app, not `web/`)

To serve the browser remote, the **desktop host must implement a WebRTC peer** in addition to its Ktor LAN WebSocket server (e.g. a JVM WebRTC library such as `dev.onvoid.webrtc` / webrtc-java). Both transports feed the *same* session logic and the *same* message types — the transport is an implementation detail behind the existing `PresenterServer` abstraction (this is exactly the DIP/OCP win from the main prompt). Flag this in the main app's transport module; do not duplicate session logic per transport.

---

## 8. Security & privacy

- Pairing requires the session code/PIN shown on the host; a wrong PIN gets `hello_reject`.
- Signaling stores only ephemeral SDP/ICE with TTL; no content, no logs of content.
- No accounts, no analytics, no third-party trackers on the remote. Landing analytics, if any, must be privacy-respecting and disclosed.

---

## 9. Milestones (each = several micro-commits)

1. Next.js + TypeScript scaffold, deploy a placeholder to Vercel, README for `web/`.
2. Landing page (hero, features, how-to-connect, footer, OG tags, a11y, performance pass).
3. Signaling functions + TTL KV store, with input validation, rate limiting, and tests.
4. `lib/protocol.ts` mirror of `BeamProtocol.kt`.
5. `/remote`: pairing via session code → WebRTC offer/answer/ICE → DataChannel open.
6. Remote control surface: deck picker, navigation, slide indicator, notes pane.
7. Optional timer controls + optional normalized drawing surface.
8. Polish: error/empty/disconnect states, reconnect, responsive + a11y pass, README refresh.

---

## 10. Acceptance criteria

- Landing page is live on Vercel, responsive, accessible, fast.
- A browser at `/remote` pairs with a desktop host using the session code and opens a DataChannel with no content passing through Vercel.
- Navigation, slide indicator, and speaker notes work over the DataChannel; optional timer and drawing behave identically to the native remote.
- A wrong PIN is rejected; signaling keys expire after the session.
- Nothing in the repo, commit history, comments, or docs attributes the work to an AI.

---

## 11. Non-goals

No slideshow editor. No accounts or login. No server-side storage of decks, notes, or strokes. No telemetry. The native present-from-phone flow must never depend on this web layer or on internet connectivity.
