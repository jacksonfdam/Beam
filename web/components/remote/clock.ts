"use client";

import { useSyncExternalStore } from "react";

/*
 * A shared ticking clock exposed through useSyncExternalStore. The timer view
 * reads this instead of calling Date.now() during render, which keeps the
 * component pure and avoids setState-in-effect. The snapshot only changes on
 * each tick, so it is stable between ticks.
 */

let now = Date.now();
const listeners = new Set<() => void>();
let interval: ReturnType<typeof setInterval> | null = null;

function subscribe(onChange: () => void): () => void {
  listeners.add(onChange);
  if (interval === null) {
    interval = setInterval(() => {
      now = Date.now();
      listeners.forEach((l) => l());
    }, 250);
  }
  return () => {
    listeners.delete(onChange);
    if (listeners.size === 0 && interval !== null) {
      clearInterval(interval);
      interval = null;
    }
  };
}

/** Current wall-clock time in ms, updated roughly every 250 ms while observed. */
export function useWallClock(): number {
  return useSyncExternalStore(
    subscribe,
    () => now,
    () => 0,
  );
}
