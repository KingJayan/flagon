// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS, open, readPixel, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`the GLxxC core-profile classes on ${backend}`, () => {
    test('build and draw a program the way lwjgl 3 code writes it', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=glcore');

      await expect(page.getByTestId('log')).toContainText('built a core-profile program', {
        timeout: 30_000,
      });
      await expect(page.getByTestId('log')).toContainText('GL11C reports');
      expect(failures).toEqual([]);
    });

    test('draw the triangle the vertex buffer describes', async ({ page }) => {
      await open(page, backend, 'program=glcore');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });

      const corner = await readPixel(page, 8, 8);
      expect(corner[0] + corner[1] + corner[2]).toBeLessThan(60);

      const middle = await readPixel(page, 320, 200);
      expect(middle[0] + middle[1] + middle[2]).toBeGreaterThan(120);
    });

    test('run to the end with nothing failing', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=glcore');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

      await expect(page.getByTestId('log')).toContainText('the core-profile classes drew');
      expect(failures).toEqual([]);
    });
  });
}
