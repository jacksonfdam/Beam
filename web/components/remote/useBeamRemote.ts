"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { BeamRemote, type RemoteState } from "@/lib/beam-remote";
import {
  ClientMsg,
  normPoint,
  type DeckInfo,
  type HostMessage,
  type NavAction,
  type TimerAction,
} from "@/lib/protocol";

export interface TimerView {
  elapsedMs: number;
  running: boolean;
  /** performance/Date anchor for ticking the display between host updates. */
  anchorAt: number;
}

export interface Presentation {
  decks: DeckInfo[];
  selectedDeckId: string | null;
  index: number;
  total: number;
  notes: string | null;
  hasNotes: boolean;
  timer: TimerView;
  lastError: string | null;
}

const EMPTY: Presentation = {
  decks: [],
  selectedDeckId: null,
  index: 0,
  total: 0,
  notes: null,
  hasNotes: false,
  timer: { elapsedMs: 0, running: false, anchorAt: Date.now() },
  lastError: null,
};

export function useBeamRemote() {
  const remoteRef = useRef<BeamRemote | null>(null);
  const strokeCounter = useRef(0);
  const [state, setState] = useState<RemoteState>({ phase: "idle" });
  const [presentation, setPresentation] = useState<Presentation>(EMPTY);

  const onHostMessage = useCallback((msg: HostMessage) => {
    setPresentation((prev) => reduce(prev, msg));
  }, []);

  const connect = useCallback(
    (sessionCode: string, clientName: string, pin?: string) => {
      remoteRef.current?.close();
      setPresentation(EMPTY);
      const remote = new BeamRemote({ onState: setState, onHostMessage });
      remoteRef.current = remote;
      void remote.connect(sessionCode.trim(), clientName.trim() || "Browser", pin?.trim() || undefined);
    },
    [onHostMessage],
  );

  const disconnect = useCallback(() => {
    remoteRef.current?.close();
    remoteRef.current = null;
    setState({ phase: "idle" });
    setPresentation(EMPTY);
  }, []);

  useEffect(() => () => remoteRef.current?.close(), []);

  const selectDeck = useCallback((deckId: string) => {
    remoteRef.current?.send(ClientMsg.selectDeck(deckId));
  }, []);
  const nav = useCallback((action: NavAction) => {
    remoteRef.current?.send(ClientMsg.nav(action));
  }, []);
  const goTo = useCallback((index: number) => {
    remoteRef.current?.send(ClientMsg.goto(index));
  }, []);
  const timer = useCallback((action: TimerAction) => {
    remoteRef.current?.send(ClientMsg.timer(action));
  }, []);

  const beginStroke = useCallback(
    (x: number, y: number, colorArgb: number, widthDp: number): number => {
      const id = ++strokeCounter.current;
      remoteRef.current?.send(ClientMsg.strokeStart(id, colorArgb, widthDp, normPoint(x, y)));
      return id;
    },
    [],
  );
  const extendStroke = useCallback((id: number, x: number, y: number) => {
    remoteRef.current?.send(ClientMsg.strokePoint(id, normPoint(x, y)));
  }, []);
  const endStroke = useCallback((id: number) => {
    remoteRef.current?.send(ClientMsg.strokeEnd(id));
  }, []);
  const clearInk = useCallback(() => {
    remoteRef.current?.send(ClientMsg.clearInk());
  }, []);

  return {
    state,
    presentation,
    connect,
    disconnect,
    selectDeck,
    nav,
    goTo,
    timer,
    beginStroke,
    extendStroke,
    endStroke,
    clearInk,
  };
}

function reduce(prev: Presentation, msg: HostMessage): Presentation {
  switch (msg.type) {
    case "hello_ack":
      return { ...prev, decks: msg.decks, lastError: null };
    case "hello_reject":
      return { ...prev, lastError: msg.reason };
    case "deck_selected":
      return {
        ...prev,
        selectedDeckId: msg.deckId,
        total: msg.slideCount,
        hasNotes: msg.hasNotes,
        index: 0,
        notes: null,
      };
    case "slide_changed":
      return {
        ...prev,
        index: msg.index,
        total: msg.total,
        notes: msg.notes ?? null,
      };
    case "timer_state":
      return {
        ...prev,
        timer: { elapsedMs: msg.elapsedMs, running: msg.running, anchorAt: Date.now() },
      };
    case "error":
      return { ...prev, lastError: msg.message };
    case "pong":
    default:
      return prev;
  }
}
