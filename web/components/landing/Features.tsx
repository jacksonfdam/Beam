"use client";

import { useI18n } from "@/lib/i18n";

export function Features() {
  const { t } = useI18n();
  return (
    <section
      id="features"
      aria-labelledby="features-heading"
      className="border-t border-ink-line/60 bg-ink-soft/30"
    >
      <div className="mx-auto max-w-content px-6 py-20">
        <h2
          id="features-heading"
          className="max-w-2xl text-3xl font-semibold tracking-tight sm:text-4xl"
        >
          {t.features.heading}
        </h2>
        <p className="mt-4 max-w-2xl text-white/60">{t.features.sub}</p>

        <ul className="mt-12 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {t.features.items.map((f) => (
            <li
              key={f.title}
              className="rounded-2xl border border-ink-line bg-ink p-6"
            >
              <h3 className="text-lg font-semibold text-white">{f.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-white/60">{f.body}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
