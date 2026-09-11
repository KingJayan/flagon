// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`reflection over a class the build stripped on ${backend}`, () => {
    test('warns, and says how to keep the members', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=selftest');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

      const warning = page.getByTestId('log').locator('.warn', { hasText: 'flagon:' });
      await expect(warning.first()).toContainText('kept no members for it');
      await expect(warning.first()).toContainText('flagon.reflect.classes');
      expect(failures).toEqual([]);
    });

    test('says it once for a class, however often it is asked', async ({ page }) => {
      await open(page, backend, 'program=selftest');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

      const said = page.getByTestId('log').locator('.warn', { hasText: 'Square reports no' });
      await expect(said).toHaveCount(1);
    });

    test('stays quiet for a class the build was told to keep', async ({ page }) => {
      await open(page, backend, 'program=selftest');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });

      const log = page.getByTestId('log');
      await expect(log).toContainText('reflection: Reflected');
      await expect(log.locator('.warn', { hasText: 'Reflected reports no' })).toHaveCount(0);
    });
  });
}
