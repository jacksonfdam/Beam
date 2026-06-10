const steps = [
  {
    n: "1",
    title: "Open your deck on the desktop",
    body: "Launch Beam on your Mac, Windows, or Linux machine and open the exported PDF. Beam goes fullscreen on your chosen display.",
  },
  {
    n: "2",
    title: "Scan the QR or type the IP",
    body: "The presenter screen shows a QR code, the host IP, and a short PIN. Scan it with the Beam app, or enter the IP and PIN by hand.",
  },
  {
    n: "3",
    title: "Present from your phone",
    body: "Pick the deck, press Start, and navigate. Notes, timer, and drawing are right there in your hand — everything stays on your network.",
  },
];

export function HowToConnect() {
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
          How to connect
        </h2>
        <p className="mt-4 max-w-2xl text-white/60">
          Two devices, one Wi-Fi network, a short PIN. That&apos;s the whole
          handshake — no sign-in, no pairing servers.
        </p>

        <ol className="mt-12 grid grid-cols-1 gap-6 md:grid-cols-3">
          {steps.map((s) => (
            <li
              key={s.n}
              className="relative rounded-2xl border border-ink-line bg-ink-soft/40 p-6"
            >
              <span
                aria-hidden="true"
                className="flex h-9 w-9 items-center justify-center rounded-full border border-beam/40 bg-beam/10 font-mono text-sm text-beam-glow"
              >
                {s.n}
              </span>
              <h3 className="mt-4 text-lg font-semibold text-white">
                {s.title}
              </h3>
              <p className="mt-2 text-sm leading-relaxed text-white/60">
                {s.body}
              </p>
            </li>
          ))}
        </ol>

        <p className="mt-8 text-sm text-white/50">
          Prefer no install? The{" "}
          <a
            href="/remote"
            className="text-beam-glow underline underline-offset-4 hover:text-beam-bright"
          >
            browser remote
          </a>{" "}
          pairs with the same session code over a direct peer-to-peer
          connection.
        </p>
      </div>
    </section>
  );
}
