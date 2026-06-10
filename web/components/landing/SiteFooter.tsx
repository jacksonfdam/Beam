"use client";

import { BeamMark } from "./BeamMark";
import { useI18n } from "@/lib/i18n";

export function SiteFooter() {
  const { t } = useI18n();
  return (
    <footer className="border-t border-ink-line/60">
      <div className="mx-auto max-w-content px-6 py-14">
        <div className="flex flex-col gap-10 md:flex-row md:justify-between">
          <div className="max-w-sm">
            <div className="flex items-center gap-2 font-semibold">
              <BeamMark className="h-5 w-5 text-beam-bright" />
              <span>Beam</span>
            </div>
            <p className="mt-3 text-sm leading-relaxed text-white/55">{t.footer.tagline}</p>
          </div>

          <div className="max-w-md">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-white/70">
              {t.footer.whatHeading}
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-white/55">{t.footer.whatBody}</p>
          </div>
        </div>

        <div className="mt-12 flex flex-col gap-3 border-t border-ink-line/60 pt-6 text-sm text-white/45 sm:flex-row sm:items-center sm:justify-between">
          <p>{t.footer.built}</p>
          <nav aria-label="Footer" className="flex gap-6">
            <a href="#features" className="hover:text-white/80">
              {t.footer.features}
            </a>
            <a href="#connect" className="hover:text-white/80">
              {t.footer.connect}
            </a>
            <a href="/remote" className="hover:text-white/80">
              {t.footer.remote}
            </a>
          </nav>
        </div>
      </div>
    </footer>
  );
}
