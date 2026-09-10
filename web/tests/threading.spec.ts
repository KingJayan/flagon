// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`a render loop with workers behind it on ${backend}`, () => {
    test('keeps drawing while a worker thread sleeps', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=threading');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('threading: worker ticked 5 over true frames drawn beside it');
      await expect(log).toContainText('threading: done');
      expect(failures).toEqual([]);
    });

    test('runs an executor and takes its futures back', async ({ page }) => {
      await open(page, backend, 'program=threading');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('threading: pool summed 55, terminated true');
    });

    test('runs the completable future chains chunk loading is built on', async ({ page }) => {
      await open(page, backend, 'program=threading');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('threading: chain produced chunk 42');
      await expect(log).toContainText('threading: combined 5');
      await expect(log).toContainText('threading: allOf waited for 4');
      await expect(log).toContainText('threading: a failed stage recovered');
    });

    test('opens a latch and guards a counter with a lock', async ({ page }) => {
      await open(page, backend, 'program=threading');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('threading: latch opened true with 3 under a lock');
    });

    test('drains chunks the workers produced without stalling the loop', async ({ page }) => {
      await open(page, backend, 'program=threading');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('threading: drained 12 of 12 chunks');
      await expect(log).toContainText('threading: the loop kept drawing while empty true');
    });
  });
}
