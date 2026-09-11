import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`what a ClassLoader can answer on ${backend}`, () => {
    test('finds a class the build kept and refuses one nobody compiled', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=classloader');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('classloader: loaded dev.flagon.sandbox.Reflected');
      await expect(log).toContainText('classloader: a class nobody compiled is refused by name');
      expect(failures).toEqual([]);
    });

    test('says so rather than quietly finding no copies of a resource', async ({ page }) => {
      await open(page, backend, 'program=classloader');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('classloader: scanning found any resource false');
      await expect(log).toContainText('a bundle is one artifact with no classpath to scan');
    });
  });
}
