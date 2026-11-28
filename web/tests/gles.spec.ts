// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`lwjgl 3 gl es 3.0 on ${backend}`, () => {
    test('draws and reads back through the gl es api', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=gles');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('gles: 3.0 true, 3.1 false, version true');
      await expect(log).toContainText(
        'gles: framebuffer complete true, block true, targets read 0 0 255 255 and 0 255 0 255',
      );
      await expect(log).toContainText('gles: cleared target reads 255 255 0 255');
      await expect(log).toContainText('gles: layer 1 reads 255 0 255 255, sampler filter nearest true');
      await expect(log).toContainText('gles: client arrays read 255 255 255 255');
      await expect(log).toContainText('gles: transform feedback doubles 1 2 3 to 2.0 4.0 6.0');
      await expect(log).toContainText('gles: samples passed 1');
      await expect(log).toContainText('gles: fence signaled true');
      await expect(log).toContainText('gles: blit reads 0 0 255 255');
      await expect(log).toContainText('gles: no gl errors');
      await expect(log).toContainText('gles: done');
      expect(failures).toEqual([]);
    });
  });
}
