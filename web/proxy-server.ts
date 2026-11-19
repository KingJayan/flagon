import http from 'node:http';
import fs from 'node:fs/promises';
import path from 'node:path';
import zlib from 'node:zlib';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';
import {
  attachTcpProxy,
  DEFAULT_ALLOW,
  isLoopback,
  ruleOf,
  TCP_PROXY_PATH,
} from './tcp-proxy';
import { answerNetProbe } from './net-probes';


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
  token: string;
  hosts: string[];
  probes: boolean;
  tlsCa: string;
}

export { isLoopback };

export function settingsFrom(argv: string[]): Settings {
  const settings: Settings = {
    host: '127.0.0.1',
    port: 8080,
    root: 'dist',
    allow: [...DEFAULT_ALLOW],
    origins: [],
    token: '',
    hosts: [],
    probes: false,
    tlsCa: '',
  };
  const many: Record<string, string[]> = { '--allow': [], '--origin': [], '--allowed-host': [] };

  for (let at = 0; at < argv.length; at += 2) {
    const name = argv[at];
    if (name === '--probes') {
      settings.probes = true;
      at -= 1;
      continue;
    }
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
    } else if (name === '--token') {
      settings.token = value;
    } else if (name === '--tls-ca') {
      settings.tlsCa = value;
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
  const bound = ['0.0.0.0', '::', '[::]'].includes(settings.host) ? [] : [settings.host];
  settings.hosts = [...many['--allowed-host'], ...bound];
  settings.allow.forEach(ruleOf);
  if (!isLoopback(settings.host) && settings.token === '') {
    throw new Error(
      `--host ${settings.host} reaches this proxy from the network, where the origin check ` +
        'means nothing. give it a --token, or bind it to loopback.',
    );
  }
  return settings;
}

const brotli = promisify(zlib.brotliCompress);
const gzip = promisify(zlib.gzip);

const COMPRESSIBLE = new Set(['.html', '.js', '.css', '.json', '.map', '.wasm', '.svg', '.txt']);
const HASHED = /-[A-Za-z0-9_-]{8,}\.[A-Za-z0-9]+$/;

const packed = new Map<string, { tag: string; body: Buffer }>();

async function compressed(file: string, tag: string, how: 'br' | 'gzip', raw: Buffer) {
  const key = `${how} ${file}`;
  const already = packed.get(key);
  if (already?.tag === tag) {
    return already.body;
  }
  const body =
    how === 'br'
      ? await brotli(raw, { params: { [zlib.constants.BROTLI_PARAM_QUALITY]: 5 } })
      : await gzip(raw, { level: 6 });
  packed.set(key, { tag, body });
  return body;
}

function encodingFor(request: http.IncomingMessage, file: string): 'br' | 'gzip' | null {
  if (!COMPRESSIBLE.has(path.extname(file))) {
    return null;
  }
  const accepted = String(request.headers['accept-encoding'] ?? '');
  if (/\bbr\b/.test(accepted)) {
    return 'br';
  }
  return /\bgzip\b/.test(accepted) ? 'gzip' : null;
}

const POLICY = [
  "default-src 'self'",
  "script-src 'self' 'wasm-unsafe-eval'",
  "style-src 'self'",
  "img-src 'self' data: blob:",
  "media-src 'self' data: blob:",
  "connect-src 'self'",
  "worker-src 'self' blob:",
  "object-src 'none'",
  "base-uri 'none'",
  "form-action 'none'",
  "frame-ancestors 'none'",
].join('; ');

const GUARDS: http.OutgoingHttpHeaders = {
  'content-security-policy': POLICY,
  'x-content-type-options': 'nosniff',
  'referrer-policy': 'no-referrer',
  'cross-origin-opener-policy': 'same-origin',
};

async function send(
  request: http.IncomingMessage,
  response: http.ServerResponse,
  file: string,
): Promise<void> {
  const stat = await fs.stat(file);
  const tag = `"${stat.size.toString(16)}-${Math.floor(stat.mtimeMs).toString(16)}"`;
  const headers: http.OutgoingHttpHeaders = {
    'content-type': TYPES[path.extname(file)] ?? 'application/octet-stream',
    'cache-control': HASHED.test(path.basename(file))
      ? 'public, max-age=31536000, immutable'
      : 'no-cache',
    etag: tag,
    vary: 'accept-encoding',
    ...GUARDS,
  };

  if (request.headers['if-none-match'] === tag) {
    response.writeHead(304, headers).end();
    return;
  }

  const raw = await fs.readFile(file);
  const how = encodingFor(request, file);
  const body = how === null ? raw : await compressed(file, tag, how, raw);
  if (how !== null) {
    headers['content-encoding'] = how;
  }
  response.writeHead(200, { ...headers, 'content-length': body.length });
  response.end(body);
}

async function serve(root: string, request: http.IncomingMessage, response: http.ServerResponse) {
  const asked = new URL(request.url ?? '/', 'http://site.invalid');
  const wanted = path.join(root, path.normalize(asked.pathname));
  if (wanted !== root && !wanted.startsWith(root + path.sep)) {
    response.writeHead(403).end('forbidden');
    return;
  }
  try {
    const found = (await fs.stat(wanted)).isDirectory() ? path.join(wanted, 'index.html') : wanted;
    await send(request, response, found);
  } catch {
    response.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
    response.end(`nothing here: ${asked.pathname}`);
  }
}

export async function startProxyServer(settings: Settings): Promise<http.Server> {
  const root = path.resolve(settings.root);
  const server = http.createServer((request, response) => {

    request.on('error', () => {});
    response.on('error', () => {});
    if (/close/i.test(String(request.headers.connection ?? ''))) {
      response.setHeader('connection', 'close');
      response.on('finish', () => request.socket.end());
    }
    if (settings.probes && answerNetProbe(request, response)) {
      return;
    }
    void serve(root, request, response);
  });
  attachTcpProxy(server, {
    tlsCa: settings.tlsCa === '' ? '' : await fs.readFile(settings.tlsCa, 'utf8'),
    allow: settings.allow,
    origins: settings.origins,
    token: settings.token,
    hosts: settings.hosts,
  });
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
  if (settings.probes) {
    console.log('flagon: serving the net test fixtures too');
  }
  if (settings.token !== '') {
    console.log('flagon: the socket wants ?proxytoken= on the page url');
  }
}
