"use client";

import { useI18n } from "@/lib/i18n";

// Download targets. Links are placeholders until release artifacts exist (TODO).
// `noteKey` indexes the localized note in the i18n bundle.
const targets = [
  { os: "macOS", noteKey: "macos", href: "#", ready: false },
  { os: "Windows", noteKey: "windows", href: "#", ready: false },
  { os: "Linux", noteKey: "linux", href: "#", ready: false },
  { os: "iOS / Android", noteKey: "mobile", href: "#", ready: false },
] as const;

export function Downloads() {
  const { t } = useI18n();
  return (
    <section
      id="download"
      aria-labelledby="download-heading"
      className="border-t border-ink-line/60 bg-ink-soft/30"
    >
      <div className="mx-auto max-w-content px-6 py-20">
        <h2
          id="download-heading"
          className="text-3xl font-semibold tracking-tight sm:text-4xl"
        >
          {t.downloads.heading}
        </h2>
        <p className="mt-4 max-w-2xl text-white/60">{t.downloads.sub}</p>

        <div className="mt-12 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {targets.map((tg) => (
            <a
              key={tg.os}
              href={tg.href}
              aria-disabled={!tg.ready}
              className="group rounded-2xl border border-ink-line bg-ink p-6 transition hover:border-beam/50"
            >
              <div className="flex items-center justify-between">
                <span className="text-lg font-semibold text-white">{tg.os}</span>
                {!tg.ready && (
                  <span className="rounded-full border border-ink-line px-2 py-0.5 text-[10px] uppercase tracking-wider text-white/50">
                    {t.downloads.soon}
                  </span>
                )}
              </div>
              <p className="mt-2 text-sm text-white/60">{t.downloads.notes[tg.noteKey]}</p>
            </a>
          ))}
        </div>
      </div>
    </section>
  );
}
