"use client";

import { useI18n } from "@/lib/i18n";

export function HowToConnect() {
  const { t } = useI18n();
  return (
    <section
      id="connect"
      aria-labelledby="connect-heading"
      className="border-t border-ink-line/60"
    >
      <div className="mx-auto max-w-content px-6 py-20">
        <h2
          id="connect-heading"
          className="text-3xl font-semibold tracking-tight sm:text-4xl"
        >
          {t.connect.heading}
        </h2>
        <p className="mt-4 max-w-2xl text-white/60">{t.connect.sub}</p>

        <ol className="mt-12 grid grid-cols-1 gap-6 md:grid-cols-3">
          {t.connect.steps.map((s, i) => (
            <li
              key={s.title}
              className="relative rounded-2xl border border-ink-line bg-ink-soft/40 p-6"
            >
              <span
                aria-hidden="true"
                className="flex h-9 w-9 items-center justify-center rounded-full border border-beam/40 bg-beam/10 font-mono text-sm text-beam-glow"
              >
                {i + 1}
              </span>
              <h3 className="mt-4 text-lg font-semibold text-white">{s.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-white/60">{s.body}</p>
            </li>
          ))}
        </ol>

        <p className="mt-8 text-sm text-white/50">
          {t.connect.preferPre}
          <a
            href="/remote"
            className="text-beam-glow underline underline-offset-4 hover:text-beam-bright"
          >
            {t.connect.browserLink}
          </a>
          {t.connect.preferPost}
        </p>
      </div>
    </section>
  );
}
