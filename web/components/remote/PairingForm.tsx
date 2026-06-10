"use client";

import { useState } from "react";
import type { RemoteState } from "@/lib/beam-remote";

const CODE_RE = /^[A-Za-z0-9]{4,16}$/;

interface Props {
  state: RemoteState;
  onConnect: (sessionCode: string, name: string) => void;
  onCancel: () => void;
}

/**
 * The session code shown on the presenter screen is used both to pair through
 * signaling AND as the PIN in the Beam handshake — a wrong code is rejected.
 */
export function PairingForm({ state, onConnect, onCancel }: Props) {
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const busy = state.phase === "connecting" || state.phase === "handshaking";
  const valid = CODE_RE.test(code.trim());

  return (
    <form
      className="mx-auto w-full max-w-sm rounded-2xl border border-ink-line bg-ink-soft/40 p-6"
      onSubmit={(e) => {
        e.preventDefault();
        if (valid && !busy) onConnect(code.trim(), name);
      }}
    >
      <h1 className="text-2xl font-semibold tracking-tight">Connect to a host</h1>
      <p className="mt-2 text-sm text-white/60">
        Enter the session code shown on the presenter screen.
      </p>

      <div className="mt-6 space-y-4">
        <div>
          <label htmlFor="code" className="block text-sm font-medium text-white/80">
            Session code
          </label>
          <input
            id="code"
            inputMode="text"
            autoComplete="off"
            autoCapitalize="characters"
            // eslint-disable-next-line jsx-a11y/no-autofocus
            autoFocus
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            aria-describedby="code-hint"
            className="mt-1 w-full rounded-xl border border-ink-line bg-ink px-4 py-3 font-mono text-lg tracking-[0.3em] text-white outline-none focus:border-beam"
            placeholder="4821"
          />
          <p id="code-hint" className="mt-1 text-xs text-white/45">
            4–16 letters or digits.
          </p>
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
