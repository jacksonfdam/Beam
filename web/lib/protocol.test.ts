import { describe, expect, it } from "vitest";
import {
  ClientMsg,
  DEFAULT_PORT,
  decodeClientMessage,
  decodeHostMessage,
  encode,
  endpointToUri,
  normPoint,
  parseEndpoint,
  type ClientMessage,
  type HostMessage,
} from "./protocol";

// Every client->host message, exercised through encode + decode.
const clientMessages: ClientMessage[] = [
  ClientMsg.hello("Browser", "4821"),
  ClientMsg.hello("Browser"),
  ClientMsg.selectDeck("deck-1"),
  ClientMsg.nav("NEXT"),
  ClientMsg.nav("PREV"),
  ClientMsg.nav("FIRST"),
  ClientMsg.nav("LAST"),
  ClientMsg.goto(7),
  ClientMsg.strokeStart(1, 0xffef4444, 4, normPoint(0.1, 0.2, 0.5)),
  ClientMsg.strokePoint(1, normPoint(0.3, 0.4)),
  ClientMsg.strokeEnd(1),
  ClientMsg.clearInk(),
  ClientMsg.timer("START"),
  ClientMsg.timer("PAUSE"),
  ClientMsg.timer("RESET"),
  ClientMsg.ping(),
];

// Every host->client message.
const hostMessages: HostMessage[] = [
  {
    type: "hello_ack",
    sessionName: "Jackson's Mac",
    hostVersion: "1.0.0",
    decks: [{ id: "d1", title: "Talk", slideCount: 12, hasNotes: true }],
  },
  { type: "hello_reject", reason: "bad pin" },
  { type: "deck_selected", deckId: "d1", slideCount: 12, hasNotes: false },
  { type: "slide_changed", index: 3, total: 12, notes: "Smile." },
  { type: "slide_changed", index: 0, total: 12 },
  { type: "timer_state", elapsedMs: 4200, running: true },
  { type: "pong" },
  { type: "error", message: "boom" },
];

describe("protocol round-trip", () => {
  it("re-decodes every client message identically", () => {
    for (const msg of clientMessages) {
      const decoded = decodeClientMessage(encode(msg));
      expect(decoded).toEqual(msg);
      expect(decoded.type).toBe(msg.type);
    }
  });

  it("re-decodes every host message identically", () => {
    for (const msg of hostMessages) {
      const decoded = decodeHostMessage(encode(msg));
      expect(decoded).toEqual(msg);
      expect(decoded.type).toBe(msg.type);
    }
  });

  it("rejects an unknown discriminator", () => {
    expect(() => decodeHostMessage(JSON.stringify({ type: "nope" }))).toThrow();
    expect(() => decodeClientMessage(JSON.stringify({ type: "nope" }))).toThrow();
    expect(() => decodeHostMessage("not json")).toThrow();
  });

  it("keeps the snake_case discriminator on the wire", () => {
    expect(JSON.parse(encode(ClientMsg.selectDeck("x"))).type).toBe("select_deck");
    expect(JSON.parse(encode(ClientMsg.clearInk())).type).toBe("clear_ink");
  });
});

describe("HostEndpoint parse <-> toUri", () => {
  it("round-trips with a pin", () => {
    const e = { host: "192.168.1.42", port: 53317, pin: "4821" };
    expect(parseEndpoint(endpointToUri(e))).toEqual(e);
  });

  it("round-trips without a pin and defaults the port", () => {
    const uri = endpointToUri({ host: "10.0.0.5", port: DEFAULT_PORT });
    const parsed = parseEndpoint(uri);
    expect(parsed).toEqual({ host: "10.0.0.5", port: DEFAULT_PORT, pin: null });
  });

  it("returns null for a malformed uri", () => {
    expect(parseEndpoint("beam://connect?port=1")).toBeNull();
    expect(parseEndpoint("garbage")).toBeNull();
  });
});

describe("NormPoint", () => {
  it("defaults pressure to 1", () => {
    expect(normPoint(0.5, 0.5)).toEqual({ x: 0.5, y: 0.5, pressure: 1 });
  });
});
