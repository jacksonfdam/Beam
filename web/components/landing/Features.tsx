const features = [
  {
    title: "Exact-as-designed rendering",
    body: "Beam renders the PDF itself, so fonts, gradients, and layouts look precisely as you exported them — on any machine, on any projector.",
  },
  {
    title: "Your phone is the remote",
    body: "Connect over the local network and drive the deck from your hand: next, previous, jump to a slide. No perceptible lag.",
  },
  {
    title: "Speaker notes, privately",
    body: "Notes for the current slide show on your phone's presenter view — pulled from a sidecar file you control, never embedded in the projected slide.",
  },
  {
    title: "Live drawing on the slide",
    body: "Draw with your finger and the ink appears on the projected slide instantly, mapped to the right spot at any resolution. Clear with one tap.",
  },
  {
    title: "Host-owned timer",
    body: "Start, pause, and reset a talk timer. The host owns the clock, so it keeps perfect time even if your phone drops and reconnects.",
  },
  {
    title: "Multi-screen aware",
    body: "Present fullscreen on the external display while your laptop keeps the presenter view. Pick which screen beams the slides.",
  },
  {
    title: "Fully local, fully private",
    body: "No accounts, no cloud storage, no telemetry. Devices talk directly over your Wi-Fi. Slides and notes never leave your network.",
  },
  {
    title: "Browser remote, no install",
    body: "No app on hand? Open the remote in any browser and pair with a session code. It speaks the same protocol as the native remote.",
  },
];

export function Features() {
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
          Everything a talk needs, nothing it doesn&apos;t
        </h2>
        <p className="mt-4 max-w-2xl text-white/60">
          Beam presents — it doesn&apos;t author. That focus is why it stays
          fast, private, and dead simple.
        </p>

        <ul className="mt-12 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((f) => (
            <li
              key={f.title}
              className="rounded-2xl border border-ink-line bg-ink p-6"
            >
              <h3 className="text-lg font-semibold text-white">{f.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-white/60">
                {f.body}
              </p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
