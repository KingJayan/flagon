import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`java serialization on ${backend}`, () => {
    test('reads and writes the bytes the jvm writes', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=serial');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('serial: read 2959 bytes with 0 differences');
      await expect(log).toContainText('serial: wrote the same 2959 bytes as the jvm');
      await expect(log).toContainText(
        'serial: a round trip keeps {odd=[1, 3]} and bad caused by io',
      );
      expect(failures).toEqual([]);
    });
  });
}
