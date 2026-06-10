/*
 * Shared helpers for the signaling route handlers: input validation, size
 * limits, JSON responses, and CORS locked to the deployed origin.
 */

import { NextResponse } from "next/server";

export const SESSION_CODE_RE = /^[A-Za-z0-9]{4,16}$/;
export const MAX_SDP_CHARS = 20_000;
export const MAX_CANDIDATE_CHARS = 4_000;
export const MAX_BODY_BYTES = 32_000;

export function isValidSessionCode(code: unknown): code is string {
  return typeof code === "string" && SESSION_CODE_RE.test(code);
}

export function isValidSdp(sdp: unknown): sdp is string {
  return typeof sdp === "string" && sdp.length > 0 && sdp.length <= MAX_SDP_CHARS;
}

export function isValidRole(role: unknown): role is "offerer" | "answerer" {
  return role === "offerer" || role === "answerer";
}

/** ICE candidates may be a string or RTCIceCandidateInit-shaped object. */
export function isValidCandidate(candidate: unknown): boolean {
  if (candidate == null) return false;
  if (typeof candidate === "string") return candidate.length <= MAX_CANDIDATE_CHARS;
  if (typeof candidate === "object") {
    return JSON.stringify(candidate).length <= MAX_CANDIDATE_CHARS;
  }
  return false;
}

/** Read and size-limit a JSON body. Returns null on oversize or malformed. */
export async function readJsonBody(req: Request): Promise<unknown | null> {
  const text = await req.text();
  if (text.length > MAX_BODY_BYTES) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

// CORS — default to same-origin only. If an explicit origin is configured,
// echo it for matching requests so the deployed site can call its own API.
function allowedOrigin(): string | null {
  return (
    process.env.SIGNAL_ALLOWED_ORIGIN ??
    process.env.NEXT_PUBLIC_SITE_ORIGIN ??
    null
  );
}

export function corsHeaders(reqOrigin: string | null): Headers {
  const headers = new Headers();
  const allowed = allowedOrigin();
  if (allowed && reqOrigin === allowed) {
    headers.set("Access-Control-Allow-Origin", allowed);
    headers.set("Vary", "Origin");
    headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    headers.set("Access-Control-Allow-Headers", "Content-Type");
  }
  return headers;
}

export function json(
  body: unknown,
  init: { status?: number; headers?: Headers; retryAfter?: number } = {},
): NextResponse {
  const headers = init.headers ?? new Headers();
  headers.set("Cache-Control", "no-store");
  if (init.retryAfter) headers.set("Retry-After", String(init.retryAfter));
  return NextResponse.json(body as object, {
    status: init.status ?? 200,
    headers,
  });
}

export function preflight(reqOrigin: string | null): NextResponse {
  return new NextResponse(null, { status: 204, headers: corsHeaders(reqOrigin) });
}
