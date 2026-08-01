import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, open, watchForFailures, type Backend } from './harness';

async function runDemo(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=net&arg=localhost:5273');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('net: done');
}

for (const backend of BACKENDS) {
  test.describe(`sockets through a proxy on ${backend}`, () => {
    test('opens a connection the browser has no way to open itself', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('network: websocket proxy');
      await expect(page.getByTestId('log')).toContainText('connected to localhost:5273');
      expect(failures).toEqual([]);
    });

    test('sends a request and reads the answer back', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('status: HTTP/1.1 200 OK');
      await expect(page.getByTestId('log')).toContainText(
        'body: flagon reached this server through a socket it could not open',
      );
      await expect(page.getByTestId('log')).toContainText('closed: true');
    });

    test('reassembles a body that arrives as many frames', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('bulk: 262144 bytes in order');
    });

    test('answers an unmodified java.net.URL over the same proxy', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('url type: text/plain');
      await expect(page.getByTestId('log')).toContainText(
        'url body: flagon reached this server through a socket it could not open',
      );
    });

    /** a body nobody is reading stops arriving, and starts again where it left off */
    test('holds the proxy off while the reader is behind', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('capped: true');
      await expect(page.getByTestId('log')).toContainText('paced: sending resumed: true');
    });

    /** the other direction: a write larger than the browser will hold loses nothing */
    test('waits for a large write to go out', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('paced: whole write arrived: true');
    });

    test('reports a port with nothing behind it rather than waiting', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('refused: localhost:1');
    });

    test('refuses a host the proxy was not told to allow', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText(
        "not allowed: example.com:80 is not in this proxy's allow list",
      );
    });
  });
}

test('says what a page without a proxy is missing', async ({ page }) => {
  await open(page, 'js', 'program=hello');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

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
