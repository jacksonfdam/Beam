"use client";

import { useCallback, useEffect, useRef } from "react";

interface Props {
  /** Returns a strokeId. */
  onBegin: (x: number, y: number) => number;
  onExtend: (id: number, x: number, y: number) => void;
  onEnd: (id: number) => void;
  onClear: () => void;
  /** CSS colour for the local preview line. */
  previewColor: string;
}

/**
 * A pointer surface that emits NORMALIZED (0..1) coordinates relative to its
 * own rect — exactly like the native remote — so the host maps them onto the
 * projected slide at any resolution. Draws a local preview for feedback.
 */
export function DrawingSurface({ onBegin, onExtend, onEnd, onClear, previewColor }: Props) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const activeId = useRef<number | null>(null);
  const last = useRef<{ x: number; y: number } | null>(null);

  const resize = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.max(1, Math.round(rect.width * dpr));
    canvas.height = Math.max(1, Math.round(rect.height * dpr));
    const ctx = canvas.getContext("2d");
    if (ctx) ctx.scale(dpr, dpr);
  }, []);

  useEffect(() => {
    resize();
    window.addEventListener("resize", resize);
    return () => window.removeEventListener("resize", resize);
  }, [resize]);

  function norm(e: React.PointerEvent<HTMLCanvasElement>) {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = clamp01((e.clientX - rect.left) / rect.width);
    const y = clamp01((e.clientY - rect.top) / rect.height);
    return { x, y, px: x * rect.width, py: y * rect.height };
  }

  function drawSegment(px: number, py: number) {
    const ctx = canvasRef.current?.getContext("2d");
    if (!ctx || !last.current) return;
    ctx.strokeStyle = previewColor;
    ctx.lineWidth = 3;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.beginPath();
    ctx.moveTo(last.current.x, last.current.y);
    ctx.lineTo(px, py);
    ctx.stroke();
  }

  return (
    <div className="space-y-3">
      <canvas
        ref={canvasRef}
        className="aspect-video w-full touch-none rounded-xl border border-ink-line bg-ink"
        aria-label="Drawing surface — draw to annotate the projected slide"
        role="img"
        onPointerDown={(e) => {
          e.currentTarget.setPointerCapture(e.pointerId);
          const { x, y, px, py } = norm(e);
          activeId.current = onBegin(x, y);
          last.current = { x: px, y: py };
        }}
        onPointerMove={(e) => {
          if (activeId.current == null) return;
          const { x, y, px, py } = norm(e);
          drawSegment(px, py);
          last.current = { x: px, y: py };
          onExtend(activeId.current, x, y);
        }}
        onPointerUp={() => {
          if (activeId.current != null) onEnd(activeId.current);
          activeId.current = null;
          last.current = null;
        }}
        onPointerCancel={() => {
          if (activeId.current != null) onEnd(activeId.current);
          activeId.current = null;
          last.current = null;
        }}
      />
      <button
        type="button"
        onClick={() => {
          const ctx = canvasRef.current?.getContext("2d");
          const canvas = canvasRef.current;
          if (ctx && canvas) ctx.clearRect(0, 0, canvas.width, canvas.height);
          onClear();
        }}
        className="rounded-full border border-ink-line px-4 py-2 text-sm font-medium text-white/80 transition hover:text-white"
      >
        Clear ink
      </button>
    </div>
  );
}

function clamp01(v: number): number {
  return v < 0 ? 0 : v > 1 ? 1 : v;
}
