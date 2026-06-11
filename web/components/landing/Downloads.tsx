"use client";

import Link from "next/link";
import { useI18n } from "@/lib/i18n";

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
        <p className="mt-4 max-w-2xl text-white/60">{t.downloads.lead}</p>

        <pre className="mt-6 w-full max-w-xl overflow-x-auto rounded-xl border border-ink-line bg-ink p-4 font-mono text-sm leading-relaxed text-beam-glow">
          <code>{`git clone https://github.com/jacksonfdam/Beam.git
cd Beam
./beam`}</code>
        </pre>

        <p className="mt-6 max-w-2xl text-sm text-white/55">
          {t.downloads.browserNote}{" "}
          <Link
            href="/remote"
            className="text-beam-glow underline underline-offset-4 hover:text-beam-bright"
          >
            {t.downloads.browserCta}
          </Link>
        </p>

        <p className="mt-3 text-sm text-white/45">
          <a
            href="https://github.com/jacksonfdam/Beam"
            target="_blank"
            rel="noopener"
            className="underline underline-offset-4 hover:text-white/70"
          >
            github.com/jacksonfdam/Beam
          </a>
        </p>
      </div>
    </section>
  );
}
