import { test, expect, type Page } from '@playwright/test';
import http from 'node:http';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import type { AddressInfo } from 'node:net';
import { settingsFrom, startProxyServer } from '../proxy-server';
import { ruleOf } from '../tcp-proxy';

const VITE = 'ws://localhost:5273/__flagon/tcp?host=localhost&port=5273';

// a page on a port of its own: another origin, still on the loopback the browser will talk to
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
    server = await startProxyServer({
      host: 'localhost',
      port: 0,
      root,
      allow: ['localhost:*'],
      origins: [],
      token: '',
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
