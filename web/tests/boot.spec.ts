import { test, expect, type ConsoleMessage, type Page } from '@playwright/test';

/** collects anything the browser itself considers an error. */
function watchForFailures(page: Page): { failures: string[] } {
  const failures: string[] = [];
  page.on('console', (message: ConsoleMessage) => {
    if (message.type() === 'error') {
      failures.push(`console.error: ${message.text()}`);
    }
  });
  page.on('pageerror', (error) => failures.push(`pageerror: ${error.message}`));
  return { failures };
}

test('boots the runtime and reports every phase', async ({ page }) => {
  const { failures } = watchForFailures(page);
  await page.goto('/');

  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('phases').locator('li')).toHaveText([
    'platform: browser',
    'select: hello',
    'program: hello',
  ]);
  expect(failures).toEqual([]);
});

test('runs a simple java program', async ({ page }) => {
  const { failures } = watchForFailures(page);
  await page.goto('/?program=hello');

  const log = page.getByTestId('log');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(log).toContainText('Hello, world!');
  await expect(log).toContainText('exit code 0');
  expect(failures).toEqual([]);
});

test('passes arguments through to the program', async ({ page }) => {
  await page.goto('/?program=hello&arg=flagon');

  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('Hello, flagon!');
});

test('java semantics survive compilation to the browser', async ({ page }) => {
  const { failures } = watchForFailures(page);
  await page.goto('/?program=selftest');

  const log = page.getByTestId('log');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(log).not.toContainText('selftest failure');

  const summary = await log.textContent();
  const match = /selftest: (\d+)\/(\d+) checks passed/.exec(summary ?? '');
  expect(match, `no selftest summary in log:\n${summary}`).not.toBeNull();
  expect(match![1]).toBe(match![2]);
  expect(Number(match![2])).toBeGreaterThan(50);
  expect(failures).toEqual([]);
});

test('reports an unknown program as a fatal boot failure', async ({ page }) => {
  await page.goto('/?program=does-not-exist');

  await expect(page.getByTestId('state')).toHaveText('failed', { timeout: 30_000 });
  const log = page.getByTestId('log');
  await expect(log).toContainText('fatal in select');
  await expect(log).toContainText("unknown program 'does-not-exist'");
  await expect(log).toContainText('available: hello, selftest');
});

test('still runs when the page installs no host bridge', async ({ page }) => {
  const lines: string[] = [];
  page.on('console', (message) => lines.push(`${message.type()}:${message.text()}`));
  const crashes: string[] = [];
  page.on('pageerror', (error) => crashes.push(error.message));

  await page.goto('/bare.html');
  await expect
    .poll(() => lines.some((line) => line.includes('Hello, bare!')), { timeout: 30_000 })
    .toBe(true);

  expect(lines.some((line) => line.includes('no window.__flagonHost installed'))).toBe(true);
  expect(crashes).toEqual([]);
});
