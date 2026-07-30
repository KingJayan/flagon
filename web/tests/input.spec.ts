import { test, expect, type Page } from '@playwright/test';

const CANVAS = '#flagon-canvas';

async function start(page: Page): Promise<void> {
  await page.goto('/?program=input');
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('input ready');
}

async function canvasPoint(page: Page, x: number, y: number): Promise<{ x: number; y: number }> {
  const box = await page.locator(CANVAS).boundingBox();
  if (!box) {
    throw new Error('the canvas has no box');
  }
  return { x: box.x + x, y: box.y + y };
}

async function readPixel(page: Page, x: number, y: number): Promise<number[]> {
  return page.evaluate(
    ([px, py]) => {
      const gl = (document.getElementById('flagon-canvas') as HTMLCanvasElement).getContext(
        'webgl2',
      )!;
      const pixel = new Uint8Array(4);
      gl.readPixels(px, py, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel);
      return Array.from(pixel);
    },
    [x, y],
  );
}

test.describe('lwjgl input', () => {
  test('reports key presses and releases with their characters', async ({ page }) => {
    await start(page);
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
    
    await start(page);
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
    await start(page);
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
    await start(page);
    const log = page.getByTestId('log');

    const near = await canvasPoint(page, 100, 40);
    await page.mouse.move(near.x, near.y);
    await page.mouse.down();
    await page.mouse.up();

    await expect(log).toContainText(/button 0 down at 100,3\d\d/);
  });

  test('turns wheel movement into notches away from the user', async ({ page }) => {
    await start(page);
    const log = page.getByTestId('log');
    const point = await canvasPoint(page, 320, 180);
    await page.mouse.move(point.x, point.y);

    await page.mouse.wheel(0, -100);
    await expect(log).toContainText('wheel 120');

    await page.mouse.wheel(0, 200);
    await expect(log).toContainText('wheel -240');
  });

  test('draws the marker under the pointer', async ({ page }) => {
    await start(page);

    const point = await canvasPoint(page, 200, 120);
    await page.mouse.move(point.x, point.y);

    await expect
      .poll(async () => (await readPixel(page, 200, 240))[1], { timeout: 5_000 })
      .toBeGreaterThan(150);
    expect((await readPixel(page, 200, 60))[1]).toBeLessThan(100);
  });

  test('runs the whole demo without a console error', async ({ page }) => {
    const failures: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        failures.push(message.text());
      }
    });
    page.on('pageerror', (error) => failures.push(error.message));

    await start(page);
    await page.keyboard.press('KeyE');
    const point = await canvasPoint(page, 320, 180);
    await page.mouse.move(point.x, point.y);
    await page.mouse.down();
    await page.mouse.up();
    await page.mouse.wheel(0, -100);

    expect(failures).toEqual([]);
  });
});
