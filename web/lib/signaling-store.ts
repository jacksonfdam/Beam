/*
 * TTL key/value store for WebRTC signaling.
 *
 * Stores ONLY ephemeral SDP and ICE candidates, keyed by session code, with a
 * short expiry. No slide content, notes, or ink ever passes through here.
 *
 * Two backends, chosen at runtime:
 *   - Redis REST (Vercel KV / Upstash) when the REST env vars are present.
 *     Uses the documented Upstash REST API (a POST of a command array with a
 *     Bearer token), so no extra dependency is required.
 *   - An in-memory map otherwise (local dev). Per-instance and non-durable,
 *     which is fine for a single dev process; production must set the env vars.
 */

export const SIGNAL_TTL_SECONDS = 180; // ~3 minutes, then keys auto-expire.

export type Role = "offerer" | "answerer";

export function oppositeRole(role: Role): Role {
  return role === "offerer" ? "answerer" : "offerer";
}

export interface SignalingStore {
  /** Store a single value (offer/answer SDP) under a key, with TTL. */
  setValue(key: string, value: string): Promise<void>;
  /** Read a single value, or null if absent/expired. */
  getValue(key: string): Promise<string | null>;
  /** Append a value to a list (ICE candidates), refreshing the list TTL. */
  pushToList(key: string, value: string): Promise<void>;
  /** Return all list values and clear the list (so each poll sees only new). */
  drainList(key: string): Promise<string[]>;
}

// ---------------------------------------------------------------------------
// In-memory backend (dev / fallback)
// ---------------------------------------------------------------------------

interface Entry {
  value: string | string[];
  expiresAt: number;
}

class InMemoryStore implements SignalingStore {
  private readonly map = new Map<string, Entry>();

  private live(key: string): Entry | undefined {
    const e = this.map.get(key);
    if (!e) return undefined;
    if (Date.now() > e.expiresAt) {
      this.map.delete(key);
      return undefined;
    }
    return e;
  }

  async setValue(key: string, value: string): Promise<void> {
    this.map.set(key, { value, expiresAt: ttlDeadline() });
  }

  async getValue(key: string): Promise<string | null> {
    const e = this.live(key);
    return e && typeof e.value === "string" ? e.value : null;
  }

  async pushToList(key: string, value: string): Promise<void> {
    const e = this.live(key);
    const list = e && Array.isArray(e.value) ? e.value : [];
    list.push(value);
    this.map.set(key, { value: list, expiresAt: ttlDeadline() });
  }

  async drainList(key: string): Promise<string[]> {
    const e = this.live(key);
    if (!e || !Array.isArray(e.value)) return [];
    this.map.delete(key);
    return e.value;
  }
}

function ttlDeadline(): number {
  return Date.now() + SIGNAL_TTL_SECONDS * 1000;
}

// ---------------------------------------------------------------------------
// Redis REST backend (Vercel KV / Upstash)
// ---------------------------------------------------------------------------

interface RedisRestConfig {
  url: string;
  token: string;
}

function redisRestConfig(): RedisRestConfig | null {
  const url = process.env.KV_REST_API_URL ?? process.env.UPSTASH_REDIS_REST_URL;
  const token =
    process.env.KV_REST_API_TOKEN ?? process.env.UPSTASH_REDIS_REST_TOKEN;
  if (url && token) return { url, token };
  return null;
}

class RedisRestStore implements SignalingStore {
  constructor(private readonly cfg: RedisRestConfig) {}

  private async command<T>(args: (string | number)[]): Promise<T> {
    const res = await fetch(this.cfg.url, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${this.cfg.token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(args),
      cache: "no-store",
    });
    if (!res.ok) {
      throw new Error(`Signaling store error: ${res.status}`);
    }
    const json = (await res.json()) as { result: T };
    return json.result;
  }

  async setValue(key: string, value: string): Promise<void> {
    await this.command(["SET", key, value, "EX", SIGNAL_TTL_SECONDS]);
  }

  async getValue(key: string): Promise<string | null> {
    return this.command<string | null>(["GET", key]);
  }

  async pushToList(key: string, value: string): Promise<void> {
    await this.command(["RPUSH", key, value]);
    await this.command(["EXPIRE", key, SIGNAL_TTL_SECONDS]);
  }

  async drainList(key: string): Promise<string[]> {
    const values = await this.command<string[] | null>(["LRANGE", key, 0, -1]);
    if (values && values.length > 0) {
      await this.command(["DEL", key]);
    }
    return values ?? [];
  }
}

// ---------------------------------------------------------------------------
// Factory
// ---------------------------------------------------------------------------

let cached: SignalingStore | null = null;

export function getSignalingStore(): SignalingStore {
  if (cached) return cached;
  const cfg = redisRestConfig();
  cached = cfg ? new RedisRestStore(cfg) : new InMemoryStore();
  return cached;
}

/** True when a durable backend is configured (used for health/diagnostics). */
export function hasDurableStore(): boolean {
  return redisRestConfig() !== null;
}

// ---------------------------------------------------------------------------
// Key helpers — one namespace per session code.
// ---------------------------------------------------------------------------

export const keys = {
  offer: (code: string) => `beam:offer:${code}`,
  answer: (code: string) => `beam:answer:${code}`,
  ice: (code: string, role: Role) => `beam:ice:${code}:${role}`,
};
