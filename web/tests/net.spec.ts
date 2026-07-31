import { test, expect, type Page } from '@playwright/test';

async function runDemo(page: Page): Promise<void> {
  await page.goto('/?program=net&arg=localhost:5273');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('net: done');
}

test.describe('sockets through a proxy', () => {
  test('opens a connection the browser has no way to open itself', async ({ page }) => {
    const failures: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        failures.push(message.text());
      }
    });
    page.on('pageerror', (error) => failures.push(error.message));

    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('network: websocket proxy');
    await expect(page.getByTestId('log')).toContainText('connected to localhost:5273');
    expect(failures).toEqual([]);
  });

  test('sends a request and reads the answer back', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('status: HTTP/1.1 200 OK');
    await expect(page.getByTestId('log')).toContainText(
      'body: flagon reached this server through a socket it could not open',
    );
    await expect(page.getByTestId('log')).toContainText('closed: true');
  });

  test('reports a port with nothing behind it rather than waiting', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('refused: localhost:1');
  });

  test('refuses a host the proxy was not told to allow', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText(
      "not allowed: example.com:80 is not in this proxy's allow list",
    );
  });
});
