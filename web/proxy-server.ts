import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { attachTcpProxy, DEFAULT_ALLOW, TCP_PROXY_PATH } from './tcp-proxy';

/**
 * the proxy a built site needs, with nothing of vite in it.
 *
 * <p>it serves the site as well as the sockets on purpose: the upgrade is only granted to the
 * origin it came from, so a page and its proxy have to be the same origin unless `--origin`
 * names another one.
 */

const TYPES: Record<string, string> = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.wasm': 'application/wasm',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
};

export interface Settings {
  host: string;
  port: number;
  root: string;
  allow: string[];
  origins: string[];
}

export function settingsFrom(argv: string[]): Settings {
  const settings: Settings = {
    host: 'localhost',
    port: 8080,
    root: 'dist',
    allow: [...DEFAULT_ALLOW],
    origins: [],
  };
  const many: Record<string, string[]> = { '--allow': [], '--origin': [] };

  for (let at = 0; at < argv.length; at += 2) {
    const name = argv[at];
    const value = argv[at + 1];
    if (value === undefined) {
      throw new Error(`${name} needs a value`);
    }
    if (name === '--host') {
      settings.host = value;
    } else if (name === '--port') {
      settings.port = Number(value);
    } else if (name === '--root') {
      settings.root = value;
    } else if (name in many) {
      many[name].push(...value.split(',').map((one) => one.trim()));
    } else {
      throw new Error(`no option named ${name}`);
    }
  }

  if (many['--allow'].length > 0) {
    settings.allow = many['--allow'];
  }
  settings.origins = many['--origin'];
  return settings;
}

async function send(response: http.ServerResponse, file: string): Promise<void> {
  const body = await fs.readFile(file);
  response.writeHead(200, {
    'content-type': TYPES[path.extname(file)] ?? 'application/octet-stream',
    'content-length': body.length,
  });
  response.end(body);
}

/** files under the root and nothing else, whatever the url says */
async function serve(root: string, request: http.IncomingMessage, response: http.ServerResponse) {
  const asked = new URL(request.url ?? '/', 'http://site.invalid');
  const wanted = path.join(root, path.normalize(asked.pathname));
  if (wanted !== root && !wanted.startsWith(root + path.sep)) {
    response.writeHead(403).end('forbidden');
    return;
  }
  try {
    const found = (await fs.stat(wanted)).isDirectory() ? path.join(wanted, 'index.html') : wanted;
    await send(response, found);
  } catch {
    response.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
    response.end(`nothing here: ${asked.pathname}`);
  }
}

export async function startProxyServer(settings: Settings): Promise<http.Server> {
  const root = path.resolve(settings.root);
  const server = http.createServer((request, response) => {
    void serve(root, request, response);
  });
  attachTcpProxy(server, { allow: settings.allow, origins: settings.origins });
  await new Promise<void>((listening) => server.listen(settings.port, settings.host, listening));
  return server;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const settings = settingsFrom(process.argv.slice(2));
  const server = await startProxyServer(settings);
  const address = server.address();
  const port = typeof address === 'object' && address !== null ? address.port : settings.port;
  console.log(`flagon: serving ${path.resolve(settings.root)} on http://${settings.host}:${port}`);
  console.log(`flagon: sockets at ${TCP_PROXY_PATH} to ${settings.allow.join(', ')}`);
}
