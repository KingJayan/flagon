import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`code the compiler once got wrong on ${backend}`, () => {
    test('runs a conditional that can suspend under a string', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=shapes');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('shapes: a split conditional carries 7');
      await expect(log).toContainText('shapes: a missing source reads -1');
      expect(failures).toEqual([]);
    });

    test('lists a reflected class whose other method takes a missing class', async ({ page }) => {
      await open(page, backend, 'program=shapes');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText(
        'shapes: a reflected class still lists label true',
      );
    });
  });
}
