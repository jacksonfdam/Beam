// Download targets. Links are placeholders until release artifacts exist (TODO).
const targets = [
  { os: "macOS", note: "Apple silicon & Intel", href: "#", ready: false },
  { os: "Windows", note: "Windows 10 & 11", href: "#", ready: false },
  { os: "Linux", note: "AppImage & .deb", href: "#", ready: false },
  { os: "iOS / Android", note: "The remote app", href: "#", ready: false },
];

export function Downloads() {
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
          Download Beam
        </h2>
        <p className="mt-4 max-w-2xl text-white/60">
          The desktop app is the presenter and host. The mobile app is the
          remote. Builds are on the way.
        </p>

        <div className="mt-12 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {targets.map((t) => (
            <a
              key={t.os}
              href={t.href}
              aria-disabled={!t.ready}
              className="group rounded-2xl border border-ink-line bg-ink p-6 transition hover:border-beam/50"
            >
              <div className="flex items-center justify-between">
                <span className="text-lg font-semibold text-white">{t.os}</span>
                {!t.ready && (
                  <span className="rounded-full border border-ink-line px-2 py-0.5 text-[10px] uppercase tracking-wider text-white/50">
                    Soon
                  </span>
                )}
              </div>
              <p className="mt-2 text-sm text-white/60">{t.note}</p>
            </a>
          ))}
        </div>
      </div>
    </section>
  );
}
