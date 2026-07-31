import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`booting a bundle on ${backend}`, () => {
    test('boots the runtime and reports every phase', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, '');

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
      await open(page, backend, 'program=hello');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('Hello, world!');
      await expect(log).toContainText('exit code 0');
      expect(failures).toEqual([]);
    });

    test('passes arguments through to the program', async ({ page }) => {
      await open(page, backend, 'program=hello&arg=flagon');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText('Hello, flagon!');
    });

    test('java semantics survive compilation to the browser', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=selftest&arg=alpha&arg=beta');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).not.toContainText('selftest failure');

      await expect(log).toContainText('arguments: [alpha,beta]');

      const summary = await log.textContent();
      const match = /selftest: (\d+)\/(\d+) checks passed/.exec(summary ?? '');
      expect(match, `no selftest summary in log:\n${summary}`).not.toBeNull();
      expect(match![1]).toBe(match![2]);
      expect(Number(match![2])).toBeGreaterThanOrEqual(80);
      expect(failures).toEqual([]);
    });

    test('reports an unknown program as a fatal boot failure', async ({ page }) => {
      await open(page, backend, 'program=does-not-exist');

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

      await page.goto(`/bare.html?backend=${backend}`);
      await expect
        .poll(() => lines.some((line) => line.includes('Hello, bare!')), { timeout: 30_000 })
        .toBe(true);

      expect(lines.some((line) => line.includes('no window.__flagonHost installed'))).toBe(true);
      expect(crashes).toEqual([]);
    });
  });
}

test('reports a backend that does not exist', async ({ page }) => {
  await page.goto('/?backend=asm');

  await expect(page.getByTestId('state')).toHaveText('failed');
  await expect(page.getByTestId('log')).toContainText("no backend named 'asm'; try js, wasm");
});
