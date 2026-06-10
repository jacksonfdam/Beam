/*
 * Best-effort in-memory rate limiter (fixed window per key).
 *
 * On serverless this is per-instance, so it is a mitigation, not a hard global
 * guarantee — documented as such. Pair with the short TTL on stored keys so
 * abuse cannot accumulate state.
 */

interface Window {
  count: number;
  resetAt: number;
}

const windows = new Map<string, Window>();

export interface RateLimitResult {
  ok: boolean;
  remaining: number;
  retryAfterSeconds: number;
}

export function rateLimit(
  key: string,
  limit: number,
  windowSeconds: number,
): RateLimitResult {
  const now = Date.now();
  const w = windows.get(key);

  if (!w || now > w.resetAt) {
    windows.set(key, { count: 1, resetAt: now + windowSeconds * 1000 });
    return { ok: true, remaining: limit - 1, retryAfterSeconds: 0 };
  }

  if (w.count >= limit) {
    return {
      ok: false,
      remaining: 0,
      retryAfterSeconds: Math.ceil((w.resetAt - now) / 1000),
    };
  }

  w.count += 1;
  return { ok: true, remaining: limit - w.count, retryAfterSeconds: 0 };
}

/** Derive a client identifier from forwarded headers, falling back to a label. */
export function clientId(headers: Headers): string {
  const fwd = headers.get("x-forwarded-for");
  if (fwd) return fwd.split(",")[0].trim();
  return headers.get("x-real-ip") ?? "unknown";
}
