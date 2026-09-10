import { test, expect, type Page } from '@playwright/test';
import http from 'node:http';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import type { AddressInfo } from 'node:net';
import { settingsFrom, startProxyServer } from '../proxy-server';
import { ruleOf } from '../tcp-proxy';
import { BULK_PROBE, BULK_PROBE_SIZE, FLOOD } from '../net-probes';

const VITE = 'ws://localhost:5273/__flagon/tcp?host=localhost&port=5273';

let elsewhere: http.Server;
let elsewhereUrl: string;

test.beforeAll(async () => {
  elsewhere = http.createServer((_asked, response) => {
    response.writeHead(200, { 'content-type': 'text/html' });
    response.end('<title>elsewhere</title>');
  });
  await new Promise<void>((listening) => elsewhere.listen(0, 'localhost', listening));
  elsewhereUrl = `http://localhost:${portOf(elsewhere)}/`;
});

test.afterAll(async () => {
  await new Promise<void>((closed) => elsewhere.close(() => closed()));
});

function portOf(server: http.Server): number {
  return (server.address() as AddressInfo).port;
}

function ask(page: Page, to: string): Promise<string> {
  return page.evaluate(
    (target) =>
      new Promise<string>((resolve) => {
        const socket = new WebSocket(target);
        socket.onmessage = (event) => {
          resolve(String(event.data));
          socket.close();
        };
        socket.onerror = () => resolve('refused');
        socket.onclose = () => resolve('refused');
      }),
    to,
  );
}

test.describe('the proxy and where the page asking for it came from', () => {
  test('opens a socket for the page it is serving', async ({ page }) => {
    await page.goto('/');

    expect(await ask(page, VITE)).toBe('open');
  });

  test('refuses a page served from another origin', async ({ page }) => {
    await page.goto(elsewhereUrl);

    expect(await ask(page, VITE)).toBe('refused');
  });
});

test.describe('the proxy a hosted build runs for itself', () => {
  let root: string;
  let server: http.Server;
  let site: string;
  let target: string;

  test.beforeAll(async () => {
    root = await fs.mkdtemp(path.join(os.tmpdir(), 'flagon-site-'));
    await fs.writeFile(path.join(root, 'index.html'), '<title>a built site</title>');
    await fs.mkdir(path.join(root, 'assets'));
    await fs.writeFile(path.join(root, 'assets', 'main-0a1b2c3d4e.js'), 'export {};');
    await fs.writeFile(path.join(root, 'pixel.png'), Buffer.alloc(64));
    server = await startProxyServer({
      host: 'localhost',
      port: 0,
      root,
      allow: ['localhost:*'],
      origins: [],
      token: '',
      hosts: [],
      probes: false,
    });
    site = `http://localhost:${portOf(server)}/`;
    target = `ws://localhost:${portOf(server)}/__flagon/tcp?host=localhost&port=${portOf(server)}`;
  });

  test.afterAll(async () => {
    await new Promise<void>((closed) => server.close(() => closed()));
    await fs.rm(root, { recursive: true, force: true });
  });

  test('serves the site it was pointed at', async ({ page }) => {
    await page.goto(site);

    await expect(page).toHaveTitle('a built site');
  });

  test('opens a socket with no vite anywhere', async ({ page }) => {
    await page.goto(site);

    expect(await ask(page, target)).toBe('open');
  });

  test('refuses a page served from another origin', async ({ page }) => {
    await page.goto(elsewhereUrl);

    expect(await ask(page, target)).toBe('refused');
  });

  test('compresses what it serves, and only what is worth compressing', async ({ request }) => {
    const page = await request.get(site, { headers: { 'accept-encoding': 'br' } });
    expect(page.headers()['content-encoding']).toBe('br');
    expect(page.headers()['vary']).toBe('accept-encoding');
    expect(await page.text()).toBe('<title>a built site</title>');

    const plain = await request.get(site, { headers: { 'accept-encoding': 'identity' } });
    expect(plain.headers()['content-encoding']).toBeUndefined();

    const image = await request.get(`${site}pixel.png`, { headers: { 'accept-encoding': 'br' } });
    expect(image.headers()['content-encoding']).toBeUndefined();
  });

  test('lets a cache keep what it has', async ({ request }) => {
    const first = await request.get(`${site}assets/main-0a1b2c3d4e.js`);
    expect(first.headers()['cache-control']).toBe('public, max-age=31536000, immutable');

    const page = await request.get(site);
    expect(page.headers()['cache-control']).toBe('no-cache');

    const again = await request.get(site, { headers: { 'if-none-match': page.headers()['etag'] } });
    expect(again.status()).toBe(304);
  });

  test('serves nothing from outside its root', async ({ request }) => {
    const answer = await request.get(`${site}../../../etc/hosts`);

    expect(answer.status()).toBe(404);
  });
});

