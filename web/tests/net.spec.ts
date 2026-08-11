import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, open, watchForFailures, type Backend } from './harness';

/** the demo talks to the server the page came from, whichever server that is */
function hostOf(baseURL: string | undefined): string {
  return new URL(baseURL ?? 'http://localhost:5273').host;
}

async function runDemo(page: Page, backend: Backend, host: string): Promise<void> {
  await open(page, backend, `program=net&arg=${host}`);
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
  await expect(page.getByTestId('log')).toContainText('net: done');
}

for (const backend of BACKENDS) {
  test.describe(`sockets through a proxy on ${backend}`, () => {
    test('opens a connection the browser has no way to open itself', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      const { failures } = watchForFailures(page);

      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText('network: websocket proxy');
      await expect(page.getByTestId('log')).toContainText(`connected to ${host}`);
      expect(failures).toEqual([]);
    });

    test('sends a request and reads the answer back', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText('status: HTTP/1.1 200 OK');
      await expect(page.getByTestId('log')).toContainText(
        'body: flagon reached this server through a socket it could not open',
      );
      await expect(page.getByTestId('log')).toContainText('closed: true');
    });

    /** the socket a desktop program opens, on a page that has none of its own */
    test('carries an unmodified java.net.Socket through the same proxy', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText(
        `socket: connected true to ${host}`,
      );
      await expect(page.getByTestId('log')).toContainText(
        'socket body: flagon reached this server through a socket it could not open',
      );
      await expect(page.getByTestId('log')).toContainText('socket timeout kept: 30000');
    });

    test('refuses to listen, which a page can never do', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText(
        'socket listen refused: nothing can connect in to a page, so it cannot listen',
      );
    });

    test('reassembles a body that arrives as many frames', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText('bulk: 262144 bytes in order');
    });

    test('answers an unmodified java.net.URL over the same proxy', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText('url type: text/plain');
      await expect(page.getByTestId('log')).toContainText(
        'url body: flagon reached this server through a socket it could not open',
      );
    });

    /**
     a body nobody is reading stops arriving, and starts again where it left off. a host that
     buffers the whole flood never has to hold back, so only the settling is asserted here
     */
    test('holds the proxy off while the reader is behind', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText('capped: true');
      await expect(page.getByTestId('log')).toContainText('paced: sending resumed: true');
    });

    /** the other direction: a write larger than the browser will hold loses nothing */
    test('waits for a large write to go out', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText('paced: whole write arrived: true');
    });

    test('reports a port with nothing behind it rather than waiting', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText('refused: localhost:1');
    });

    test('refuses a host the proxy was not told to allow', async ({ page, baseURL }) => {
      const host = hostOf(baseURL);
      await runDemo(page, backend, host);

      await expect(page.getByTestId('log')).toContainText(
        "not allowed: example.com:80 is not in this proxy's allow list",
      );
    });
  });
}

test('carries a proxy token from the page url onto the socket', async ({ page, baseURL }) => {
  const host = hostOf(baseURL);
  const opened: string[] = [];
  page.on('websocket', (socket) => opened.push(socket.url()));

  await open(page, 'js', `program=net&arg=${host}&proxytoken=hunter2`);
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

  await expect(page.getByTestId('log')).toContainText(`connected to ${host}`);
  const sockets = opened.filter((url) => url.includes('/__flagon/tcp'));
  expect(sockets.length).toBeGreaterThan(0);
  expect(sockets.every((url) => url.includes('token=hunter2'))).toBe(true);
});

test('says what a page without a proxy is missing', async ({ page }) => {
  await open(page, 'js', 'program=hello');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

  const failure = await page.evaluate(
    () =>
      new Promise<string>((resolve) => {
        const net = (window as unknown as { __flagonNet: { open(url: string, done: Function): void } })
          .__flagonNet;
        net.open('ws://localhost:65533/__flagon/tcp', (error: string) =>
          resolve(String(error)),
        );
      }),
  );

  expect(failure).toContain('flagon tcp proxy');
});
