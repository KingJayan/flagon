// needs a display
import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, open, readPixel, watchForFailures, type Backend } from './harness';

const CANVAS = '#flagon-canvas';

async function start(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=input');
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('input ready');
}

async function canvasPoint(page: Page, x: number, y: number): Promise<{ x: number; y: number }> {
  const box = await page.locator(CANVAS).boundingBox();
  if (!box) {
    throw new Error('the canvas has no box');
  }
  const [width, height] = await page
    .locator(CANVAS)
    .evaluate((canvas: HTMLCanvasElement) => [canvas.width, canvas.height]);
  // the page scales the canvas and object-fit: contain centres the picture in its box
  const scale = Math.max(width / box.width, height / box.height);
  return {
    x: box.x + (box.width - width / scale) / 2 + x / scale,
    y: box.y + (box.height - height / scale) / 2 + y / scale,
  };
}

for (const backend of BACKENDS) {
  test.describe(`lwjgl input on ${backend}`, () => {
    test('reports key presses and releases with their characters', async ({ page }) => {
      await start(page, backend);
      const log = page.getByTestId('log');

      await page.keyboard.press('KeyK');
      await expect(log).toContainText('key down K char=k');
      await expect(log).toContainText('key up K');

      await page.keyboard.press('Escape');
      await expect(log).toContainText('key down ESCAPE');

      await page.keyboard.press('ArrowLeft');
      await expect(log).toContainText('key down LEFT char=none');
    });

    test('tracks a held key until it is released', async ({ page }) => {
      await start(page, backend);
      await page.keyboard.down('KeyW');
      await expect
        .poll(async () => (await readPixel(page, 600, 300))[0], { timeout: 5_000 })
        .toBeGreaterThan(150);

      await page.keyboard.up('KeyW');
      await expect
        .poll(async () => (await readPixel(page, 600, 300))[0], { timeout: 5_000 })
        .toBeLessThan(60);
    });

    test('maps mouse buttons to the lwjgl order', async ({ page }) => {
      await start(page, backend);
      const log = page.getByTestId('log');
      const point = await canvasPoint(page, 320, 180);

      await page.mouse.move(point.x, point.y);
      await page.mouse.down({ button: 'left' });
      await page.mouse.up({ button: 'left' });
      await expect(log).toContainText('button 0 down');

      await page.mouse.down({ button: 'right' });
      await page.mouse.up({ button: 'right' });
      await expect(log).toContainText('button 1 down');

      await page.mouse.down({ button: 'middle' });
      await page.mouse.up({ button: 'middle' });
      await expect(log).toContainText('button 2 down');
    });

    /**
     * dom y vs gl y
     */
    test('reports the pointer with y counted up from the bottom', async ({ page }) => {
      await start(page, backend);
      const log = page.getByTestId('log');

      const near = await canvasPoint(page, 100, 40);
      await page.mouse.move(near.x, near.y);
      await page.mouse.down();
      await page.mouse.up();

      await expect(log).toContainText(/button 0 down at 100,3\d\d/);
    });

    test('turns wheel movement into notches away from the user', async ({ page }) => {
      await start(page, backend);
      const log = page.getByTestId('log');
      const point = await canvasPoint(page, 320, 180);
      await page.mouse.move(point.x, point.y);

      await page.mouse.wheel(0, -100);
      await expect(log).toContainText('wheel 120');

      await page.mouse.wheel(0, 200);
      await expect(log).toContainText('wheel -240');
    });

    test('draws the marker under the pointer', async ({ page }) => {
      await start(page, backend);

      const point = await canvasPoint(page, 200, 120);
      await page.mouse.move(point.x, point.y);

      await expect
        .poll(async () => (await readPixel(page, 200, 240))[1], { timeout: 5_000 })
        .toBeGreaterThan(150);
      expect((await readPixel(page, 200, 60))[1]).toBeLessThan(100);
    });

    test('takes the keyboard only while the canvas has focus', async ({ page }) => {
      await start(page, backend);
      const focused = () => page.evaluate(() => document.activeElement?.id ?? '');

      expect(await focused()).toBe('flagon-canvas');

      // tab is the way out: nothing on the page may be left unreachable
      await page.keyboard.press('Tab');
      expect(await focused()).not.toBe('flagon-canvas');

      // whether the page still gets to act on a key is what preventDefault decides
      await page.evaluate(() => {
        const seen: boolean[] = [];
        (window as unknown as { swallowed: boolean[] }).swallowed = seen;
        window.addEventListener('keydown', (event) => seen.push(event.defaultPrevented));
      });
      const swallowed = () =>
        page.evaluate(() => (window as unknown as { swallowed: boolean[] }).swallowed.pop());

      await page.locator('#program').focus();
      await page.keyboard.press('Space');
      expect(await swallowed()).toBe(false);

      // and a key pressed elsewhere is the page's, not the program's
      await page.keyboard.down('KeyW');
      expect((await readPixel(page, 600, 300))[0]).toBeLessThan(60);
      await page.keyboard.up('KeyW');

      await page.locator(CANVAS).click();
      await page.keyboard.press('Space');
      expect(await swallowed()).toBe(true);
    });

    test('reports the pointer in canvas pixels, not css pixels', async ({ page }) => {
      await page.setViewportSize({ width: 420, height: 800 });
      await start(page, backend);
      const log = page.getByTestId('log');

      const box = await page.locator(CANVAS).boundingBox();
      if (!box) {
        throw new Error('the canvas has no box');
      }
      expect(box.width).toBeLessThan(500);

      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
      await page.mouse.down();
      await page.mouse.up();

      await expect(log).toContainText(/button 0 down at 3[12]\d,1[78]\d/);
    });

    test.describe('on a touch screen', () => {
      test.use({ hasTouch: true, viewport: { width: 420, height: 860 } });

      test('reports a tap as the pointer it is', async ({ page }) => {
        await start(page, backend);
        const log = page.getByTestId('log');

        const point = await canvasPoint(page, 0, 0);
        const box = await page.locator(CANVAS).boundingBox();
        await page.touchscreen.tap(point.x + box!.width / 2, point.y + box!.height / 2);

        await expect(log).toContainText(/button 0 down at 3[12]\d,1[78]\d/);
        await expect(log).toContainText(/button 0 up at 3[12]\d,1[78]\d/);
      });

      test('drags without the page panning under it', async ({ page }) => {
        const { failures } = watchForFailures(page);
        await start(page, backend);
        const box = await page.locator(CANVAS).boundingBox();
        if (!box) {
          throw new Error('the canvas has no box');
        }

        await page.locator(CANVAS).dispatchEvent('touchstart', {
          changedTouches: [{ identifier: 1, clientX: box.x + 10, clientY: box.y + 10 }],
        });
        await page.locator(CANVAS).dispatchEvent('touchmove', {
          changedTouches: [
            { identifier: 1, clientX: box.x + box.width / 2, clientY: box.y + box.height / 2 },
          ],
        });
        await page.locator(CANVAS).dispatchEvent('touchend', {
          changedTouches: [
            { identifier: 1, clientX: box.x + box.width / 2, clientY: box.y + box.height / 2 },
          ],
        });

        await expect(page.getByTestId('log')).toContainText(/button 0 up at 3[12]\d,1[78]\d/);
        expect(await page.evaluate(() => window.scrollY)).toBe(0);
        expect(failures).toEqual([]);
      });
    });

    test('runs the whole demo without a console error', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await start(page, backend);
      await page.keyboard.press('KeyE');
      const point = await canvasPoint(page, 320, 180);
      await page.mouse.move(point.x, point.y);
      await page.mouse.down();
      await page.mouse.up();
      await page.mouse.wheel(0, -100);

      expect(failures).toEqual([]);
    });
  });
}
