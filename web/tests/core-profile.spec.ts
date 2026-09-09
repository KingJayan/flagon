import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`gl 3.2 core profile on ${backend}`, () => {
    test('reports a 3.2 context and binds a vertex array', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=core-profile');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('core: profile 3.2, 3.3 entry points true');
      await expect(log).toContainText('core: vertex array bound true');
      await expect(log).toContainText('core: done');
      expect(failures).toEqual([]);
    });

    test('draws instanced triangles', async ({ page }) => {
      await open(page, backend, 'program=core-profile');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('core: drew 10 frames of 3 instances');
      await expect(page.locator('canvas')).toBeVisible();
    });

    test('refuses a base vertex webgl2 cannot honour', async ({ page }) => {
      await open(page, backend, 'program=core-profile');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('core: base vertex is refused');
    });
  });
}
