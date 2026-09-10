// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS, open, readPixel, readPixels, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`a shader reading gl's fixed-function state on ${backend}`, () => {
    test('links, and the pipeline feeds it the matrices and attributes', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=fixedfunction');

      await expect(page.getByTestId('log')).toContainText('linked a fixed-function shader', {
        timeout: 30_000,
      });
      expect(failures).toEqual([]);
    });

    test('draws the triangle glBegin built, shaded by gl_Vertex', async ({ page }) => {
      await open(page, backend, 'program=fixedfunction');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });

      // the background stays the clear colour the fixed-function calls set
      const corner = await readPixel(page, 8, 8);
      expect(corner[0]).toBeLessThan(40);
      expect(corner[2]).toBeLessThan(40);

      // the triangle's own pixels came from gl_Color and gl_Vertex, so they are neither
      const middle = await readPixel(page, 320, 200);
      expect(middle[0] + middle[1] + middle[2]).toBeGreaterThan(60);
    });

    test('shades by gl_Vertex.y, so the triangle is darker at the bottom', async ({ page }) => {
      await open(page, backend, 'program=fixedfunction');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });

      const [high, low] = await readPixels(page, [
        [320, 250],
        [320, 130],
      ]);
      const brightness = (pixel: number[]): number => pixel[0] + pixel[1] + pixel[2];
      expect(brightness(high)).toBeGreaterThan(brightness(low));
    });

    test('takes the fixed-function pipeline back when the program is unbound', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=fixedfunction');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

      await expect(page.getByTestId('log')).toContainText('fixed-function shader drew');
      expect(failures).toEqual([]);
    });
  });
}
