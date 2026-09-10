// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS, open, readPixel, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`a program supplying one shader stage on ${backend}`, () => {
    test('links with the stage gl would have run itself', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=halfshader');

      await expect(page.getByTestId('log')).toContainText('linked a vertex-only program', {
        timeout: 30_000,
      });
      await expect(page.getByTestId('log')).toContainText('linked a fragment-only program');
      expect(failures).toEqual([]);
    });

    test('the written fragment stage shades with the colour the vertex stage wrote', async ({
      page,
    }) => {
      await open(page, backend, 'program=halfshader');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });

      const left = await readPixel(page, 160, 200);
      expect(left[1]).toBeGreaterThan(180);
      expect(left[0]).toBeLessThan(80);
      expect(left[2]).toBeLessThan(80);
    });

    test('the written vertex stage feeds the fragment shader gl_Color', async ({ page }) => {
      await open(page, backend, 'program=halfshader');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });

      // red went in, and the fragment shader swaps bgr, so blue comes out
      const right = await readPixel(page, 480, 200);
      expect(right[2]).toBeGreaterThan(180);
      expect(right[0]).toBeLessThan(80);
      expect(right[1]).toBeLessThan(80);
    });

    test('runs to the end with nothing failing', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=halfshader');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

      await expect(page.getByTestId('log')).toContainText('half-shader programs drew');
      expect(failures).toEqual([]);
    });
  });
}
