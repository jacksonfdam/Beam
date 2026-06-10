"use client";

import { useState } from "react";
import type { RemoteState } from "@/lib/beam-remote";

interface Props {
  state: RemoteState;
  onConnect: (host: string, pin: string, name: string) => void;
  onCancel: () => void;
}

/**
 * The browser connects straight to the host's LAN WebSocket. Enter the host IP
 * and PIN shown on the presenter screen (a wrong PIN is rejected).
 */
export function PairingForm({ state, onConnect, onCancel }: Props) {
  const [host, setHost] = useState("");
  const [pin, setPin] = useState("");
  const [name, setName] = useState("");
  const busy = state.phase === "connecting" || state.phase === "handshaking";
  const valid = host.trim().length > 0;

  return (
    <form
      className="mx-auto w-full max-w-sm rounded-2xl border border-ink-line bg-ink-soft/40 p-6"
      onSubmit={(e) => {
        e.preventDefault();
        if (valid && !busy) onConnect(host, pin, name);
      }}
    >
      <h1 className="text-2xl font-semibold tracking-tight">Connect to a host</h1>
      <p className="mt-2 text-sm text-white/60">
        Enter the host IP and PIN shown on the presenter screen.
      </p>

      <div className="mt-6 space-y-4">
        <div>
          <label htmlFor="host" className="block text-sm font-medium text-white/80">
            Host IP
          </label>
          <input
            id="host"
            inputMode="text"
            autoComplete="off"
            autoFocus
            value={host}
            onChange={(e) => setHost(e.target.value)}
            aria-describedby="host-hint"
            className="mt-1 w-full rounded-xl border border-ink-line bg-ink px-4 py-3 font-mono text-white outline-none focus:border-beam"
            placeholder="192.168.0.8"
          />
          <p id="host-hint" className="mt-1 text-xs text-white/45">
            Port is optional — defaults to 53317.
          </p>
        </div>

        <div>
          <label htmlFor="pin" className="block text-sm font-medium text-white/80">
            PIN
          </label>
          <input
            id="pin"
            inputMode="numeric"
            value={pin}
            onChange={(e) => setPin(e.target.value)}
            className="mt-1 w-full rounded-xl border border-ink-line bg-ink px-4 py-3 font-mono text-lg tracking-[0.3em] text-white outline-none focus:border-beam"
            placeholder="4821"
          />
        </div>

        <div>
          <label htmlFor="name" className="block text-sm font-medium text-white/80">
            Your name <span className="text-white/40">(optional)</span>
          </label>
          <input
            id="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1 w-full rounded-xl border border-ink-line bg-ink px-4 py-3 text-white outline-none focus:border-beam"
            placeholder="Browser"
            maxLength={40}
          />
        </div>
      </div>

      {state.error && (
        <p role="alert" className="mt-4 rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-300">
          {state.error}
        </p>
      )}

      <div className="mt-6 flex gap-3">
        <button
          type="submit"
          disabled={!valid || busy}
          className="flex-1 rounded-full bg-beam px-5 py-3 font-semibold text-ink transition hover:bg-beam-bright disabled:cursor-not-allowed disabled:opacity-40"
        >
          {busy ? "Connecting…" : "Connect"}
        </button>
        {busy && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-full border border-ink-line px-5 py-3 font-medium text-white/80 transition hover:text-white"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
