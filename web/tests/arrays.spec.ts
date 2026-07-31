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

function expectColor(actual: number[], expected: number[], label: string): void {
  for (let channel = 0; channel < 3; channel++) {
    expect(
      Math.abs(actual[channel] - expected[channel]),
      `${label}: channel ${channel} was ${actual} expected ~${expected}`,
    ).toBeLessThanOrEqual(2);
  }
}

const GREEN = [0, 204, 51];
const RED = [230, 51, 26];
const CLEAR = [26, 38, 77];

async function runDemo(page: Page): Promise<void> {
  await page.goto('/?program=arrays');
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('vertex buffer');
}

test.describe('vertex arrays and buffer objects', () => {
  /**
   all four corners are covered only if both indexed triangles were issued
   */
  test('draws an indexed quad out of a buffer object', async ({ page }) => {
    await runDemo(page);

    expectColor(await readPixel(page, 176, 180), GREEN, 'vbo quad center');
    expectColor(await readPixel(page, 70, 95), GREEN, 'bottom-left corner');
    expectColor(await readPixel(page, 282, 95), GREEN, 'bottom-right corner');
    expectColor(await readPixel(page, 70, 265), GREEN, 'top-left corner');
    expectColor(await readPixel(page, 282, 265), GREEN, 'top-right corner');
  });

  /**
   the vertex colors are unsigned bytes
   */
  test('reads byte colors as normalised components', async ({ page }) => {
    await runDemo(page);
    expect((await readPixel(page, 176, 180))[1]).toBeGreaterThan(200);
  });

  test('feeds the current color where no color array is enabled', async ({ page }) => {
    await runDemo(page);
    expectColor(await readPixel(page, 460, 220), RED, 'client array quad');
  });

  test('leaves the space between the two quads alone', async ({ page }) => {
    await runDemo(page);
    expectColor(await readPixel(page, 320, 180), CLEAR, 'between the quads');
    expectColor(await readPixel(page, 5, 5), CLEAR, 'clear color');
  });

  /** the pointer is set over one buffer whose contents change every frame. */
  test('re-reads a client array as its contents change', async ({ page }) => {
    await runDemo(page);

    const heights = new Set<number>();
    for (let attempt = 0; attempt < 12; attempt++) {
      const column = await page.evaluate(() => {
        const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement;
        const gl = canvas.getContext('webgl2')!;
        const pixels = new Uint8Array(360 * 4);
        gl.readPixels(460, 0, 1, 360, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
        let covered = 0;
        for (let y = 0; y < 360; y++) {
          if (pixels[y * 4] > 128) {
            covered++;
          }
        }
        return covered;
      });
      heights.add(column);
      await page.waitForTimeout(120);
    }

    expect(heights.size, 'the client array quad never changed size').toBeGreaterThan(1);
  });

  test('runs the whole demo without a browser error', async ({ page }) => {
    const failures: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        failures.push(message.text());
      }
    });
    page.on('pageerror', (error) => failures.push(error.message));

    await runDemo(page);
    await page.waitForTimeout(1500);

    expect(failures).toEqual([]);
  });
});
