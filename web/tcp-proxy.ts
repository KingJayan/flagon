import net from 'node:net';
import { timingSafeEqual } from 'node:crypto';
import type { IncomingMessage } from 'node:http';
import type { Duplex } from 'node:stream';
import type { Plugin, PreviewServer, ViteDevServer } from 'vite';
import { WebSocketServer, type WebSocket } from 'ws';

export const TCP_PROXY_PATH = '/__flagon/tcp';

export const DEFAULT_ALLOW = ['localhost:*', '127.0.0.1:*', '[::1]:*'];

export interface TcpProxyOptions {
  allow?: string[];
  origins?: string[];
  token?: string;
  hosts?: string[];
}

const LOOPBACK = new Set(['localhost', '127.0.0.1', '::1']);

export function hostnameOf(header: string): string {
  const bare = header.toLowerCase().trim();
  const bracketed = /^\[([^\]]+)\](?::\d+)?$/.exec(bare);
  if (bracketed) {
    return bracketed[1];
  }
  return bare.replace(/:\d+$/, '');
}

export function isLoopback(host: string): boolean {
  const name = hostnameOf(host);
  return LOOPBACK.has(name) || name.startsWith('127.');
}

interface Rule {
  host: string;
  port: number | undefined;
}

const RULE = /^(\[[^\]]+\]|[^:]+):(\*|\d{1,5})$/;

export function ruleOf(entry: string): Rule {
  const parts = RULE.exec(entry.toLowerCase());
  if (!parts) {
    throw new Error(`${entry} needs a port: host:port, or host:* for any`);
  }
  const port = parts[2] === '*' ? undefined : Number(parts[2]);
  if (port !== undefined && (port <= 0 || port > 65535)) {
    throw new Error(`${entry} is not a port`);
  }
  return { host: parts[1], port };
}


export interface Upgradable {
  on(
    event: 'upgrade',
    listener: (request: IncomingMessage, socket: Duplex, head: Buffer) => void,
  ): unknown;
}

export function attachTcpProxy(server: Upgradable, options: TcpProxyOptions = {}): void {
  const allowed = (options.allow ?? DEFAULT_ALLOW).map(ruleOf);
  const origins = (options.origins ?? []).map((one) => one.toLowerCase());
  const hosts = (options.hosts ?? []).map(hostnameOf);
  const token = options.token ?? '';
  const sockets = new WebSocketServer({ noServer: true });

  server.on('upgrade', (request: IncomingMessage, socket: Duplex, head: Buffer) => {
    const asked = new URL(request.url ?? '/', 'http://proxy.invalid');
    if (asked.pathname !== TCP_PROXY_PATH) {
      return;
    }
    if (!isOurs(request, hosts) || !isAsked(request, origins) || !isTold(asked, token)) {
      socket.write('HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n');
      socket.destroy();
      return;
    }
    sockets.handleUpgrade(request, socket, head, (client) => open(client, asked, allowed));
  });
}

/**
 * a socket the page cannot open, opened on its behalf
 */
export function tcpProxy(options: TcpProxyOptions = {}): Plugin {
  const attach = (server: ViteDevServer | PreviewServer): void => {
    if (server.httpServer) {
      attachTcpProxy(server.httpServer, options);
    }
  };

  return {
    name: 'flagon-tcp-proxy',
    configureServer: attach,
    configurePreviewServer: attach,
  };
}

function isOurs(request: IncomingMessage, hosts: string[]): boolean {
  const host = request.headers.host;
  if (host === undefined) {
    return false;
  }
  return isLoopback(host) || hosts.includes(hostnameOf(host));
}

function isAsked(request: IncomingMessage, origins: string[]): boolean {
  const origin = request.headers.origin;
  const host = request.headers.host;
  if (origin === undefined || host === undefined) {
    return false;
  }
  if (origins.includes(origin.toLowerCase())) {
    return true;
  }
  try {
    return new URL(origin).host.toLowerCase() === host.toLowerCase();
  } catch {
    return false;
  }
}

function isTold(asked: URL, token: string): boolean {
  if (token === '') {
    return true;
  }
  const given = Buffer.from(asked.searchParams.get('token') ?? '');
  const wanted = Buffer.from(token);
  return given.length === wanted.length && timingSafeEqual(given, wanted);
}

function open(client: WebSocket, asked: URL, allowed: Rule[]): void {
  const host = asked.searchParams.get('host') ?? '';
  const port = Number(asked.searchParams.get('port') ?? '0');

  if (!host || !Number.isInteger(port) || port <= 0 || port > 65535) {
    refuse(client, `${host}:${port} is not an address`);
    return;
  }
  if (!isAllowed(host, port, allowed)) {
    refuse(client, `${host}:${port} is not in this proxy's allow list`);
    return;
  }

  const socket = net.createConnection({ host, port });

  let flushing = false;
  let held = false;
  const pace = (): void => {
    if (flushing || held) {
      socket.pause();
    } else {
      socket.resume();
    }
  };

  socket.on('connect', () => client.send('open'));
  socket.on('data', (data: Buffer) => {
    if (client.readyState !== client.OPEN) {
      return;
    }
    flushing = true;
    pace();
    client.send(data, () => {
      flushing = false;
      pace();
    });
  });
  socket.on('error', (error: Error) => refuse(client, `${host}:${port} ${error.message}`));
  socket.on('close', () => client.close());

  // text frames are the page pacing us; everything else is the socket's own bytes
  client.on('message', (data: Buffer, isBinary: boolean) => {
    if (!isBinary) {
      held = data.toString() === 'pause';
      pace();
      return;
    }
    if (!socket.write(data)) {
      hold(client, true);
    }
  });
  socket.on('drain', () => hold(client, false));
  client.on('close', () => socket.destroy());
}

/**
 * stops reading the page while the socket catches up
 *
 * <p>ws pauses the socket under it; runtimes that answer the same api without that method are
 * given the socket directly, so the shipping server backs off the same way vite's does.
 */
function hold(client: WebSocket, holding: boolean): void {
  const stream = (client as unknown as { _socket?: NodeJS.ReadWriteStream })._socket;
  const paced = typeof client.pause === 'function' ? client : stream;
  if (holding) {
    paced?.pause();
  } else {
    paced?.resume();
  }
}

function refuse(client: WebSocket, reason: string): void {
  if (client.readyState === client.OPEN) {
    client.send(reason);
    client.close();
  }
}

function isAllowed(host: string, port: number, allowed: Rule[]): boolean {
  const asked = host.toLowerCase();
  return allowed.some((one) => one.host === asked && (one.port === undefined || one.port === port));
}
