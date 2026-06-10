/*
 * Beam wire protocol — TypeScript mirror.
 *
 * CANONICAL SOURCE: ../files/BeamProtocol.kt (Kotlin, :shared module).
 * This file MUST change together with it. The names, the "type" discriminator
 * values, and the field names below mirror the Kotlin @SerialName tags and
 * data-class properties exactly, so a browser peer speaks the same JSON as the
 * native Android / iOS / Desktop clients.
 *
 * Transport: JSON over a WebRTC DataChannel (browser remote) or WebSocket
 * (native). Source of truth is the HOST: it owns the current slide, timer, and
 * speaker notes; the remote sends commands and renders what the host pushes.
 *
 * kotlinx.serialization is configured with classDiscriminator = "type" and no
 * field-naming strategy, so JSON keys are camelCase and the discriminator value
 * is the snake_case @SerialName.
 */

export const PROTOCOL_VERSION = 1;
export const DEFAULT_PORT = 53317;

// ---------------------------------------------------------------------------
// Geometry
// ---------------------------------------------------------------------------

/**
 * Ink coordinates are NORMALIZED (0..1) relative to the slide CONTENT rect, not
 * the screen, so a finger on the remote lands on the same spot on the projector
 * regardless of resolution or letterboxing. `pressure` defaults to 1.
 */
export interface NormPoint {
  x: number;
  y: number;
  pressure: number;
}

export function normPoint(x: number, y: number, pressure = 1): NormPoint {
  return { x, y, pressure };
}

// ---------------------------------------------------------------------------
// Decks
// ---------------------------------------------------------------------------

export interface DeckInfo {
  id: string;
  title: string;
  slideCount: number;
  hasNotes: boolean;
}

// ---------------------------------------------------------------------------
// Enums (serialized by name, matching the Kotlin enum constants)
// ---------------------------------------------------------------------------

export type NavAction = "NEXT" | "PREV" | "FIRST" | "LAST";
export type TimerAction = "START" | "PAUSE" | "RESET";
export type PresentMode = "SLIDES" | "SCREEN";

// ---------------------------------------------------------------------------
// Client -> Host   (the remote drives)
// ---------------------------------------------------------------------------

export interface Hello {
  type: "hello";
  clientName: string;
  protocolVersion: number;
  /** Short code shown on the presenter screen. */
  pin?: string | null;
}

export interface SelectDeck {
  type: "select_deck";
  deckId: string;
}

export interface Nav {
  type: "nav";
  action: NavAction;
}

export interface GoTo {
  type: "goto";
  index: number;
}

export interface StrokeStart {
  type: "stroke_start";
  strokeId: number;
  /** ARGB packed colour, e.g. 0xFFEF4444. */
  colorArgb: number;
  widthDp: number;
  point: NormPoint;
}

export interface StrokePoint {
  type: "stroke_point";
  strokeId: number;
  point: NormPoint;
}

export interface StrokeEnd {
  type: "stroke_end";
  strokeId: number;
}

export interface ClearInk {
  type: "clear_ink";
}

export interface TimerCmd {
  type: "timer";
  action: TimerAction;
}

export interface Ping {
  type: "ping";
}

export interface SetMode {
  type: "set_mode";
  mode: PresentMode;
}

export interface SetInteracting {
  type: "set_interacting";
  interacting: boolean;
}

