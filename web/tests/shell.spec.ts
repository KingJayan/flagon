import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`the shell's program picker on ${backend}`, () => {
    test('lists the programs the bundle actually registered, with their descriptions', async ({
      page,
    }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, '');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const options = page.locator('#program option');
      await expect(options.first()).toHaveAttribute('value', '');
      await expect(options.nth(1)).toHaveAttribute('value', 'hello');
      await expect(options.nth(1)).toContainText('hello — ');
      await expect(options).toHaveCount(12);
      expect(failures).toEqual([]);
    });

    test('switching bundle forgets a program the new bundle does not have', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=hello');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

      await page.locator('#bundle').selectOption('example');
      await expect(page.locator('#program')).toHaveValue('');
      await expect(page.locator('#program option')).toHaveCount(1);

      await page.locator('#run').click();
      await expect(page.getByTestId('phases').locator('li')).toHaveText(
        ['platform: browser', 'select: spinner', 'program: spinner'],
        { timeout: 30_000 },
      );
      expect(failures).toEqual([]);
    });

    test('stops a running program without reloading the page', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=lwjgl');

      const stop = page.locator('#stop');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
      await expect(stop).toBeEnabled();

      await stop.click();
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText('exit code 0');
      await expect(stop).toBeDisabled();
      expect(failures).toEqual([]);
    });

    test('keeps the previous run in the console when a new one starts', async ({ page }) => {
      await open(page, backend, 'program=hello');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

      await page.locator('#run').click();
      const log = page.getByTestId('log');
      await expect(log.locator('.past').last()).toHaveText('— previous run above —\n');
      await expect(log.locator('.past', { hasText: 'Hello, world!' })).toHaveCount(1);
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    });

    test('remembers a bundle it has already booted', async ({ page }) => {
      await open(page, backend, 'bundle=example');
      await expect(page.getByTestId('phases').locator('li')).toHaveText(
        ['platform: browser', 'select: spinner', 'program: spinner'],
        { timeout: 30_000 },
      );

      await page.locator('#bundle').selectOption('sandbox');
      await expect(page.locator('#program option')).toHaveCount(1);
      await page.locator('#bundle').selectOption('example');
      await expect(page.locator('#program option')).toHaveCount(3);
      await expect(page.locator('#program option').nth(1)).toContainText('spinner — ');
    });
  });
}
