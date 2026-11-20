// needs a display
import { test, expect } from '@playwright/test';
import { open } from './harness';

test.describe('reaching the shell without a mouse', () => {
  test('shows every control the keyboard lands on', async ({ page }) => {
    await open(page, 'js', 'program=hello');
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

    const outlined = () =>
      page.evaluate(() => {
        const focused = document.activeElement as HTMLElement | null;
        if (!focused || focused === document.body) {
          return null;
        }
        const style = getComputedStyle(focused);
        return {
          id: focused.id,
          shown: style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0,
        };
      });

    // stop is disabled once the program has exited, so the console follows run
    for (const id of ['source', 'bundle', 'backend', 'program', 'args', 'run', 'log']) {
      await page.keyboard.press('Tab');
      expect(await outlined()).toEqual({ id, shown: true });
    }
  });

  test('lets the keyboard scroll the console it reaches', async ({ page }) => {
    await open(page, 'js', 'program=hello');
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

    const log = page.locator('#log');
    expect(await log.evaluate((node) => getComputedStyle(node).overflowY)).toBe('auto');

    await log.focus();
    await log.evaluate((node) => node.append('filler\n'.repeat(400)));
    await expect
      .poll(() => log.evaluate((node) => node.scrollHeight > node.clientHeight))
      .toBe(true);

    await page.keyboard.press('PageDown');
    await expect.poll(() => log.evaluate((node) => node.scrollTop)).toBeGreaterThan(0);
  });
});

test.describe('what a screen reader is told', () => {
  test('announces output as it arrives', async ({ page }) => {
    await open(page, 'js', 'program=hello');

    const log = page.locator('#log');
    await expect(log).toHaveAttribute('role', 'log');
    await expect(log).toHaveAttribute('aria-live', 'polite');
    await expect(page.locator('.console')).toHaveAccessibleName('Console');
  });

  test('announces the run state as it changes', async ({ page }) => {
    await open(page, 'js', 'program=hello');
    const state = page.getByTestId('state');

    await expect(state).toHaveAttribute('role', 'status');
    await expect(state).toHaveAccessibleName('run state: exited', { timeout: 30_000 });
  });

  test('names the canvas and says what it is for without one', async ({ page }) => {
    await open(page, 'js', 'program=lwjgl');
    const canvas = page.locator('#flagon-canvas');

    await expect(canvas).toBeVisible({ timeout: 30_000 });
    await expect(canvas).toHaveAccessibleName("the program's display");
    await expect(canvas).toContainText('unable to show its display');
  });
});
