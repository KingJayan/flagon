import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`what the resource pack carries on ${backend}`, () => {
    test('reads a packed file by both the relative and the absolute name', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=resources');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText(
        'resources: beside the class says this file should get packed into the bundle',
      );
      await expect(log).toContainText(
        'resources: the absolute name says this file should get packed into the bundle',
      );
      await expect(log).toContainText(
        'resources: the loader says this file should get packed into the bundle',
      );
      expect(failures).toEqual([]);
    });

    test('reads nothing for a file the build packed none of', async ({ page }) => {
      await open(page, backend, 'program=resources');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText(
        'resources: a file nobody packed reads nothing',
      );
    });
  });
}
