import { BeamMark } from "./BeamMark";

export function SiteFooter() {
  return (
    <footer className="border-t border-ink-line/60">
      <div className="mx-auto max-w-content px-6 py-14">
        <div className="flex flex-col gap-10 md:flex-row md:justify-between">
          <div className="max-w-sm">
            <div className="flex items-center gap-2 font-semibold">
              <BeamMark className="h-5 w-5 text-beam-bright" />
              <span>Beam</span>
            </div>
            <p className="mt-3 text-sm leading-relaxed text-white/55">
              A local-first PDF presenter. Open → Present → Done.
            </p>
          </div>

          <div className="max-w-md">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-white/70">
              What leaves your device
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-white/55">
              Nothing of substance. Slides, speaker notes, navigation, timer,
              and ink stay on your local network — peer-to-peer between your
              devices. The browser remote uses a server only to exchange the
              initial WebRTC handshake (a session code and connection details);
              once paired, all data is direct. No accounts, no content storage,
              no telemetry.
            </p>
          </div>
        </div>

        <div className="mt-12 flex flex-col gap-3 border-t border-ink-line/60 pt-6 text-sm text-white/45 sm:flex-row sm:items-center sm:justify-between">
          <p>Built for presenters who just want it to work.</p>
          <nav aria-label="Footer" className="flex gap-6">
            <a href="#features" className="hover:text-white/80">
              Features
            </a>
            <a href="#connect" className="hover:text-white/80">
              Connect
            </a>
            <a href="/remote" className="hover:text-white/80">
              Remote
            </a>
          </nav>
        </div>
      </div>
    </footer>
  );
}
