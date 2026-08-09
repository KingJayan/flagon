import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, expectColor, open, readPixel, watchForFailures, type Backend } from './harness';

async function runDemo(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=lwjgl');
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
}

for (const backend of BACKENDS) {
  test.describe(`lwjgl rendering on ${backend}`, () => {
    test('draws through the compatibility layer into a webgl2 canvas', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await runDemo(page, backend);
      await expect(page.getByTestId('log')).toContainText('display created at 640x360');

      expectColor(await readPixel(page, 320, 180), [0, 204, 51], 'center panel');

      expectColor(await readPixel(page, 5, 5), [26, 38, 77], 'clear color');

      expect(failures).toEqual([]);
    });

    /**
     the panel is drawn with GL_QUADS, which becomes triangles (0,1,2) and (0,2,3)
     */
    test('fills a whole GL_QUADS quad, not just one of its triangles', async ({ page }) => {
      await runDemo(page, backend);

      const green = [0, 204, 51];
      expectColor(await readPixel(page, 165, 95), green, 'bottom-left corner');
      expectColor(await readPixel(page, 475, 95), green, 'bottom-right corner');
      expectColor(await readPixel(page, 165, 265), green, 'top-left corner');
      expectColor(await readPixel(page, 475, 265), green, 'top-right corner');

      // just outside the panel is still the clear color, so the quad is not oversized
      expectColor(await readPixel(page, 155, 180), [26, 38, 77], 'left of the panel');
      expectColor(await readPixel(page, 485, 180), [26, 38, 77], 'right of the panel');
    });

    test('keeps presenting frames', async ({ page }) => {
      await runDemo(page, backend);

      const samples = new Set<string>();
      for (let attempt = 0; attempt < 12; attempt++) {
        const row = await page.evaluate(() => {
          const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement;
          const gl = canvas.getContext('webgl2')!;
          const pixels = new Uint8Array(640 * 4);
          gl.readPixels(0, 20, 640, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
          let signature = '';
          for (let x = 0; x < 640; x += 16) {
            signature += pixels[x * 4] > 128 ? '1' : '0';
          }
          return signature;
        });
        samples.add(row);
        await page.waitForTimeout(120);
      }

      expect(samples.size, 'the animated marker never moved').toBeGreaterThan(1);
    });

    /** what the program can find out about the gl it got, and whether it complained. */
    test('answers the queries a program checks its context with', async ({ page }) => {
      await runDemo(page, backend);

      const log = page.getByTestId('log');
      await expect(log).toContainText(/gl WebGL .+ textures up to \d+/, { timeout: 30_000 });
      await expect(log).toContainText('no gl errors', { timeout: 60_000 });
    });

    test('reports the frame loop finishing', async ({ page }) => {
      await open(page, backend, 'program=lwjgl');
      await expect(page.getByTestId('log')).toContainText('frame 60', { timeout: 30_000 });
    });
  });
}
