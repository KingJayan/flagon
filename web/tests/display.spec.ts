// needs a display
import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, open, readPixel, watchForFailures, type Backend } from './harness';

const RATE = 30;
const FRAMES = 30;

async function start(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=display');
  await expect(page.getByTestId('log')).toContainText('display ready 640x360', {
    timeout: 30_000,
  });
}

async function pacedMillis(page: Page): Promise<number> {
  const log = page.getByTestId('log');
  await expect(log).toContainText(/paced \d+ frames at \d+ in \d+ms/, { timeout: 30_000 });
  const line = /paced \d+ frames at \d+ in (\d+)ms/.exec((await log.textContent()) ?? '');
  return Number(line?.[1] ?? 0);
}

for (const backend of BACKENDS) {
  test.describe(`the display lifecycle on ${backend}`, () => {
    test('holds a loop to the frame rate it was asked for', async ({ page }) => {
      await start(page, backend);

      // unpaced, thirty animation frames take about half a second
      expect(await pacedMillis(page)).toBeGreaterThan((FRAMES / RATE) * 1000 * 0.8);
    });

    test('opens another display after one is destroyed', async ({ page }) => {
      await start(page, backend);
      const log = page.getByTestId('log');

      await expect(log).toContainText('display destroyed, created=false', { timeout: 30_000 });
      await expect(log).toContainText('display again 640x360 closing=false', { timeout: 30_000 });

      const pixel = await readPixel(page, 320, 180);
      expect(pixel[0]).toBeGreaterThan(150);
      expect(pixel[1]).toBeLessThan(80);
    });

    test('leaves the page its canvas where the page supplied one', async ({ page }) => {
      await start(page, backend);
      await expect(page.getByTestId('log')).toContainText('display again', { timeout: 30_000 });

      expect(
        await page.evaluate(() => {
          const canvas = document.getElementById('flagon-canvas');
          return canvas !== null && canvas.closest('#display') !== null;
        }),
      ).toBe(true);
    });

    test('reports the canvas losing and regaining the keyboard', async ({ page }) => {
      await start(page, backend);
      const log = page.getByTestId('log');

      await expect(log).toContainText('focus: active=true visible=true', { timeout: 30_000 });

      await page.evaluate(() => document.getElementById('flagon-canvas')!.blur());
      await expect(log).toContainText('focus: active=false visible=true', { timeout: 10_000 });

      await page.evaluate(() => document.getElementById('flagon-canvas')!.focus());
      await expect(log).toContainText(/active=false[\s\S]*active=true/, { timeout: 10_000 });
    });

    // headless chromium keeps a backgrounded tab visible, so the state itself is what moves here
    test('reports a tab nobody is looking at as hidden', async ({ page }) => {
      await start(page, backend);
      const log = page.getByTestId('log');
      await expect(log).toContainText('focus: active=true visible=true', { timeout: 30_000 });

      await page.evaluate(() =>
        Object.defineProperty(document, 'visibilityState', {
          configurable: true,
          get: () => 'hidden',
        }),
      );
      await expect(log).toContainText('visible=false', { timeout: 15_000 });

      await page.evaluate(() => delete (document as unknown as Record<string, unknown>)
        .visibilityState);
      await expect(log).toContainText(/visible=false[\s\S]*visible=true/, { timeout: 15_000 });
    });

    test('runs the whole demo without a console error', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await start(page, backend);
      await expect(page.getByTestId('log')).toContainText('display again', { timeout: 30_000 });

      expect(failures).toEqual([]);
    });
  });
}
