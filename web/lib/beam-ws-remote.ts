/*
 * BeamWsRemote — browser remote over a direct LAN WebSocket.
 *
 * The desktop host speaks the Beam protocol over a Ktor WebSocket at
 * ws://<host>:<port>/cue. This connects to it straight from the browser and
 * speaks the same JSON the native clients do — no WebRTC, no signaling.
 *
 * Caveat: browsers block ws:// (insecure) from an https:// page, so the page
 * must be served over http (e.g. `npm run dev`, or an http LAN host) for this
 * to connect to a plain-WebSocket host.
 */

import {
  ClientMsg,
  decodeHostMessage,
  encode,
  type ClientMessage,
  type HostEndpoint,
  type HostMessage,
} from "./protocol";
import type { RemoteState } from "./beam-remote";

export interface BeamRemoteCallbacks {
  onState: (state: RemoteState) => void;
  onHostMessage: (msg: HostMessage) => void;
}

export class BeamWsRemote {
  private ws: WebSocket | null = null;
  private state: RemoteState = { phase: "idle" };

  constructor(private readonly cb: BeamRemoteCallbacks) {}

  connect(endpoint: HostEndpoint, clientName: string): void {
    this.close();
    this.setState({ phase: "connecting" });

    let ws: WebSocket;
    try {
      ws = new WebSocket(`ws://${endpoint.host}:${endpoint.port}/cue`);
    } catch (e) {
      this.setState({ phase: "failed", error: e instanceof Error ? e.message : String(e) });
      return;
    }
    this.ws = ws;

    ws.onopen = () => {
      this.setState({ phase: "handshaking" });
      ws.send(encode(ClientMsg.hello(clientName, endpoint.pin ?? null)));
    };
    ws.onmessage = (ev) => {
      if (typeof ev.data !== "string") return;
      let msg: HostMessage;
      try {
        msg = decodeHostMessage(ev.data);
      } catch {
        return; // ignore anything that isn't a known host message
      }
      if (msg.type === "hello_ack") this.setState({ phase: "connected", session: msg });
      else if (msg.type === "hello_reject") this.setState({ phase: "rejected", error: msg.reason });
      this.cb.onHostMessage(msg);
    };
    ws.onerror = () => {
      if (this.state.phase !== "connected") {
        this.setState({
          phase: "failed",
          error: "Couldn't reach the host. Check the IP/PIN, same Wi‑Fi, and that this page is served over http.",
        });
      }
    };
    ws.onclose = () => {
      if (this.state.phase === "connected" || this.state.phase === "handshaking") {
        this.setState({ phase: "closed" });
      }
    };
  }

  send(msg: ClientMessage): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) this.ws.send(encode(msg));
  }

  close(): void {
    this.ws?.close();
    this.ws = null;
  }

  private setState(state: RemoteState): void {
    this.state = state;
    this.cb.onState(state);
  }
}