export interface Spotlight {
  type: "spotlight";
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export type ClientMessage =
  | Hello
  | SelectDeck
  | Nav
  | GoTo
  | StrokeStart
  | StrokePoint
  | StrokeEnd
  | ClearInk
  | TimerCmd
  | Ping
  | SetMode
  | SetInteracting
  | Spotlight;

// ---------------------------------------------------------------------------
// Host -> Client
// ---------------------------------------------------------------------------

export interface HelloAck {
  type: "hello_ack";
  sessionName: string;
  hostVersion: string;
  decks: DeckInfo[];
  screenAspect: number;
}

export interface HelloReject {
  type: "hello_reject";
  /** Bad PIN or protocol-version mismatch. */
  reason: string;
}

export interface DeckSelected {
  type: "deck_selected";
  deckId: string;
  slideCount: number;
  hasNotes: boolean;
}

export interface SlideChanged {
  type: "slide_changed";
  index: number;
  total: number;
  /** Speaker notes for this slide, from the host-side sidecar. */
  notes?: string | null;
}

export interface TimerState {
  type: "timer_state";
  elapsedMs: number;
  running: boolean;
}

export interface Pong {
  type: "pong";
}

export interface HostError {
  type: "error";
  message: string;
}

export interface SlideImage {
  type: "slide_image";
  index: number;
  /** PNG of the current slide, Base64-encoded, for the remote preview. */
  pngBase64: string;
}

export interface ModeChanged {
  type: "mode_changed";
  mode: PresentMode;
}

export interface ScreenImage {
  type: "screen_image";
  /** JPEG of the host's live screen, Base64-encoded. */
  jpegBase64: string;
}

export type HostMessage =
  | HelloAck
  | HelloReject
  | DeckSelected
  | SlideChanged
  | TimerState
  | Pong
  | HostError
  | SlideImage
  | ModeChanged
  | ScreenImage;

// ---------------------------------------------------------------------------
// Builders (Client -> Host) — convenience constructors used by the remote
// ---------------------------------------------------------------------------

export const ClientMsg = {
  hello(clientName: string, pin?: string | null): Hello {
    return { type: "hello", clientName, protocolVersion: PROTOCOL_VERSION, pin: pin ?? null };
  },
  selectDeck(deckId: string): SelectDeck {
    return { type: "select_deck", deckId };
  },
  nav(action: NavAction): Nav {
    return { type: "nav", action };
  },
  goto(index: number): GoTo {
    return { type: "goto", index };
  },
  strokeStart(
    strokeId: number,
    colorArgb: number,
    widthDp: number,
    point: NormPoint,
  ): StrokeStart {
    return { type: "stroke_start", strokeId, colorArgb, widthDp, point };
  },
  strokePoint(strokeId: number, point: NormPoint): StrokePoint {
    return { type: "stroke_point", strokeId, point };
  },
  strokeEnd(strokeId: number): StrokeEnd {
    return { type: "stroke_end", strokeId };
  },
  clearInk(): ClearInk {
    return { type: "clear_ink" };
  },
  timer(action: TimerAction): TimerCmd {
    return { type: "timer", action };
  },
  ping(): Ping {
    return { type: "ping" };
  },
  setMode(mode: PresentMode): SetMode {
    return { type: "set_mode", mode };
  },
  setInteracting(interacting: boolean): SetInteracting {
    return { type: "set_interacting", interacting };
  },
  spotlight(left: number, top: number, right: number, bottom: number): Spotlight {
    return { type: "spotlight", left, top, right, bottom };
  },
};

// ---------------------------------------------------------------------------
// Serialization
// ---------------------------------------------------------------------------

const CLIENT_TYPES: ReadonlySet<string> = new Set([
  "hello",
  "select_deck",
  "nav",
  "goto",
  "stroke_start",
  "stroke_point",
  "stroke_end",
  "clear_ink",
  "timer",
  "ping",
  "set_mode",
  "set_interacting",
  "spotlight",
]);

const HOST_TYPES: ReadonlySet<string> = new Set([
  "hello_ack",
  "hello_reject",
  "deck_selected",
  "slide_changed",
  "timer_state",
  "pong",
  "error",
  "slide_image",
  "mode_changed",
  "screen_image",
]);

export function encode(msg: ClientMessage | HostMessage): string {
  return JSON.stringify(msg);
}

/** Parse a host->client message. Throws on malformed JSON or unknown type. */
export function decodeHostMessage(raw: string): HostMessage {
  const obj = JSON.parse(raw) as unknown;
  if (!isRecord(obj) || typeof obj.type !== "string" || !HOST_TYPES.has(obj.type)) {
    throw new Error(`Unknown host message: ${preview(raw)}`);
  }
  return obj as unknown as HostMessage;
}

/** Parse a client->host message. Useful for tests and a host-side mock. */
export function decodeClientMessage(raw: string): ClientMessage {
  const obj = JSON.parse(raw) as unknown;
  if (!isRecord(obj) || typeof obj.type !== "string" || !CLIENT_TYPES.has(obj.type)) {
    throw new Error(`Unknown client message: ${preview(raw)}`);
  }
  return obj as unknown as ClientMessage;
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null;
}

function preview(raw: string): string {
  return raw.length > 80 ? `${raw.slice(0, 80)}…` : raw;
}

// ---------------------------------------------------------------------------
// Connection endpoint (mirror of HostEndpoint.toUri / parse for the QR path)
// ---------------------------------------------------------------------------

export interface HostEndpoint {
  host: string;
  port: number;
  pin?: string | null;
}

export function endpointToUri(e: HostEndpoint): string {
  let uri = `beam://connect?host=${e.host}&port=${e.port}`;
  if (e.pin != null && e.pin !== "") uri += `&pin=${e.pin}`;
  return uri;
}

export function parseEndpoint(uri: string): HostEndpoint | null {
  try {
    const query = uri.includes("?") ? uri.slice(uri.indexOf("?") + 1) : "";
    const params = new Map<string, string>();
    for (const pair of query.split("&")) {
      const [k, v] = pair.split("=");
      if (k && v !== undefined) params.set(k, v);
    }
    const host = params.get("host");
    if (!host) return null;
    const portRaw = params.get("port");
    const port = portRaw ? Number.parseInt(portRaw, 10) : DEFAULT_PORT;
    if (Number.isNaN(port)) return null;
    return { host, port, pin: params.get("pin") ?? null };
  } catch {
    return null;
  }
}
