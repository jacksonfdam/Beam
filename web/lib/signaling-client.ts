/*
 * Client-side wrappers over the signaling HTTP API. These carry ONLY SDP and
 * ICE candidates — never slide content. Once the DataChannel opens the caller
 * stops polling and everything is peer-to-peer.
 */

import type { Role } from "./signaling-store";

const BASE = "/api/signal";

async function postJson(path: string, body: unknown): Promise<void> {
  const res = await fetch(`${BASE}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`Signaling POST ${path} failed: ${res.status}`);
}

async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { method: "GET" });
  if (!res.ok) throw new Error(`Signaling GET ${path} failed: ${res.status}`);
  return (await res.json()) as T;
}

export function postOffer(sessionCode: string, sdp: string): Promise<void> {
  return postJson("/offer", { sessionCode, sdp });
}

export function getOffer(sessionCode: string): Promise<{ sdp: string | null }> {
  return getJson(`/offer?code=${encodeURIComponent(sessionCode)}`);
}

export function postAnswer(sessionCode: string, sdp: string): Promise<void> {
  return postJson("/answer", { sessionCode, sdp });
}

export function getAnswer(sessionCode: string): Promise<{ sdp: string | null }> {
  return getJson(`/answer?code=${encodeURIComponent(sessionCode)}`);
}

export function postIce(
  sessionCode: string,
  role: Role,
  candidate: RTCIceCandidateInit,
): Promise<void> {
  return postJson("/ice", { sessionCode, role, candidate });
}

export function getIce(
  sessionCode: string,
  role: Role,
): Promise<{ candidates: RTCIceCandidateInit[] }> {
  return getJson(
    `/ice?code=${encodeURIComponent(sessionCode)}&role=${role}`,
  );
}
