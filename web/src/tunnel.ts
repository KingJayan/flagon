import { OPENED, PAUSE, RESUME } from '../wire';

/**
 * the page half of the socket proxy: one websocket per connection, with the reader's demand
 * paced back to the far end so a slow reader cannot be buried.
 */

const HOLD_AT = 256 * 1024;
const RESUME_AT = 64 * 1024;

type Settled = (error: string | null, result: unknown) => void;

export interface Connection {
  socket: WebSocket;
  chunks: Int8Array[];
  queued: number;
  pending: Settled | null;
  closed: boolean;
  failure: string | null;
  opened: Settled | null;
  held: boolean;
}

export interface Tunnel {
  open(url: string, done: Settled): void;
  receive(state: Connection, done: Settled): void;
  available(state: Connection): number;
  isOpen(state: Connection): boolean;
  buffered(state: Connection): number;
  send(state: Connection, data: Int8Array): void;
  close(state: Connection): void;
}

declare global {
  interface Window {
    __flagonNet?: Tunnel;
  }
}

function missingAt(url: string): string {
  return (
    `no flagon tcp proxy answered at ${url}; a page reaches the network only through one` +
    ' served with it, which a hosted build has to run for itself'
  );
}

function flow(state: Connection): void {
  if (state.socket.readyState !== WebSocket.OPEN) {
    return;
  }
  if (!state.held && state.queued >= HOLD_AT) {
    state.held = true;
    state.socket.send(PAUSE);
  } else if (state.held && state.queued <= RESUME_AT) {
    state.held = false;
    state.socket.send(RESUME);
  }
}

function deliver(state: Connection): void {
  const done = state.pending;
  if (done === null) {
    return;
  }
  const chunk = state.chunks.shift();
  if (chunk !== undefined) {
    state.pending = null;
    state.queued -= chunk.length;
    flow(state);
    done(null, chunk);
  } else if (state.failure !== null) {
    state.pending = null;
    done(state.failure, null);
  } else if (state.closed) {
    state.pending = null;
    done(null, null);
  }
}

/** an error before the socket opened answers the opener; after it, the next reader */
function report(state: Connection, message: string): void {
  const open = state.opened;
  if (open !== null) {
    state.opened = null;
    open(message, null);
    return;
  }
  state.failure = message;
  deliver(state);
}

export function tunnel(): Tunnel {
  return {
    open(url: string, done: Settled): void {
      const socket = new WebSocket(url);
      socket.binaryType = 'arraybuffer';
      const state: Connection = {
        socket,
        chunks: [],
        queued: 0,
        pending: null,
        closed: false,
        failure: null,
        opened: done,
        held: false,
      };
      socket.addEventListener('message', (event: MessageEvent): void => {
        if (typeof event.data === 'string') {
          const open = state.opened;
          if (open !== null && event.data === OPENED) {
            state.opened = null;
            open(null, state);
          } else {
            report(state, event.data);
          }
          return;
        }
        const chunk = new Int8Array(event.data as ArrayBuffer);
        if (chunk.length > 0) {
          state.chunks.push(chunk);
          state.queued += chunk.length;
          flow(state);
        }
        deliver(state);
      });
      socket.addEventListener('close', (): void => {
        state.closed = true;
        const open = state.opened;
        if (open !== null) {
          state.opened = null;
          open(missingAt(url), null);
          return;
        }
        deliver(state);
      });
      socket.addEventListener('error', (): void => {
        report(state, state.opened !== null ? missingAt(url) : 'the connection to the proxy failed');
      });
    },
    receive(state: Connection, done: Settled): void {
      state.pending = done;
      deliver(state);
    },
    available: (state: Connection): number => state.queued,
    isOpen: (state: Connection): boolean => !state.closed && state.failure === null,
    buffered: (state: Connection): number => state.socket.bufferedAmount,
    send(state: Connection, data: Int8Array): void {
      state.socket.send(data);
    },
    close(state: Connection): void {
      state.closed = true;
      if (state.socket.readyState < WebSocket.CLOSING) {
        state.socket.close();
      }
      deliver(state);
    },
  };
}

export function installTunnel(): void {
  window.__flagonNet ??= tunnel();
}
