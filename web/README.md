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
  layout.tsx            root layout, metadata, Open Graph tags
  page.tsx              landing page (/)
  remote/page.tsx       browser remote (/remote)
  api/signal/*/route.ts WebRTC signaling endpoints (offer/answer/ice)
lib/
  protocol.ts           TypeScript mirror of BeamProtocol.kt (canonical)
```

## Protocol source of truth

`../files/BeamProtocol.kt` is canonical. `lib/protocol.ts` mirrors the message
types and must change together with it.

## Status

- [x] Milestone 1 — Next.js + TypeScript scaffold.
- [ ] Milestone 2 — Landing page.
- [ ] Milestone 3 — Signaling functions + TTL store.
- [ ] Milestone 4 — `lib/protocol.ts` mirror.
- [ ] Milestone 5 — `/remote` pairing + DataChannel.
- [ ] Milestone 6 — Remote control surface.
- [ ] Milestone 7 — Optional timer + drawing.
- [ ] Milestone 8 — Polish + a11y pass.
