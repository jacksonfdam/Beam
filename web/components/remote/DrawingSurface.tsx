"use client";

import { useEffect, useRef, useState } from "react";
import { useI18n } from "@/lib/i18n";

export type DrawTool = "PEN" | "MARKER" | "SPOTLIGHT";

const PEN = { argb: 0xffef4444, css: "#ef4444", width: 4 };
const MARKER = { argb: 0x55ffeb3b, css: "rgba(255,235,59,0.35)", width: 18 };

interface Props {
  image: string | null; // data URL background (slide or live screen)
  aspect: number; // used when there's no image
  tool: DrawTool;
  onBegin: (x: number, y: number, colorArgb: number, widthDp: number) => number;
  onExtend: (id: number, x: number, y: number) => void;
  onEnd: (id: number) => void;
  onClear: () => void;
  onSpotlight: (left: number, top: number, right: number, bottom: number) => void;
}

interface Stroke {
  points: { x: number; y: number }[];
  css: string;
  width: number;
}

/**
 * Canvas surface mirroring the native remote: draws over the slide/screen image,
 * supports pen/marker/spotlight, and pinch/wheel zoom + pan. Emits NORMALIZED
 * (0..1) coordinates so strokes land at the right spot on the host.
 */
export function DrawingSurface({ image, aspect, tool, onBegin, onExtend, onEnd, onClear, onSpotlight }: Props) {
  const { t } = useI18n();
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const imgRef = useRef<HTMLImageElement | null>(null);
  const scale = useRef(1);
  const pan = useRef({ x: 0, y: 0 });
  const completed = useRef<Stroke[]>([]);
  const active = useRef<Stroke | null>(null);
  const activeId = useRef<number | null>(null);
  const spot = useRef<{ ax: number; ay: number; bx: number; by: number } | null>(null);
  const pointers = useRef(new Map<number, { x: number; y: number }>());
  const pinch = useRef<{ dist: number; cx: number; cy: number } | null>(null);
  const toolRef = useRef(tool);
  toolRef.current = tool;
  const [imgAspect, setImgAspect] = useState<number | null>(null);

  function cssSize() {
    const c = canvasRef.current;
    const dpr = window.devicePixelRatio || 1;
    return c ? { w: c.width / dpr, h: c.height / dpr } : { w: 1, h: 1 };
  }
  function contentOf(px: number, py: number) {
    return { x: (px - pan.current.x) / scale.current, y: (py - pan.current.y) / scale.current };
  }
  function normOf(px: number, py: number) {
    const { w, h } = cssSize();
    const c = contentOf(px, py);
    return { x: clamp01(c.x / w), y: clamp01(c.y / h) };
  }

  function draw() {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!canvas || !ctx) return;
    const dpr = window.devicePixelRatio || 1;
    const w = canvas.width / dpr;
    const h = canvas.height / dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = "#13161d";
    ctx.fillRect(0, 0, w, h);
    ctx.save();
    ctx.translate(pan.current.x, pan.current.y);
    ctx.scale(scale.current, scale.current);
    if (imgRef.current) ctx.drawImage(imgRef.current, 0, 0, w, h);
    const drawStroke = (s: Stroke) => {
      if (s.points.length === 0) return;
      ctx.strokeStyle = s.css;
      ctx.lineWidth = s.width / scale.current;
      ctx.lineCap = "round";
      ctx.lineJoin = "round";
      ctx.beginPath();
      ctx.moveTo(s.points[0].x, s.points[0].y);
      for (let i = 1; i < s.points.length; i++) ctx.lineTo(s.points[i].x, s.points[i].y);
      ctx.stroke();
    };
    completed.current.forEach(drawStroke);
    if (active.current) drawStroke(active.current);
    if (spot.current) {
      const s = spot.current;
      ctx.strokeStyle = "#ffc107";
      ctx.lineWidth = 2 / scale.current;
      ctx.strokeRect(Math.min(s.ax, s.bx), Math.min(s.ay, s.by), Math.abs(s.bx - s.ax), Math.abs(s.by - s.ay));
    }
    ctx.restore();
  }

  function resize() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.max(1, Math.round(rect.width * dpr));
    canvas.height = Math.max(1, Math.round(rect.height * dpr));
    draw();
  }

  // Load the background image whenever it changes; keep zoom/strokes.
  useEffect(() => {
    if (!image) {
      imgRef.current = null;
      setImgAspect(null);
      draw();
      return;
    }
    const el = new Image();
    el.onload = () => {
      imgRef.current = el;
      if (el.naturalHeight > 0) setImgAspect(el.naturalWidth / el.naturalHeight);
      draw();
    };
    el.src = image;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [image]);

  useEffect(() => {
    resize();
    window.addEventListener("resize", resize);
    return () => window.removeEventListener("resize", resize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function localXY(e: React.PointerEvent<HTMLCanvasElement>) {
    const rect = e.currentTarget.getBoundingClientRect();
    return { x: e.clientX - rect.left, y: e.clientY - rect.top };
  }

  function onDown(e: React.PointerEvent<HTMLCanvasElement>) {
    e.currentTarget.setPointerCapture(e.pointerId);
    const p = localXY(e);
    pointers.current.set(e.pointerId, p);
    if (pointers.current.size >= 2) {
      // entering pinch: abort any draw
      if (activeId.current != null) { onEnd(activeId.current); activeId.current = null; active.current = null; }
      spot.current = null;
      const pts = [...pointers.current.values()];
      pinch.current = pinchState(pts[0], pts[1]);
      return;
    }
    if (toolRef.current === "SPOTLIGHT") {
      const c = contentOf(p.x, p.y);
      spot.current = { ax: c.x, ay: c.y, bx: c.x, by: c.y };
    } else {
      const style = toolRef.current === "MARKER" ? MARKER : PEN;
      const n = normOf(p.x, p.y);
      activeId.current = onBegin(n.x, n.y, style.argb, style.width);
      const c = contentOf(p.x, p.y);
      active.current = { points: [c], css: style.css, width: style.width };
    }
    draw();
  }

  function onMove(e: React.PointerEvent<HTMLCanvasElement>) {
    if (!pointers.current.has(e.pointerId)) return;
    const p = localXY(e);
    pointers.current.set(e.pointerId, p);
    if (pointers.current.size >= 2) {
      const pts = [...pointers.current.values()];
      const next = pinchState(pts[0], pts[1]);
      const prev = pinch.current;
      if (prev) {
        const factor = next.dist / Math.max(1, prev.dist);
        scale.current = clamp(scale.current * factor, 1, 6);
        pan.current = scale.current <= 1
          ? { x: 0, y: 0 }
          : { x: pan.current.x + (next.cx - prev.cx), y: pan.current.y + (next.cy - prev.cy) };
      }
      pinch.current = next;
      draw();
      return;
    }
    if (toolRef.current === "SPOTLIGHT") {
      if (spot.current) {
        const c = contentOf(p.x, p.y);
        spot.current = { ...spot.current, bx: c.x, by: c.y };
        draw();
      }
    } else if (activeId.current != null && active.current) {
      const n = normOf(p.x, p.y);
      onExtend(activeId.current, n.x, n.y);
      active.current.points.push(contentOf(p.x, p.y));
      draw();
    }
  }

  function endPointer(e: React.PointerEvent<HTMLCanvasElement>) {
    pointers.current.delete(e.pointerId);
    if (pointers.current.size < 2) pinch.current = null;
    if (pointers.current.size > 0) return;
    if (activeId.current != null) {
      if (active.current) completed.current.push(active.current);
      active.current = null;
      onEnd(activeId.current);
      activeId.current = null;
    }
    if (toolRef.current === "SPOTLIGHT" && spot.current) {
      const { w, h } = cssSize();
      const s = spot.current;
      onSpotlight(
        clamp01(Math.min(s.ax, s.bx) / w),
        clamp01(Math.min(s.ay, s.by) / h),
        clamp01(Math.max(s.ax, s.bx) / w),
        clamp01(Math.max(s.ay, s.by) / h),
      );
      spot.current = null;
    }
    draw();
  }

  function onWheel(e: React.WheelEvent<HTMLCanvasElement>) {
    const p = { x: e.clientX - e.currentTarget.getBoundingClientRect().left, y: e.clientY - e.currentTarget.getBoundingClientRect().top };
    const before = contentOf(p.x, p.y);
    scale.current = clamp(scale.current * (e.deltaY < 0 ? 1.1 : 0.9), 1, 6);
    // keep the point under the cursor stable
    pan.current = scale.current <= 1 ? { x: 0, y: 0 } : { x: p.x - before.x * scale.current, y: p.y - before.y * scale.current };
    draw();
  }

  function resetZoom() {
    scale.current = 1;
    pan.current = { x: 0, y: 0 };
    draw();
  }

  return (
    <div className="space-y-3">
      <canvas
        ref={canvasRef}
        style={{ aspectRatio: String(imgAspect ?? aspect) }}
        className="w-full touch-none rounded-xl border border-ink-line bg-ink"
        aria-label={t.controls.drawingAria}
        role="img"
        onPointerDown={onDown}
        onPointerMove={onMove}
        onPointerUp={endPointer}
        onPointerCancel={endPointer}
        onWheel={onWheel}
      />
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => {
            completed.current = [];
            active.current = null;
            spot.current = null;
            draw();
            onClear();
          }}
          className="rounded-full border border-ink-line px-4 py-2 text-sm font-medium text-white/80 transition hover:text-white"
        >
          {t.controls.clearInk}
        </button>
        <button
          type="button"
          onClick={resetZoom}
          className="rounded-full border border-ink-line px-4 py-2 text-sm font-medium text-white/80 transition hover:text-white"
        >
          {t.controls.resetZoom}
        </button>
      </div>
    </div>
  );
}

function pinchState(a: { x: number; y: number }, b: { x: number; y: number }) {
  return { dist: Math.hypot(a.x - b.x, a.y - b.y), cx: (a.x + b.x) / 2, cy: (a.y + b.y) / 2 };
}
function clamp(v: number, lo: number, hi: number): number {
  return v < lo ? lo : v > hi ? hi : v;
}
function clamp01(v: number): number {
  return clamp(v, 0, 1);
}
