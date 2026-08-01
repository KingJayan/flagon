import { test, expect, type Page } from '@playwright/test';
import http from 'node:http';
import type { AddressInfo } from 'node:net';

const TARGET = 'ws://localhost:5273/__flagon/tcp?host=localhost&port=5273';

// a page on a port of its own: another origin, still on the loopback the browser will talk to
let elsewhere: http.Server;
let elsewhereUrl: string;

test.beforeAll(async () => {
  elsewhere = http.createServer((_asked, response) => {
    response.writeHead(200, { 'content-type': 'text/html' });
    response.end('<title>elsewhere</title>');
  });
  await new Promise<void>((listening) => elsewhere.listen(0, 'localhost', listening));
  elsewhereUrl = `http://localhost:${(elsewhere.address() as AddressInfo).port}/`;
});

test.afterAll(async () => {
  await new Promise<void>((closed) => elsewhere.close(() => closed()));
});

function ask(page: Page): Promise<string> {
  return page.evaluate(
    (to) =>
      new Promise<string>((resolve) => {
        const socket = new WebSocket(to);
        socket.onmessage = (event) => {
          resolve(String(event.data));
          socket.close();
        };
        socket.onerror = () => resolve('refused');
        socket.onclose = () => resolve('refused');
      }),
    TARGET,
  );
}

test.describe('the proxy and where the page asking for it came from', () => {
  test('opens a socket for the page it is serving', async ({ page }) => {
    await page.goto('/');

    expect(await ask(page)).toBe('open');
  });

  test('refuses a page served from another origin', async ({ page }) => {
    await page.goto(elsewhereUrl);

    expect(await ask(page)).toBe('refused');
  });
});
