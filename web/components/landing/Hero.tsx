"use client";

import Link from "next/link";
import { useI18n } from "@/lib/i18n";

export function Hero() {
  const { t } = useI18n();
  return (
    <section className="relative overflow-hidden">
      {/* Soft beam of light behind the headline. Decorative. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute left-1/2 top-[-10rem] h-[28rem] w-[40rem] -translate-x-1/2 rounded-full bg-beam/20 blur-[120px]"
      />
      <div className="mx-auto max-w-content px-6 pb-20 pt-20 text-center sm:pt-28">
        <p className="mb-5 inline-flex items-center gap-2 rounded-full border border-ink-line bg-ink-soft px-4 py-1.5 text-xs font-medium uppercase tracking-widest text-beam-glow">
          {t.hero.badge}
        </p>
        <h1 className="mx-auto max-w-3xl text-balance text-4xl font-semibold leading-[1.1] tracking-tight sm:text-6xl">
          {t.hero.title}
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg text-white/70">{t.hero.subtitle}</p>

        <p className="mt-8 font-mono text-sm tracking-[0.3em] text-beam-glow">{t.hero.flow}</p>

        <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <a
            href="#download"
            className="w-full rounded-full bg-beam px-6 py-3 font-semibold text-ink transition hover:bg-beam-bright sm:w-auto"
          >
            {t.hero.download}
          </a>
          <Link
            href="/remote"
            className="w-full rounded-full border border-ink-line px-6 py-3 font-semibold text-white transition hover:border-beam/60 hover:text-beam-glow sm:w-auto"
          >
            {t.hero.useBrowserRemote}
          </Link>
        </div>
      </div>
    </section>
  );
}
