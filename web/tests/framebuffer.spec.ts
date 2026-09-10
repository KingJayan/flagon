// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`framebuffer objects on ${backend}`, () => {
    test('completes a framebuffer with a texture and a depth renderbuffer', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=framebuffer');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('fbo: status is complete');
      await expect(log).toContainText('fbo: bound true');
      await expect(log).toContainText('fbo: done');
      expect(failures).toEqual([]);
    });

    test('reads back what it drew into the texture', async ({ page }) => {
      await open(page, backend, 'program=framebuffer');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('fbo: read back 0,255,0');
    });

    test('binds the surface back when the framebuffer goes', async ({ page }) => {
      await open(page, backend, 'program=framebuffer');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('fbo: back on the surface true');
    });
  });
}
