import { test, expect } from '@playwright/test';
import { open } from './harness';

/** the shell's chrome is the same whichever backend runs underneath it, so this runs once. */
test.describe('the shell as a designed page', () => {
  test('wears the mark the readme leads with', async ({ page }) => {
    await open(page, 'js', 'program=hello');

    await expect(page.locator('link[rel=icon]')).toHaveAttribute('href', '/favicon.ico');
    const mark = page.locator('.mark');
    await expect(mark).toHaveAttribute('src', '/logo.png');
    expect(
      await mark.evaluate((image: HTMLImageElement) => image.naturalWidth),
    ).toBeGreaterThan(0);
  });

  test('gives the display more weight than the chrome around it', async ({ page }) => {
    await open(page, 'js', 'program=lwjgl');
    await expect(page.locator('#display')).toBeVisible({ timeout: 30_000 });

    const shadowOf = (selector: string) =>
      page.locator(selector).evaluate((node) => getComputedStyle(node).boxShadow);

    expect(await shadowOf('#display')).not.toBe('none');
    expect(await shadowOf('.controls')).toBe('none');
    expect(await shadowOf('.console')).toBe('none');
  });

  test('sizes every text role from one scale', async ({ page }) => {
    await open(page, 'js', 'program=hello');
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

    const sizes = await page.evaluate(() =>
      ['body', 'h1', 'h2', 'label', '#run', '.badge', '.log', '.tagline', '.phases li'].map(
        (selector) => getComputedStyle(document.querySelector(selector)!).fontSize,
      ),
    );

    expect([...new Set(sizes)].sort()).toEqual(['12px', '14px', '16px', '28px'].sort());
  });
});
