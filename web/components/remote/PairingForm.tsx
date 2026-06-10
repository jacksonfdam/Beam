"use client";

import { useState } from "react";
import type { RemoteState } from "@/lib/beam-remote";
import { LanguageSwitcher, useI18n } from "@/lib/i18n";

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
  const { t } = useI18n();
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
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">{t.pairing.title}</h1>
        <LanguageSwitcher />
      </div>
      <p className="mt-2 text-sm text-white/60">{t.pairing.subtitle}</p>

      <div className="mt-6 space-y-4">
        <div>
          <label htmlFor="host" className="block text-sm font-medium text-white/80">
            {t.pairing.hostIp}
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
            {t.pairing.portHint}
          </p>
        </div>

        <div>
          <label htmlFor="pin" className="block text-sm font-medium text-white/80">
            {t.pairing.pin}
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
            {t.pairing.yourName} <span className="text-white/40">{t.pairing.optional}</span>
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
          {busy ? t.pairing.connecting : t.pairing.connect}
        </button>
        {busy && (
          <button
            type="button"
            onClick={onCancel}
            className="rounded-full border border-ink-line px-5 py-3 font-medium text-white/80 transition hover:text-white"
          >
            {t.pairing.cancel}
          </button>
        )}
      </div>
    </form>
  );
}
