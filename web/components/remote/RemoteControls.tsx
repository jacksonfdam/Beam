"use client";

import { useEffect, useState } from "react";
import type { NavAction, PresentMode, TimerAction } from "@/lib/protocol";
import type { Presentation } from "./useBeamRemote";
import { DrawingSurface, type DrawTool } from "./DrawingSurface";
import { useWallClock } from "./clock";
import { useI18n } from "@/lib/i18n";

interface Props {
  presentation: Presentation;
  sessionName: string;
  onSelectDeck: (deckId: string) => void;
  onNav: (action: NavAction) => void;
  onGoTo: (index: number) => void;
  onTimer: (action: TimerAction) => void;
  onSetMode: (mode: PresentMode) => void;
  onSetInteracting: (interacting: boolean) => void;
  onSpotlight: (left: number, top: number, right: number, bottom: number) => void;
  onBeginStroke: (x: number, y: number, colorArgb: number, widthDp: number) => number;
  onExtendStroke: (id: number, x: number, y: number) => void;
  onEndStroke: (id: number) => void;
  onClearInk: () => void;
  onDisconnect: () => void;
}

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
  const { t } = useI18n();
  return (
    <section className="mx-auto w-full max-w-md" aria-labelledby="deck-heading">
      <ConnectedHeader sessionName={sessionName} onDisconnect={onDisconnect} />
      <h2 id="deck-heading" className="mt-6 text-xl font-semibold">
        {t.controls.chooseDeck}
      </h2>
      {decks.length === 0 ? (
        <p className="mt-3 text-sm text-white/55">{t.controls.noDecks}</p>
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
                    {d.slideCount} {t.controls.slides}{d.hasNotes ? ` · ${t.controls.notes}` : ""}
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
  const { t } = useI18n();
  const [showDrawing, setShowDrawing] = useState(false);
  const [tool, setTool] = useState<DrawTool>("PEN");
  const slidesMode = p.presentMode === "SLIDES";
  const previewImage = slidesMode ? p.slideImage : p.screenImage;
  const effectiveTool: DrawTool = slidesMode && tool === "SPOTLIGHT" ? "PEN" : tool;

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

      <Segmented
        options={[
          { label: t.controls.slidesMode, selected: slidesMode, onClick: () => props.onSetMode("SLIDES") },
          { label: t.controls.screenMode, selected: !slidesMode, onClick: () => props.onSetMode("SCREEN") },
        ]}
      />

      {!slidesMode && (
        <Segmented
          options={[
            { label: t.controls.annotate, selected: !p.interacting, onClick: () => props.onSetInteracting(false) },
            { label: t.controls.interact, selected: p.interacting, onClick: () => props.onSetInteracting(true) },
          ]}
        />
      )}

      {previewImage && !showDrawing && (
        <img
          src={previewImage}
          alt={slidesMode ? t.controls.currentSlide : t.controls.liveScreen}
          className="w-full rounded-2xl border border-ink-line bg-black"
        />
      )}

      <SlideIndicator index={p.index} total={p.total} />

      <div className="grid grid-cols-3 gap-3">
        <NavButton label={t.controls.previousAria} onClick={() => props.onNav("PREV")}>
          {t.controls.prev}
        </NavButton>
        <GoToControl total={p.total} onGoTo={props.onGoTo} />
        <NavButton label={t.controls.nextAria} onClick={() => props.onNav("NEXT")}>
          {t.controls.next}
        </NavButton>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <NavButton label={t.controls.firstAria} onClick={() => props.onNav("FIRST")}>
          {t.controls.first}
        </NavButton>
        <NavButton label={t.controls.lastAria} onClick={() => props.onNav("LAST")}>
          {t.controls.last}
        </NavButton>
      </div>

      <NotesPane notes={p.notes} hasNotes={p.hasNotes} />

      <TimerPanel timer={p.timer} onTimer={props.onTimer} />

      <div className="space-y-3">
        <button
          type="button"
          onClick={() => {
            if (showDrawing) props.onClearInk();
            setShowDrawing((v) => !v);
          }}
          aria-expanded={showDrawing}
          className="text-sm font-medium text-beam-glow underline underline-offset-4 hover:text-beam-bright"
        >
          {showDrawing ? t.controls.hideDrawing : slidesMode ? t.controls.drawOnSlide : t.controls.drawSpotlight}
        </button>
        {showDrawing && (
          <>
            <Segmented
              options={[
                { label: t.controls.pen, selected: effectiveTool === "PEN", onClick: () => setTool("PEN") },
                { label: t.controls.marker, selected: effectiveTool === "MARKER", onClick: () => setTool("MARKER") },
                ...(slidesMode
                  ? []
                  : [{ label: t.controls.spotlight, selected: effectiveTool === "SPOTLIGHT", onClick: () => setTool("SPOTLIGHT") }]),
              ]}
            />
            <DrawingSurface
              image={previewImage}
              aspect={p.screenAspect}
              tool={effectiveTool}
              onBegin={props.onBeginStroke}
              onExtend={props.onExtendStroke}
              onEnd={props.onEndStroke}
              onClear={props.onClearInk}
              onSpotlight={props.onSpotlight}
            />
          </>
        )}
      </div>
    </section>
  );
}

function Segmented({
  options,
}: {
  options: { label: string; selected: boolean; onClick: () => void }[];
}) {
  return (
    <div className="flex gap-2">
      {options.map((o) => (
        <button
          key={o.label}
          type="button"
          onClick={o.onClick}
          className={`flex-1 rounded-full px-4 py-2 text-sm font-medium transition ${
            o.selected
              ? "bg-beam text-ink"
              : "border border-ink-line text-white/80 hover:border-beam/60"
          }`}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}

function ConnectedHeader({ sessionName, onDisconnect }: { sessionName: string; onDisconnect: () => void }) {
  const { t } = useI18n();
  return (
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-2 text-sm text-white/70">
        <span className="inline-block h-2 w-2 rounded-full bg-green-400" aria-hidden="true" />
        {t.controls.connected}{sessionName ? ` · ${sessionName}` : ""}
      </div>
      <button
        type="button"
        onClick={onDisconnect}
        className="rounded-full border border-ink-line px-3 py-1.5 text-sm text-white/70 transition hover:text-white"
      >
        {t.controls.disconnect}
      </button>
    </div>
  );
}

function SlideIndicator({ index, total }: { index: number; total: number }) {
  const { t } = useI18n();
  return (
    <div className="rounded-2xl border border-ink-line bg-ink-soft/40 px-6 py-5 text-center">
      <div className="font-mono text-4xl font-semibold tracking-tight">
        {total > 0 ? index + 1 : 0}
        <span className="text-white/40"> / {total}</span>
      </div>
      <p className="mt-1 text-xs uppercase tracking-widest text-white/45">{t.controls.slideLabel}</p>
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
  const { t } = useI18n();
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
        aria-label={t.controls.goToAria.replace("{total}", String(total))}
        placeholder={t.controls.goTo}
        className="w-full rounded-xl border border-ink-line bg-ink px-3 text-center font-mono outline-none focus:border-beam"
      />
    </form>
  );
}

function NotesPane({ notes, hasNotes }: { notes: string | null; hasNotes: boolean }) {
  const { t } = useI18n();
  return (
    <section aria-labelledby="notes-heading" className="rounded-2xl border border-ink-line bg-ink p-4">
      <h2 id="notes-heading" className="text-xs font-semibold uppercase tracking-widest text-white/45">
        {t.controls.speakerNotes}
      </h2>
      <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-white/80">
        {notes
          ? notes
          : hasNotes
            ? t.controls.noNotesForSlide
            : t.controls.noNotesSidecar}
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
  // Read a shared ticking clock so the display advances between host updates
  // without calling Date.now() during render.
  const { t } = useI18n();
  const now = useWallClock();
  const elapsed = timer.running
    ? timer.elapsedMs + Math.max(0, now - timer.anchorAt)
    : timer.elapsedMs;

  return (
    <section aria-labelledby="timer-heading" className="rounded-2xl border border-ink-line bg-ink-soft/40 p-4">
      <div className="flex items-center justify-between">
        <h2 id="timer-heading" className="text-xs font-semibold uppercase tracking-widest text-white/45">
          {t.controls.timer}
        </h2>
        <span className="font-mono text-2xl tabular-nums" aria-live="off">
          {formatElapsed(elapsed)}
        </span>
      </div>
      <div className="mt-3 grid grid-cols-3 gap-2">
        <TimerButton onClick={() => onTimer(timer.running ? "PAUSE" : "START")}>
          {timer.running ? t.controls.pause : t.controls.start}
        </TimerButton>
        <TimerButton onClick={() => onTimer("PAUSE")}>{t.controls.pause}</TimerButton>
        <TimerButton onClick={() => onTimer("RESET")}>{t.controls.reset}</TimerButton>
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
