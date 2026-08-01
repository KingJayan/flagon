import { test, expect, type Page } from '@playwright/test';
import http from 'node:http';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import type { AddressInfo } from 'node:net';
import { startProxyServer } from '../proxy-server';

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
      allow: ['localhost'],
      origins: [],
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
