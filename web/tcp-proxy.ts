import net from 'node:net';
import type { IncomingMessage } from 'node:http';
import type { Duplex } from 'node:stream';
import type { Plugin, PreviewServer, ViteDevServer } from 'vite';
import { WebSocketServer, type WebSocket } from 'ws';

export const TCP_PROXY_PATH = '/__flagon/tcp';

export interface TcpProxyOptions {
  /** hosts connections may be made to, as `host` or `host:port` */
  allow?: string[];
}

/**
 * a socket the page cannot open, opened on its behalf
 */
export function tcpProxy(options: TcpProxyOptions = {}): Plugin {
  const allowed = (options.allow ?? ['localhost', '127.0.0.1', '[::1]']).map((one) =>
    one.toLowerCase(),
  );

  const attach = (server: ViteDevServer | PreviewServer): void => {
    const sockets = new WebSocketServer({ noServer: true });
    server.httpServer?.on('upgrade', (request: IncomingMessage, socket: Duplex, head: Buffer) => {
      const asked = new URL(request.url ?? '/', 'http://proxy.invalid');
      // vite's own hot-reload socket comes through here too, and is none of our business
      if (asked.pathname !== TCP_PROXY_PATH) {
        return;
      }
      // browsers do not apply the same-origin policy to websockets, so the proxy applies it here
      if (!isSameOrigin(request)) {
        socket.write('HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n');
        socket.destroy();
        return;
      }
      sockets.handleUpgrade(request, socket, head, (client) => open(client, asked, allowed));
    });
  };

  return {
    name: 'flagon-tcp-proxy',
    configureServer: attach,
    configurePreviewServer: attach,
  };
}

function isSameOrigin(request: IncomingMessage): boolean {
  const origin = request.headers.origin;
  const host = request.headers.host;
  if (origin === undefined || host === undefined) {
    return false;
  }
  try {
    return new URL(origin).host.toLowerCase() === host.toLowerCase();
  } catch {
    return false;
  }
}

function open(client: WebSocket, asked: URL, allowed: string[]): void {
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
  socket.on('connect', () => client.send('open'));
  socket.on('data', (data: Buffer) => {
    if (client.readyState === client.OPEN) {
      client.send(data);
    }
  });
  socket.on('error', (error: Error) => refuse(client, `${host}:${port} ${error.message}`));
  socket.on('close', () => client.close());

  client.on('message', (data: Buffer) => socket.write(data));
  client.on('close', () => socket.destroy());
}

function refuse(client: WebSocket, reason: string): void {
  if (client.readyState === client.OPEN) {
    client.send(reason);
    client.close();
  }
}

function isAllowed(host: string, port: number, allowed: string[]): boolean {
  const asked = host.toLowerCase();
  return allowed.some((one) => {
    const at = one.lastIndexOf(':');
    if (at < 0) {
      return one === asked;
    }
    return one.slice(0, at) === asked && Number(one.slice(at + 1)) === port;
  });
}
