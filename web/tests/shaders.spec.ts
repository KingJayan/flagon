import { test, expect, type Page } from '@playwright/test';

async function readPixel(page: Page, x: number, y: number): Promise<number[]> {
  return page.evaluate(
    ([px, py]) => {
      const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement | null;
      if (!canvas) {
        throw new Error('no canvas on the page');
      }
      const gl = canvas.getContext('webgl2');
      if (!gl) {
        throw new Error('no webgl2 context');
      }
      const pixel = new Uint8Array(4);
      gl.readPixels(px, py, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel);
      return Array.from(pixel);
    },
    [x, y],
  );
}

const CLEAR = [15, 15, 26];

async function runDemo(page: Page): Promise<void> {
  await page.goto('/?program=shaders');
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('program ');
}

test.describe('an application-supplied shader program', () => {
  test('links a desktop GLSL 1.20 program and reports it', async ({ page }) => {
    const failures: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        failures.push(message.text());
      }
    });
    page.on('pageerror', (error) => failures.push(error.message));

    await runDemo(page);

    // a uniform the shader does not declare is -1, and writing to it is allowed
    await expect(page.getByTestId('log')).toContainText('missing uniform reports -1');
    expect(failures).toEqual([]);
  });

  test('draws the triangle its own shader colors', async ({ page }) => {
    await runDemo(page);

    // inside the triangle, where the vertex tints interpolate to something bright
    const inside = await readPixel(page, 320, 153);
    expect(
      inside.slice(0, 3).some((channel) => channel > 60),
      `center was ${inside}`,
    ).toBe(true);

    const outside = await readPixel(page, 620, 340);
    for (let channel = 0; channel < 3; channel++) {
      expect(Math.abs(outside[channel] - CLEAR[channel])).toBeLessThanOrEqual(2);
    }
  });

  /** the byte tints are normalised, so the corners keep the colors the vertices carry. */
  test('normalises the byte attribute the vertices carry', async ({ page }) => {
    await runDemo(page);

    const nearApex = await readPixel(page, 320, 270);
    expect(nearApex[0], `apex was ${nearApex}`).toBeGreaterThan(nearApex[2]);

    const nearRight = await readPixel(page, 480, 90);
    expect(nearRight[2], `right corner was ${nearRight}`).toBeGreaterThan(nearRight[0]);
  });

  test('drives the program from a uniform every frame', async ({ page }) => {
    await runDemo(page);

    const signature = async (): Promise<string> =>
      page.evaluate(() => {
        const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement;
        const gl = canvas.getContext('webgl2')!;
        const pixels = new Uint8Array(360 * 4);
        gl.readPixels(320, 0, 1, 360, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
        let bits = '';
        for (let y = 250; y < 320; y += 4) {
          bits += pixels[y * 4] > 40 ? '1' : '0';
        }
        return bits;
      });

    const samples = new Set<string>();
    for (let attempt = 0; attempt < 10; attempt++) {
      samples.add(await signature());
      await page.waitForTimeout(100);
    }
    expect(samples.size, 'the scale uniform never changed the triangle').toBeGreaterThan(1);
  });

  /** glUseProgram(0) has to give the fixed-function pipeline its shader back. */
  test('returns to fixed-function drawing when the program is unbound', async ({ page }) => {
    await runDemo(page);

    const marker = await readPixel(page, 12, 12);
    for (let channel = 0; channel < 3; channel++) {
      expect(marker[channel], `marker was ${marker}`).toBeGreaterThan(200);
    }
  });
});
