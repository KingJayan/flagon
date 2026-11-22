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

    test('opens the same file through the url getResource hands back', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=resources');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText(
        'resources: the url beside the class opens this file should get packed into the bundle',
      );
      await expect(log).toContainText(
        "resources: the loader's url opens this file should get packed into the bundle",
      );
      expect(failures).toEqual([]);
    });

    test('fetches a file kept apart once, then reads it from the cache', async ({ page }) => {
      const apart: string[] = [];
      page.on('request', (request) => {
        if (request.url().includes('/sandbox.files/')) {
          apart.push(request.url());
        }
      });
      const said =
        'resources: a file kept apart says this file loads from beside the pack only when it ' +
        'is read in 7 lines';
      await open(page, backend, 'program=resources');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText(said);
      const fetched = apart.length;
      expect(fetched).toBeGreaterThan(0);
      expect(new Set(apart).size).toBe(fetched);

      await page.reload();
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText(said);
      expect(apart).toHaveLength(fetched);
    });

    test('reads nothing for a file the build packed none of', async ({ page }) => {
      await open(page, backend, 'program=resources');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText(
        'resources: a file nobody packed reads nothing',
      );
      await expect(page.getByTestId('log')).toContainText(
        'resources: a file nobody packed has the url null',
      );
    });
  });
}
