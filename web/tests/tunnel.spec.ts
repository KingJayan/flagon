import { test, expect } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { tunnel, type Connection } from '../src/tunnel';
import { OPENED, PAUSE, RESUME, TCP_PROXY_PATH } from '../wire';

const here = path.dirname(fileURLToPath(import.meta.url));
const TUNNEL_JAVA = path.join(
  here,
  '../../platform-browser/src/main/java/dev/flagon/platform/browser/net/Tunnel.java',
);

test.describe('the words both sides of the tunnel say', () => {
  test('are the ones the java side was compiled against', async () => {
    const source = await fs.readFile(TUNNEL_JAVA, 'utf8');
    const declared = /String PATH = "([^"]+)"/.exec(source);

    expect(declared?.[1]).toBe(TCP_PROXY_PATH);
  });

  test('are the ones the proxy answers with', async () => {
    const source = await fs.readFile(path.join(here, '../tcp-proxy.ts'), 'utf8');

    expect(source).toContain('OPENED');
    expect(source).toContain('PAUSE');
    expect(source).not.toMatch(/'\/__flagon\/tcp'/);
  });
});

// a websocket that never touches the network, so the flow control can be driven a byte at a time
class FakeSocket {
  static OPEN = 1;
  readyState = 1;
  binaryType = '';
  bufferedAmount = 0;
  sent: (string | Int8Array)[] = [];
  private listeners: Record<string, ((event: unknown) => void)[]> = {};

  addEventListener(kind: string, listener: (event: unknown) => void): void {
    (this.listeners[kind] ??= []).push(listener);
  }

  private fire(kind: string, event?: unknown): void {
    for (const listener of this.listeners[kind] ?? []) {
      listener(event);
    }
  }

  send(data: string | Int8Array): void {
    this.sent.push(data);
  }

  close(): void {
    this.readyState = 3;
    this.fire('close');
  }

  arrive(bytes: number): void {
    this.fire('message', { data: new Int8Array(bytes).buffer });
  }

  say(word: string): void {
    this.fire('message', { data: word });
  }
}

let sockets: FakeSocket[] = [];

test.beforeEach(() => {
  sockets = [];
  const made = class {
    constructor() {
      const socket = new FakeSocket();
      sockets.push(socket);
      return socket as unknown as WebSocket;
    }
    static OPEN = 1;
    static CLOSING = 2;
  };
  (globalThis as { WebSocket?: unknown }).WebSocket = made;
});

function connect(): { state: Connection; socket: FakeSocket; net: ReturnType<typeof tunnel> } {
  const net = tunnel();
  let state: Connection | null = null;
  net.open('ws://proxy.invalid/tcp', (error, result) => {
    expect(error).toBeNull();
    state = result as Connection;
  });
  const socket = sockets[0];
  socket.say(OPENED);
  return { state: state as unknown as Connection, socket, net };
}

test.describe('the tunnel pacing the proxy', () => {
  test('holds the proxy off once the reader falls far enough behind', () => {
    const { socket } = connect();

    socket.arrive(200 * 1024);
    expect(socket.sent).toEqual([]);

    socket.arrive(100 * 1024);
    expect(socket.sent).toEqual([PAUSE]);
  });

  test('holds it off only once, however much more arrives', () => {
    const { socket } = connect();

    socket.arrive(300 * 1024);
    socket.arrive(300 * 1024);

    expect(socket.sent).toEqual([PAUSE]);
  });

  test('lets it go again once the reader has caught up', () => {
    const { state, socket, net } = connect();

    socket.arrive(300 * 1024);
    expect(socket.sent).toEqual([PAUSE]);

    while (state.queued > 64 * 1024) {
      net.receive(state, () => {});
    }
    net.receive(state, () => {});

    expect(socket.sent).toEqual([PAUSE, RESUME]);
  });

  test('answers a reader that asked before anything arrived', () => {
    const { state, socket, net } = connect();
    let got: unknown = 'nothing yet';
    net.receive(state, (_error, result) => {
      got = result;
    });

    expect(got).toBe('nothing yet');
    socket.arrive(8);
    expect(got).toBeInstanceOf(Int8Array);
  });

  test('tells a waiting reader the far end hung up', () => {
    const { state, socket, net } = connect();
    let error: string | null = 'not yet';
    let got: unknown = 'not yet';
    net.receive(state, (why, result) => {
      error = why;
      got = result;
    });

    socket.close();

    expect(error).toBeNull();
    expect(got).toBeNull();
    expect(net.isOpen(state)).toBe(false);
  });

  test('routes a failure to the reader once the socket is already open', () => {
    const { state, socket, net } = connect();
    let error: string | null = null;
    net.receive(state, (why) => {
      error = why;
    });

    socket.say('localhost:1 connection refused');

    expect(error).toBe('localhost:1 connection refused');
    expect(net.isOpen(state)).toBe(false);
  });

  test('routes a failure to the opener when nothing opened', () => {
    const net = tunnel();
    let error: string | null = null;
    net.open('ws://proxy.invalid/tcp', (why) => {
      error = why;
    });

    sockets[0].close();

    expect(error).toContain('no flagon tcp proxy answered');
  });

  test('hands the queued bytes back in the order they arrived', () => {
    const { state, socket, net } = connect();
    socket.arrive(4);
    socket.arrive(9);

    const sizes: number[] = [];
    net.receive(state, (_error, chunk) => sizes.push((chunk as Int8Array).length));
    net.receive(state, (_error, chunk) => sizes.push((chunk as Int8Array).length));

    expect(sizes).toEqual([4, 9]);
    expect(net.available(state)).toBe(0);
  });
});