test.describe('what the proxy will open a socket to', () => {
  test('refuses an allow entry that names no port', () => {
    expect(() => ruleOf('localhost')).toThrow(/needs a port/);
    expect(() => settingsFrom(['--allow', 'game.example.com'])).toThrow(/needs a port/);
    expect(ruleOf('[::1]:25565')).toEqual({ host: '[::1]', port: 25565 });
  });

  test('holds a scoped entry to its own port', async ({ page }) => {
    const server = await startProxyServer({
      host: 'localhost',
      port: 0,
      root: '.',
      allow: [`localhost:${portOf(elsewhere)}`],
      origins: [],
      token: '',
      hosts: [],
      probes: false,
    });
    const at = portOf(server);
    await page.goto(`http://localhost:${at}/`);

    const tcp = `ws://localhost:${at}/__flagon/tcp?host=localhost`;
    expect(await ask(page, `${tcp}&port=${portOf(elsewhere)}`)).toBe('open');
    expect(await ask(page, `${tcp}&port=${at}`)).toContain('not in this proxy');

    await new Promise<void>((closed) => server.close(() => closed()));
  });
});

test.describe('a proxy the network can reach', () => {
  test('will not bind past loopback without a token', () => {
    expect(() => settingsFrom(['--host', '0.0.0.0'])).toThrow(/--token/);
    expect(settingsFrom(['--host', '0.0.0.0', '--token', 'hunter2']).token).toBe('hunter2');
    expect(settingsFrom(['--host', '127.0.0.1']).token).toBe('');
  });

  test('grants the socket only to a page carrying the token', async ({ page }) => {
    const server = await startProxyServer({
      host: 'localhost',
      port: 0,
      root: '.',
      allow: ['localhost:*'],
      origins: [],
      token: 'hunter2',
      hosts: [],
      probes: false,
    });
    const at = portOf(server);
    await page.goto(`http://localhost:${at}/`);

    const tcp = `ws://localhost:${at}/__flagon/tcp?host=localhost&port=${at}`;
    expect(await ask(page, tcp)).toBe('refused');
    expect(await ask(page, `${tcp}&token=wrong`)).toBe('refused');
    expect(await ask(page, `${tcp}&token=hunter2`)).toBe('open');

    await new Promise<void>((closed) => server.close(() => closed()));
  });
});

async function probeOn(probes: boolean, route: string): Promise<number> {
  const server = await startProxyServer({
    host: 'localhost',
    port: 0,
    root: '.',
    allow: ['localhost:*'],
    origins: [],
    token: '',
    hosts: [],
    probes,
  });
  const at = (server.address() as AddressInfo).port;
  const answer = await fetch(`http://localhost:${at}${route}`);
  const status = answer.ok ? (await answer.arrayBuffer()).byteLength : answer.status;
  await new Promise<void>((closed) => server.close(() => closed()));
  return status;
}

test.describe('the net fixtures on the server that ships', () => {
  test('serves the bulk body only when it was asked to', async () => {
    expect(await probeOn(true, BULK_PROBE)).toBe(BULK_PROBE_SIZE);
    expect(await probeOn(false, BULK_PROBE)).toBe(404);
  });

  test('takes a flood back and says how much of it arrived', async () => {
    expect(await probeOn(false, FLOOD)).toBe(404);
    expect(settingsFrom(['--probes']).probes).toBe(true);
    expect(settingsFrom([]).probes).toBe(false);
  });
});

function upgradeAs(at: number, host: string): Promise<number> {
  return new Promise((resolve) => {
    const asked = http.request({
      host: 'localhost',
      port: at,
      path: `${'/__flagon/tcp'}?host=localhost&port=${at}`,
      headers: {
        host,
        origin: `http://${host}`,
        connection: 'Upgrade',
        upgrade: 'websocket',
        'sec-websocket-version': '13',
        'sec-websocket-key': Buffer.alloc(16).toString('base64'),
      },
    });
    asked.on('upgrade', (response, socket) => {
      socket.destroy();
      resolve(response.statusCode ?? 101);
    });
    asked.on('response', (response) => {
      response.resume();
      resolve(response.statusCode ?? 0);
    });
    asked.on('error', () => resolve(0));
    asked.end();
  });
}

test.describe('the proxy and the name it was reached by', () => {
  let server: http.Server;

  test.beforeAll(async () => {
    server = await startProxyServer({
      host: 'localhost',
      port: 0,
      root: '.',
      allow: ['localhost:*'],
      origins: [],
      token: '',
      hosts: ['game.example.com'],
      probes: false,
    });
  });

  test.afterAll(async () => {
    await new Promise<void>((closed) => server.close(() => closed()));
  });

  test('refuses a name it was never told to answer to', async () => {
    const at = portOf(server);

    expect(await upgradeAs(at, `attacker.example:${at}`)).toBe(403);
    expect(await upgradeAs(at, `localhost:${at}`)).toBe(101);
    expect(await upgradeAs(at, `game.example.com:${at}`)).toBe(101);
  });

  test('answers the loopback addresses whatever the port', async () => {
    const at = portOf(server);

    expect(await upgradeAs(at, `127.0.0.1:${at}`)).toBe(101);
    expect(await upgradeAs(at, `[::1]:${at}`)).toBe(101);
  });

  test('says what the pages it serves are allowed to reach', async () => {
    const answered = await fetch(`http://localhost:${portOf(server)}/`).catch(() => null);
    const policy = answered?.headers.get('content-security-policy') ?? '';

    expect(policy).toContain("default-src 'self'");
    expect(policy).toContain("object-src 'none'");
    expect(policy).toContain("frame-ancestors 'none'");
    expect(answered?.headers.get('x-content-type-options')).toBe('nosniff');
  });
});
