/*
 * BeamRemote — the browser peer.
 *
 * Acts as the WebRTC *offerer*: it opens a DataChannel, exchanges SDP/ICE with
 * a Beam desktop host through the signaling API, and then speaks the Beam wire
 * protocol (see lib/protocol.ts) directly over the channel. After the channel
 * opens, nothing else touches the server.
 *
 * STUN is enough on a shared LAN (host candidates connect directly). Cross-
 * network use would need a TURN relay, which is out of scope here.
 */

import {
  ClientMsg,
  decodeHostMessage,
  encode,
  type ClientMessage,
  type HostMessage,
  type HelloAck,
} from "./protocol";
import { getAnswer, getIce, postIce, postOffer } from "./signaling-client";

const ICE_SERVERS: RTCIceServer[] = [
  { urls: "stun:stun.l.google.com:19302" },
];

const POLL_INTERVAL_MS = 1000;
const PAIR_TIMEOUT_MS = 60_000;
const DATA_CHANNEL_LABEL = "cue";

export type RemotePhase =
  | "idle"
  | "connecting" // exchanging SDP/ICE
  | "handshaking" // channel open, awaiting hello_ack
  | "connected"
  | "rejected" // bad PIN / version
  | "failed"
  | "closed";

export interface RemoteState {
  phase: RemotePhase;
  session?: HelloAck;
  error?: string;
}

export interface BeamRemoteCallbacks {
  onState: (state: RemoteState) => void;
  onHostMessage: (msg: HostMessage) => void;
}

export class BeamRemote {
  private pc: RTCPeerConnection | null = null;
  private dc: RTCDataChannel | null = null;
  private state: RemoteState = { phase: "idle" };
  private aborted = false;
  private timers: ReturnType<typeof setTimeout>[] = [];

  constructor(private readonly cb: BeamRemoteCallbacks) {}

  getState(): RemoteState {
    return this.state;
  }

  async connect(sessionCode: string, clientName: string, pin?: string): Promise<void> {
    if (typeof window === "undefined" || typeof RTCPeerConnection === "undefined") {
      this.setState({ phase: "failed", error: "WebRTC is not available in this browser." });
      return;
    }

    this.setState({ phase: "connecting" });
    this.helloOnOpen = { clientName, pin };

    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });
    this.pc = pc;

    pc.onicecandidate = (e) => {
      if (e.candidate) {
        void postIce(sessionCode, "offerer", e.candidate.toJSON()).catch(() => {});
      }
    };
    pc.onconnectionstatechange = () => {
      if (this.aborted) return;
      const s = pc.connectionState;
      if (s === "failed" || s === "disconnected") {
        this.setState({ phase: "failed", error: "Connection lost." });
      }
    };

    const dc = pc.createDataChannel(DATA_CHANNEL_LABEL, { ordered: true });
    this.bindDataChannel(dc);

    try {
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      await postOffer(sessionCode, JSON.stringify({ type: offer.type, sdp: offer.sdp }));
    } catch {
      this.setState({ phase: "failed", error: "Could not create the connection offer." });
      return;
    }

    this.pollForAnswer(sessionCode, pc);
    this.pollForIce(sessionCode, pc);
    this.armTimeout();
  }

  send(msg: ClientMessage): void {
    if (this.dc && this.dc.readyState === "open") {
      this.dc.send(encode(msg));
    }
  }

  close(): void {
    this.aborted = true;
    this.timers.forEach(clearTimeout);
    this.timers = [];
    this.dc?.close();
    this.pc?.close();
    this.dc = null;
    this.pc = null;
    this.setState({ phase: "closed" });
  }

  // --- internals ----------------------------------------------------------

  private helloOnOpen: { clientName: string; pin?: string } | null = null;

  private bindDataChannel(dc: RTCDataChannel): void {
    this.dc = dc;
    dc.onopen = () => {
      if (this.aborted) return;
      this.setState({ phase: "handshaking" });
      if (this.helloOnOpen) {
        this.send(ClientMsg.hello(this.helloOnOpen.clientName, this.helloOnOpen.pin));
      }
    };
    dc.onmessage = (e) => {
      if (this.aborted) return;
      let msg: HostMessage;
      try {
        msg = decodeHostMessage(typeof e.data === "string" ? e.data : "");
      } catch {
        return; // ignore anything that isn't a known host message
      }
      if (msg.type === "hello_ack") {
        this.setState({ phase: "connected", session: msg });
      } else if (msg.type === "hello_reject") {
        this.setState({ phase: "rejected", error: msg.reason });
      }
      this.cb.onHostMessage(msg);
    };
    dc.onclose = () => {
      if (this.aborted) return;
      this.setState({ phase: "closed" });
    };
  }

  private pollForAnswer(sessionCode: string, pc: RTCPeerConnection): void {
    const tick = async () => {
      if (this.aborted || pc.currentRemoteDescription) return;
      try {
        const { sdp } = await getAnswer(sessionCode);
        if (sdp) {
          const desc = JSON.parse(sdp) as RTCSessionDescriptionInit;
          await pc.setRemoteDescription(desc);
          return; // answer applied; ICE poll continues until connected
        }
      } catch {
        // transient; keep polling
      }
      this.timers.push(setTimeout(tick, POLL_INTERVAL_MS));
    };
    void tick();
  }

  private pollForIce(sessionCode: string, pc: RTCPeerConnection): void {
    const tick = async () => {
      if (this.aborted || pc.connectionState === "connected") return;
      try {
        const { candidates } = await getIce(sessionCode, "offerer");
        for (const c of candidates) {
          try {
            await pc.addIceCandidate(c);
          } catch {
            // candidate may arrive before remote description; ignore
          }
        }
      } catch {
        // transient; keep polling
      }
      this.timers.push(setTimeout(tick, POLL_INTERVAL_MS));
    };
    void tick();
  }

  private armTimeout(): void {
    this.timers.push(
      setTimeout(() => {
        if (this.aborted) return;
        if (this.state.phase === "connecting" || this.state.phase === "handshaking") {
          this.setState({
            phase: "failed",
            error: "Timed out waiting for the host. Check the session code and that the host is running.",
          });
        }
      }, PAIR_TIMEOUT_MS),
    );
  }

  private setState(next: RemoteState): void {
    this.state = next;
    this.cb.onState(next);
  }
}
