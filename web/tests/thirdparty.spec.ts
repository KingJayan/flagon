// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`a third-party LWJGL 3 application on ${backend}`, () => {
    test("runs LWJGL's own guide sample, which nobody here wrote", async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'bundle=example&program=hello-lwjgl3');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
      await expect(log).toContainText('Hello LWJGL 3.3.3');
      await expect(page.locator('canvas')).toBeVisible();
      expect(failures).toEqual([]);
    });

    test('draws its loop until the page stops it', async ({ page }) => {
      await open(page, backend, 'bundle=example&program=hello-lwjgl3');

      const stop = page.locator('#stop');
      await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
      await expect(stop).toBeEnabled();

      await stop.click();
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    });
  });
}
