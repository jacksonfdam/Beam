import {
  getSignalingStore,
  keys,
  oppositeRole,
  type Role,
} from "@/lib/signaling-store";
import { clientId, rateLimit } from "@/lib/rate-limit";
import {
  corsHeaders,
  isValidCandidate,
  isValidRole,
  isValidSessionCode,
  json,
  preflight,
  readJsonBody,
} from "@/lib/signaling-http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const WRITE_LIMIT = 240; // candidates can trickle in quickly
const READ_LIMIT = 240;

export async function OPTIONS(req: Request) {
  return preflight(req.headers.get("origin"));
}

export async function POST(req: Request) {
  const origin = req.headers.get("origin");
  const headers = corsHeaders(origin);

  const rl = rateLimit(`ice:post:${clientId(req.headers)}`, WRITE_LIMIT, 60);
  if (!rl.ok) {
    return json({ error: "rate_limited" }, { status: 429, headers, retryAfter: rl.retryAfterSeconds });
  }

  const body = await readJsonBody(req);
  if (body === null || typeof body !== "object") {
    return json({ error: "invalid_body" }, { status: 400, headers });
  }
  const { sessionCode, role, candidate } = body as Record<string, unknown>;
  if (!isValidSessionCode(sessionCode) || !isValidRole(role) || !isValidCandidate(candidate)) {
    return json({ error: "invalid_input" }, { status: 400, headers });
  }

  // Append to the poster's own queue; the opposite peer drains it.
  await getSignalingStore().pushToList(
    keys.ice(sessionCode, role as Role),
    JSON.stringify(candidate),
  );
  return json({ ok: true }, { headers });
}

export async function GET(req: Request) {
  const origin = req.headers.get("origin");
  const headers = corsHeaders(origin);

  const rl = rateLimit(`ice:get:${clientId(req.headers)}`, READ_LIMIT, 60);
  if (!rl.ok) {
    return json({ error: "rate_limited" }, { status: 429, headers, retryAfter: rl.retryAfterSeconds });
  }

  const params = new URL(req.url).searchParams;
  const code = params.get("code");
  const role = params.get("role");
  if (!isValidSessionCode(code) || !isValidRole(role)) {
    return json({ error: "invalid_input" }, { status: 400, headers });
  }

  // `role` is the caller's own role; drain candidates produced by the other side.
  const raw = await getSignalingStore().drainList(
    keys.ice(code, oppositeRole(role as Role)),
  );
  const candidates = raw.map((s) => safeParse(s));
  return json({ candidates }, { headers });
}

function safeParse(s: string): unknown {
  try {
    return JSON.parse(s);
  } catch {
    return s;
  }
}
