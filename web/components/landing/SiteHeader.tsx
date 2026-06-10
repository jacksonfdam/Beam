import Link from "next/link";
import { BeamMark } from "./BeamMark";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-30 border-b border-ink-line/60 bg-ink/70 backdrop-blur">
      <div className="mx-auto flex max-w-content items-center justify-between px-6 py-4">
        <Link
          href="/"
          className="flex items-center gap-2 font-semibold tracking-tight"
          aria-label="Beam home"
        >
          <BeamMark className="h-6 w-6 text-beam-bright" />
          <span>Beam</span>
        </Link>

        <nav aria-label="Primary" className="flex items-center gap-6 text-sm">
          <a href="#features" className="text-white/70 transition hover:text-white">
            Features
          </a>
          <a href="#connect" className="text-white/70 transition hover:text-white">
            How to connect
          </a>
          <Link
            href="/remote"
            className="rounded-full border border-beam/50 bg-beam/10 px-4 py-1.5 font-medium text-beam-glow transition hover:bg-beam/20"
          >
            Open remote
          </Link>
        </nav>
      </div>
    </header>
  );
}
