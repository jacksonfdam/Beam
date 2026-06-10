import { describe, expect, it } from "vitest";
import { getSignalingStore, keys, oppositeRole } from "./signaling-store";

describe("oppositeRole", () => {
  it("flips the role", () => {
    expect(oppositeRole("offerer")).toBe("answerer");
    expect(oppositeRole("answerer")).toBe("offerer");
  });
});

describe("keys", () => {
  it("namespaces per session code and role", () => {
    expect(keys.offer("AB12")).toBe("beam:offer:AB12");
    expect(keys.answer("AB12")).toBe("beam:answer:AB12");
    expect(keys.ice("AB12", "offerer")).toBe("beam:ice:AB12:offerer");
  });
});

describe("in-memory signaling store", () => {
  const store = getSignalingStore();

  it("stores and reads a value", async () => {
    await store.setValue("k1", "hello");
    expect(await store.getValue("k1")).toBe("hello");
  });

  it("returns null for a missing value", async () => {
    expect(await store.getValue("missing")).toBeNull();
  });

  it("appends to and drains a list once", async () => {
    await store.pushToList("list1", "a");
    await store.pushToList("list1", "b");
    expect(await store.drainList("list1")).toEqual(["a", "b"]);
    // Draining clears, so a second poll sees nothing.
    expect(await store.drainList("list1")).toEqual([]);
  });
});
