import { test, expect } from '@playwright/test';
import { open } from './harness';

/** the shell's chrome is the same whichever backend runs underneath it, so this runs once. */
test.describe('showing that the shell is working', () => {
  test('shows the bundle being fetched instead of nothing at all', async ({ page }) => {
    await page.route('**/flagon/sandbox.js', async (route) => {
      await new Promise((wait) => setTimeout(wait, 2_000));
      await route.continue();
    });

    await open(page, 'js', 'program=hello');
    const progress = page.getByTestId('progress');

    await expect(progress).toBeVisible();
    await expect(progress).toHaveText(/fetching sandbox\.js/);
    expect(
      await progress.evaluate((node) => getComputedStyle(node, '::after').animationName),
    ).toBe('sweep');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(progress).toBeHidden();
  });

  test('names the bundle it is waiting on with its size', async ({ page }) => {
    await open(page, 'js', 'program=hello');
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

    expect(await page.getByTestId('progress').textContent()).toMatch(
      /fetching sandbox\.js — \d+\.\d MB/,
    );
  });

  test('gives every run state a look of its own', async ({ page }) => {
    await open(page, 'js', 'program=hello');
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

    const looks: string[] = [];
    for (const state of ['loading', 'booting', 'running', 'exited', 'failed']) {
      await page.evaluate((next) => {
        document.querySelector<HTMLElement>('#app')!.dataset.state = next;
      }, state);
      await page.waitForTimeout(400);
      looks.push(
        await page.evaluate(() => {
          const badge = document.querySelector('#state')!;
          const style = getComputedStyle(badge);
          return [
            style.color,
            style.backgroundColor,
            getComputedStyle(badge, '::before').animationName,
          ].join(' ');
        }),
      );
    }

    expect(new Set(looks).size).toBe(looks.length);
  });
});
