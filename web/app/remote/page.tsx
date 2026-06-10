"use client";

import Link from "next/link";
import { useBeamRemote } from "@/components/remote/useBeamRemote";
import { PairingForm } from "@/components/remote/PairingForm";
import { RemoteControls } from "@/components/remote/RemoteControls";
import { BeamMark } from "@/components/landing/BeamMark";

const INK_COLOR_ARGB = 0xffef4444;
const INK_WIDTH_DP = 4;

export default function RemotePage() {
  const r = useBeamRemote();
  const connected = r.state.phase === "connected";

  return (
    <main className="mx-auto flex min-h-screen max-w-content flex-col px-6 py-8">
      <Link href="/" className="mb-8 inline-flex items-center gap-2 text-sm text-white/70 hover:text-white" aria-label="Beam home">
        <BeamMark className="h-5 w-5 text-beam-bright" />
        <span className="font-semibold">Beam</span>
        <span className="text-white/40">remote</span>
      </Link>

      <div className="flex flex-1 items-start justify-center">
        {connected ? (
          <RemoteControls
            presentation={r.presentation}
            sessionName={r.state.session?.sessionName ?? ""}
            onSelectDeck={r.selectDeck}
            onNav={r.nav}
            onGoTo={r.goTo}
            onTimer={r.timer}
            onBeginStroke={(x, y) => r.beginStroke(x, y, INK_COLOR_ARGB, INK_WIDTH_DP)}
            onExtendStroke={r.extendStroke}
            onEndStroke={r.endStroke}
            onClearInk={r.clearInk}
            onDisconnect={r.disconnect}
          />
        ) : (
          <PairingForm
            state={r.state}
            onConnect={(code, name) => r.connect(code, name)}
            onCancel={r.disconnect}
          />
        )}
      </div>

      <p className="mt-10 text-center text-xs text-white/40">
        Pairing uses a server only for the initial handshake. Slides, notes, and
        ink travel directly between your browser and the host.
      </p>
    </main>
  );
}
