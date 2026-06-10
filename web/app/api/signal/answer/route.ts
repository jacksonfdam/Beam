import { getSignalingStore, keys } from "@/lib/signaling-store";
import { clientId, rateLimit } from "@/lib/rate-limit";
import {
  corsHeaders,
  isValidSdp,
  isValidSessionCode,
  json,
  preflight,
  readJsonBody,
} from "@/lib/signaling-http";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const WRITE_LIMIT = 60;
const READ_LIMIT = 240;

export async function OPTIONS(req: Request) {
  return preflight(req.headers.get("origin"));
}

export async function POST(req: Request) {
  const origin = req.headers.get("origin");
  const headers = corsHeaders(origin);

  const rl = rateLimit(`answer:post:${clientId(req.headers)}`, WRITE_LIMIT, 60);
  if (!rl.ok) {
    return json({ error: "rate_limited" }, { status: 429, headers, retryAfter: rl.retryAfterSeconds });
  }

  const body = await readJsonBody(req);
  if (body === null || typeof body !== "object") {
    return json({ error: "invalid_body" }, { status: 400, headers });
  }
  const { sessionCode, sdp } = body as Record<string, unknown>;
  if (!isValidSessionCode(sessionCode) || !isValidSdp(sdp)) {
    return json({ error: "invalid_input" }, { status: 400, headers });
  }

  await getSignalingStore().setValue(keys.answer(sessionCode), sdp);
  return json({ ok: true }, { headers });
}

export async function GET(req: Request) {
  const origin = req.headers.get("origin");
  const headers = corsHeaders(origin);

  const rl = rateLimit(`answer:get:${clientId(req.headers)}`, READ_LIMIT, 60);
  if (!rl.ok) {
    return json({ error: "rate_limited" }, { status: 429, headers, retryAfter: rl.retryAfterSeconds });
  }

  const code = new URL(req.url).searchParams.get("code");
  if (!isValidSessionCode(code)) {
    return json({ error: "invalid_input" }, { status: 400, headers });
  }

  const sdp = await getSignalingStore().getValue(keys.answer(code));
  return json({ sdp }, { headers });
}
