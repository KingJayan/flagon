import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`images and the clipboard on ${backend}`, () => {
    test('writes a png and reads the same pixels back', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=image');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('image: a pixel reads back as 0xFF3366CC');
      await expect(log).toContainText('image: the raster holds 8 pixels');
      await expect(log).toContainText(/image: png written true in \d+ bytes/);
      await expect(log).toContainText('image: png read back 4x2 same pixels true');
      expect(failures).toEqual([]);
    });

    test('refuses a format it cannot write and copies one image into another', async ({
      page,
    }) => {
      await open(page, backend, 'program=image');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('image: jpeg is refused true');
      await expect(log).toContainText('image: a scaled copy carries the corner 0xFF3366CC');
      await expect(log).toContainText('image: greyscale reads 4x2 first 0xFF0A0A0A last 0xFF505050');
      await expect(log).toContainText('image: palette reads 4x2 first 0xFFFF0000 last 0xFF00FF00');
      await expect(log).toContainText('image: the clipboard hands back copied in the page');
    });
  });
}
