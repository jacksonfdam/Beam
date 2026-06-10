"use client";

import { useEffect, useState } from "react";
import type { NavAction, TimerAction } from "@/lib/protocol";
import type { Presentation } from "./useBeamRemote";
import { DrawingSurface } from "./DrawingSurface";

interface Props {
  presentation: Presentation;
  sessionName: string;
  onSelectDeck: (deckId: string) => void;
  onNav: (action: NavAction) => void;
  onGoTo: (index: number) => void;
  onTimer: (action: TimerAction) => void;
  onBeginStroke: (x: number, y: number) => number;
  onExtendStroke: (id: number, x: number, y: number) => void;
  onEndStroke: (id: number) => void;
  onClearInk: () => void;
  onDisconnect: () => void;
}

const INK_PREVIEW_CSS = "#ef4444";

export function RemoteControls(props: Props) {
  const { presentation: p } = props;

  if (!p.selectedDeckId) {
    return <DeckPicker decks={p.decks} onSelect={props.onSelectDeck} onDisconnect={props.onDisconnect} sessionName={props.sessionName} />;
  }

  return (
    <ConnectedDeck {...props} />
  );
}

function DeckPicker({
  decks,
  onSelect,
  onDisconnect,
  sessionName,
}: {
  decks: Props["presentation"]["decks"];
  onSelect: (id: string) => void;
  onDisconnect: () => void;
  sessionName: string;
}) {
  return (
    <section className="mx-auto w-full max-w-md" aria-labelledby="deck-heading">
      <ConnectedHeader sessionName={sessionName} onDisconnect={onDisconnect} />
      <h2 id="deck-heading" className="mt-6 text-xl font-semibold">
        Choose a deck
      </h2>
      {decks.length === 0 ? (
        <p className="mt-3 text-sm text-white/55">
          No decks available yet. Open one on the host.
        </p>
      ) : (
        <ul className="mt-4 space-y-3">
          {decks.map((d) => (
            <li key={d.id}>
              <button
                type="button"
                onClick={() => onSelect(d.id)}
                className="flex w-full items-center justify-between rounded-xl border border-ink-line bg-ink-soft/40 px-4 py-3 text-left transition hover:border-beam/60"
              >
                <span>
                  <span className="block font-medium text-white">{d.title}</span>
                  <span className="block text-xs text-white/50">
                    {d.slideCount} slides{d.hasNotes ? " · notes" : ""}
                  </span>
                </span>
                <span aria-hidden="true" className="text-beam-glow">→</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function ConnectedDeck(props: Props) {
  const { presentation: p } = props;
  const [showDrawing, setShowDrawing] = useState(false);

  // Arrow keys drive navigation for laptop remotes.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA") return;
      if (e.key === "ArrowRight" || e.key === "PageDown") props.onNav("NEXT");
      else if (e.key === "ArrowLeft" || e.key === "PageUp") props.onNav("PREV");
      else if (e.key === "Home") props.onNav("FIRST");
      else if (e.key === "End") props.onNav("LAST");
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [props]);

  return (
    <section className="mx-auto w-full max-w-md space-y-6">
      <ConnectedHeader sessionName={props.sessionName} onDisconnect={props.onDisconnect} />

      <SlideIndicator index={p.index} total={p.total} />

      <div className="grid grid-cols-3 gap-3">
        <NavButton label="Previous" onClick={() => props.onNav("PREV")}>
          ‹ Prev
        </NavButton>
        <GoToControl total={p.total} onGoTo={props.onGoTo} />
        <NavButton label="Next" onClick={() => props.onNav("NEXT")}>
          Next ›
        </NavButton>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <NavButton label="First slide" onClick={() => props.onNav("FIRST")}>
          ⤒ First
        </NavButton>
        <NavButton label="Last slide" onClick={() => props.onNav("LAST")}>
          Last ⤓
        </NavButton>
      </div>

      <NotesPane notes={p.notes} hasNotes={p.hasNotes} />

      <TimerPanel timer={p.timer} onTimer={props.onTimer} />

      <div>
        <button
          type="button"
          onClick={() => setShowDrawing((v) => !v)}
          aria-expanded={showDrawing}
          className="text-sm font-medium text-beam-glow underline underline-offset-4 hover:text-beam-bright"
        >
          {showDrawing ? "Hide drawing" : "Draw on the slide"}
        </button>
        {showDrawing && (
          <div className="mt-3">
            <DrawingSurface
              previewColor={INK_PREVIEW_CSS}
              onBegin={(x, y) => props.onBeginStroke(x, y)}
              onExtend={props.onExtendStroke}
              onEnd={props.onEndStroke}
              onClear={props.onClearInk}
            />
          </div>
        )}
      </div>
    </section>
  );
}

function ConnectedHeader({ sessionName, onDisconnect }: { sessionName: string; onDisconnect: () => void }) {
  return (
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-2 text-sm text-white/70">
        <span className="inline-block h-2 w-2 rounded-full bg-green-400" aria-hidden="true" />
        Connected{sessionName ? ` · ${sessionName}` : ""}
      </div>
      <button
        type="button"
        onClick={onDisconnect}
        className="rounded-full border border-ink-line px-3 py-1.5 text-sm text-white/70 transition hover:text-white"
      >
        Disconnect
      </button>
    </div>
  );
}

function SlideIndicator({ index, total }: { index: number; total: number }) {
  return (
    <div className="rounded-2xl border border-ink-line bg-ink-soft/40 px-6 py-5 text-center">
      <div className="font-mono text-4xl font-semibold tracking-tight">
        {total > 0 ? index + 1 : 0}
        <span className="text-white/40"> / {total}</span>
      </div>
      <p className="mt-1 text-xs uppercase tracking-widest text-white/45">Slide</p>
    </div>
  );
}

function NavButton({
  children,
  label,
  onClick,
}: {
  children: React.ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className="rounded-xl border border-ink-line bg-ink-soft/40 px-4 py-4 text-base font-semibold transition hover:border-beam/60 active:scale-[0.98]"
    >
      {children}
    </button>
  );
}

function GoToControl({ total, onGoTo }: { total: number; onGoTo: (index: number) => void }) {
  const [value, setValue] = useState("");
  return (
    <form
      className="flex"
      onSubmit={(e) => {
        e.preventDefault();
        const n = Number.parseInt(value, 10);
        if (!Number.isNaN(n) && n >= 1 && n <= total) onGoTo(n - 1);
        setValue("");
      }}
    >
      <input
        inputMode="numeric"
        value={value}
        onChange={(e) => setValue(e.target.value.replace(/\D/g, ""))}
        aria-label={`Go to slide (1 to ${total})`}
        placeholder="Go to"
        className="w-full rounded-xl border border-ink-line bg-ink px-3 text-center font-mono outline-none focus:border-beam"
      />
    </form>
  );
}

function NotesPane({ notes, hasNotes }: { notes: string | null; hasNotes: boolean }) {
  return (
    <section aria-labelledby="notes-heading" className="rounded-2xl border border-ink-line bg-ink p-4">
      <h2 id="notes-heading" className="text-xs font-semibold uppercase tracking-widest text-white/45">
        Speaker notes
      </h2>
      <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-white/80">
        {notes
          ? notes
          : hasNotes
            ? "No notes for this slide."
            : "This deck has no notes sidecar."}
      </p>
    </section>
  );
}

function TimerPanel({
  timer,
  onTimer,
}: {
  timer: Presentation["timer"];
  onTimer: (action: TimerAction) => void;
}) {
  const [, force] = useState(0);
  useEffect(() => {
    if (!timer.running) return;
    const id = setInterval(() => force((n) => n + 1), 250);
    return () => clearInterval(id);
  }, [timer.running, timer.anchorAt]);

  const elapsed = timer.running
    ? timer.elapsedMs + (Date.now() - timer.anchorAt)
    : timer.elapsedMs;

  return (
    <section aria-labelledby="timer-heading" className="rounded-2xl border border-ink-line bg-ink-soft/40 p-4">
      <div className="flex items-center justify-between">
        <h2 id="timer-heading" className="text-xs font-semibold uppercase tracking-widest text-white/45">
          Timer
        </h2>
        <span className="font-mono text-2xl tabular-nums" aria-live="off">
          {formatElapsed(elapsed)}
        </span>
      </div>
      <div className="mt-3 grid grid-cols-3 gap-2">
        <TimerButton onClick={() => onTimer(timer.running ? "PAUSE" : "START")}>
          {timer.running ? "Pause" : "Start"}
        </TimerButton>
        <TimerButton onClick={() => onTimer("PAUSE")}>Pause</TimerButton>
        <TimerButton onClick={() => onTimer("RESET")}>Reset</TimerButton>
      </div>
    </section>
  );
}

function TimerButton({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded-lg border border-ink-line bg-ink px-3 py-2 text-sm font-medium transition hover:border-beam/60"
    >
      {children}
    </button>
  );
}

function formatElapsed(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const mm = String(m).padStart(2, "0");
  const ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}
