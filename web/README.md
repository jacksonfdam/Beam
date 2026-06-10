# Beam — Web

The web layer of **Beam**, the local-first cross-platform PDF presenter. Two
responsibilities, shipped in this order:

1. **Landing page (`/`)** — what Beam is, its features, download links, and how
   to connect. The public face.
2. **Browser remote (`/remote`)** — a phone-or-laptop browser acting as the
   remote control for a Beam desktop host, for people who don't want to install
   the native app.

## Philosophy

Local-first, LocalSend-style: no accounts, no user content stored server-side,
no telemetry. The server's only job for the remote is brokering the WebRTC
signaling handshake. Slide content, navigation, notes, and ink travel
peer-to-peer over the DataChannel and never touch the server.

## Stack

- Next.js (App Router) + TypeScript, deployed on Vercel.
- Tailwind CSS for styling.
- WebRTC (`RTCPeerConnection` + `RTCDataChannel`) for the remote.
- Signaling via Vercel Functions (route handlers under `app/api/...`) backed by
  a TTL key-value store. No database for user content, no auth provider.

## Develop

```bash
npm install
npm run dev      # http://localhost:3000
npm run build    # production build
npm run lint     # eslint
npm test         # vitest
```

## Layout

```
app/
  layout.tsx              root layout, metadata, Open Graph tags
  page.tsx                landing page (/)
  remote/page.tsx         browser remote (/remote)
  api/signal/*/route.ts   WebRTC signaling endpoints (offer/answer/ice)
components/
  landing/                landing sections
  remote/                 pairing form, control surface, drawing, hook
lib/
  protocol.ts             TypeScript mirror of BeamProtocol.kt (canonical)
  beam-remote.ts          the browser WebRTC peer (offerer)
  signaling-client.ts     fetch wrappers over the signaling API
  signaling-store.ts      TTL store (Redis REST / in-memory fallback)
  signaling-http.ts       input validation, size limits, CORS, JSON helpers
  rate-limit.ts           best-effort in-memory rate limiter
  *.test.ts               vitest suites
```

## Protocol source of truth

`../files/BeamProtocol.kt` is canonical. `lib/protocol.ts` mirrors the message
types and must change together with it.

## Pairing & privacy

The session code shown on the presenter screen is entered in `/remote`. It is
used both to pair through signaling and as the PIN in the Beam handshake — a
wrong code is rejected with `hello_reject`. The browser is the WebRTC offerer:
it posts an SDP offer, polls for the host's answer, and trickles ICE — all
keyed by the session code. Once the DataChannel opens, polling stops and
everything (navigation, notes, timer, ink) is peer-to-peer. The signaling store
holds only SDP/ICE with a short TTL; no slide content ever touches the server.

## Configuration

Signaling uses a Redis-compatible REST store in production and an in-memory map
locally. Set either pair of env vars on Vercel to enable the durable backend:

```
KV_REST_API_URL / KV_REST_API_TOKEN          # Vercel KV
UPSTASH_REDIS_REST_URL / UPSTASH_REDIS_REST_TOKEN
SIGNAL_ALLOWED_ORIGIN                          # lock CORS to the deployed origin
```

**STUN/TURN:** on a shared LAN, host ICE candidates connect directly, so the
public STUN server is enough. Cross-network pairing would require a TURN relay,
which is out of scope here.

## Status

- [x] Milestone 1 — Next.js + TypeScript scaffold.
- [x] Milestone 2 — Landing page.
- [x] Milestone 3 — Signaling functions + TTL store (validation, rate limiting, tests).
- [x] Milestone 4 — `lib/protocol.ts` mirror.
- [x] Milestone 5 — `/remote` pairing + DataChannel.
- [x] Milestone 6 — Remote control surface.
- [x] Milestone 7 — Optional timer + drawing.
- [ ] Milestone 8 — Polish + a11y pass (ongoing).
