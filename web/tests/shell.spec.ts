// needs a display
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
      await expect(options).toHaveCount(20);
      expect(failures).toEqual([]);
    });

    test('switching bundle forgets a program the new bundle does not have', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=hello');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

      await page.locator('#bundle').selectOption('example');
      await expect(page.locator('#program')).toHaveValue('');
      await expect(page.locator('#program option')).toHaveCount(4);

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

    test('offers a reload when the program never takes the close request', async ({ page }) => {
      await open(page, backend, 'program=lwjgl');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });

      // a program that never polls, forged from outside so no bundle has to ship one
      await page.evaluate(() => {
        Object.defineProperty(window, '__flagonCloseRequested', { get: () => false, set: () => {} });
      });

      const stop = page.locator('#stop');
      await stop.click();
      await expect(stop).toHaveText('Reload', { timeout: 15_000 });
      await expect(stop).toBeEnabled();
      await expect(page.getByTestId('log')).toContainText('only reloading the page ends it');

      await stop.click();
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
      await expect(stop).toHaveText('Stop');
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

    test('shows the display only once a program opens one', async ({ page }) => {
      await open(page, backend, 'program=hello');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.locator('#display')).toBeHidden();

      await open(page, backend, 'program=lwjgl');
      await expect(page.locator('#display')).toBeVisible({ timeout: 30_000 });
      await expect(page.locator('#flagon-canvas')).toBeVisible();
    });

    test('reports the rate a running program is drawing at', async ({ page }) => {
      await open(page, backend, 'program=lwjgl');
      const rate = page.getByTestId('rate');

      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
      await expect(rate).toBeVisible();
      await expect(rate).toHaveText(/[1-9]\d* fps/, { timeout: 10_000 });

      await page.locator('#stop').click();
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(rate).toBeHidden();
    });

    test('hands the argument field to the program it runs', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=hello');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

      await page.locator('#args').fill('"a quoted name"');
      await page.locator('#run').click();

      await expect(page.getByTestId('log')).toContainText('Hello, a quoted name!', {
        timeout: 30_000,
      });
      await expect(page.locator('#args')).toHaveValue('a quoted name');
      expect(failures).toEqual([]);
    });

    test('says so when arguments were given with no program to take them', async ({ page }) => {
      await open(page, backend, 'arg=lonely');

      await expect(page.getByTestId('log')).toContainText('they were dropped', { timeout: 30_000 });
    });

    test('offers a bundle its programs without having run it', async ({ page }) => {
      await open(page, backend, 'bundle=example');
      await expect(page.locator('#program option')).toHaveCount(4);
      await expect(page.locator('#program option').nth(1)).toContainText('spinner — ');

      await page.locator('#bundle').selectOption('sandbox');
      await expect(page.locator('#program option')).toHaveCount(20);
      await expect(page.locator('#program option').nth(1)).toContainText('hello — ');

      await page.locator('#bundle').selectOption('example');
      await expect(page.locator('#program option')).toHaveCount(4);
    });

    test('keeps the log bounded while a program floods it', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=chatty');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

      const log = page.getByTestId('log');
      await expect(log).toContainText('line 2500');
      const kept = await log.evaluate((node) => node.childElementCount);
      expect(kept).toBeLessThanOrEqual(2000);
      const first = await log.evaluate((node) => node.firstElementChild?.textContent ?? '');
      expect(first.trim()).not.toBe('line 1');
      expect(failures).toEqual([]);
    });
  });
}
