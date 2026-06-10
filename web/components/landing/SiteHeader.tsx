"use client";

import Link from "next/link";
import { BeamMark } from "./BeamMark";
import { LanguageSwitcher, useI18n } from "@/lib/i18n";

export function SiteHeader() {
  const { t } = useI18n();
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
          <a href="#features" className="hidden text-white/70 transition hover:text-white sm:inline">
            {t.nav.features}
          </a>
          <a href="#connect" className="hidden text-white/70 transition hover:text-white sm:inline">
            {t.nav.howToConnect}
          </a>
          <LanguageSwitcher />
          <Link
            href="/remote"
            className="rounded-full border border-beam/50 bg-beam/10 px-4 py-1.5 font-medium text-beam-glow transition hover:bg-beam/20"
          >
            {t.nav.openRemote}
          </Link>
        </nav>
      </div>
    </header>
  );
}
