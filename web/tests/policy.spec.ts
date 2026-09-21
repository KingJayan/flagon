import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

/** the shipping server sends no 'unsafe-eval', so a bundle that needs it fails here */
for (const backend of BACKENDS) {
  test(`runs on ${backend} under the shipping content policy`, async ({ page }) => {
    const { failures } = watchForFailures(page);

    await open(page, backend, 'program=selftest');

    const log = page.getByTestId('log');
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(log).not.toContainText('selftest failure');
    expect(failures).toEqual([]);
  });
}
