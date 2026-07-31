import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, expectColor, open, readPixel, watchForFailures, type Backend } from './harness';

const GREEN = [0, 204, 51];
const RED = [230, 51, 26];
const CLEAR = [26, 38, 77];

async function runDemo(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=arrays');
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('vertex buffer');
}

for (const backend of BACKENDS) {
  test.describe(`vertex arrays and buffer objects on ${backend}`, () => {
    /**
     all four corners are covered only if both indexed triangles were issued
     */
    test('draws an indexed quad out of a buffer object', async ({ page }) => {
      await runDemo(page, backend);

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
      await runDemo(page, backend);
      expect((await readPixel(page, 176, 180))[1]).toBeGreaterThan(200);
    });

    test('feeds the current color where no color array is enabled', async ({ page }) => {
      await runDemo(page, backend);
      expectColor(await readPixel(page, 460, 220), RED, 'client array quad');
    });

    test('leaves the space between the two quads alone', async ({ page }) => {
      await runDemo(page, backend);
      expectColor(await readPixel(page, 320, 180), CLEAR, 'between the quads');
      expectColor(await readPixel(page, 5, 5), CLEAR, 'clear color');
    });

    /** the pointer is set over one buffer whose contents change every frame. */
    test('re-reads a client array as its contents change', async ({ page }) => {
      await runDemo(page, backend);

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
      const { failures } = watchForFailures(page);

      await runDemo(page, backend);
      await page.waitForTimeout(1500);

      expect(failures).toEqual([]);
    });
  });
}
