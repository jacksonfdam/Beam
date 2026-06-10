import { describe, expect, it } from "vitest";
import {
  MAX_CANDIDATE_CHARS,
  MAX_SDP_CHARS,
  isValidCandidate,
  isValidRole,
  isValidSdp,
  isValidSessionCode,
} from "./signaling-http";
import { rateLimit } from "./rate-limit";

describe("session code validation", () => {
  it("accepts 4–16 alphanumerics", () => {
    expect(isValidSessionCode("4821")).toBe(true);
    expect(isValidSessionCode("AbC123xyz")).toBe(true);
  });
  it("rejects bad shapes", () => {
    expect(isValidSessionCode("abc")).toBe(false); // too short
    expect(isValidSessionCode("a".repeat(17))).toBe(false); // too long
    expect(isValidSessionCode("has space")).toBe(false);
    expect(isValidSessionCode(42)).toBe(false);
    expect(isValidSessionCode(null)).toBe(false);
  });
});

describe("sdp validation", () => {
  it("accepts a reasonable string", () => {
    expect(isValidSdp("v=0\r\n...")).toBe(true);
  });
  it("rejects empty and oversized", () => {
    expect(isValidSdp("")).toBe(false);
    expect(isValidSdp("x".repeat(MAX_SDP_CHARS + 1))).toBe(false);
    expect(isValidSdp(123)).toBe(false);
  });
});

describe("role validation", () => {
  it("accepts only the two roles", () => {
    expect(isValidRole("offerer")).toBe(true);
    expect(isValidRole("answerer")).toBe(true);
    expect(isValidRole("admin")).toBe(false);
  });
});

describe("candidate validation", () => {
  it("accepts strings and objects within the size limit", () => {
    expect(isValidCandidate("candidate:...")).toBe(true);
    expect(isValidCandidate({ candidate: "x", sdpMid: "0" })).toBe(true);
  });
  it("rejects oversized and empty", () => {
    expect(isValidCandidate("x".repeat(MAX_CANDIDATE_CHARS + 1))).toBe(false);
    expect(isValidCandidate(null)).toBe(false);
    expect(isValidCandidate(5)).toBe(false);
  });
});

describe("rate limiter", () => {
  it("allows up to the limit then blocks within the window", () => {
    const key = `test-${Math.random()}`;
    expect(rateLimit(key, 2, 60).ok).toBe(true);
    expect(rateLimit(key, 2, 60).ok).toBe(true);
    const blocked = rateLimit(key, 2, 60);
    expect(blocked.ok).toBe(false);
    expect(blocked.retryAfterSeconds).toBeGreaterThan(0);
  });
});
