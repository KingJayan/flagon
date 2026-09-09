import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`the lwjgl 3 memory model on ${backend}`, () => {
    test('reads a buffer back through the address it was given', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=lwjgl3');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('lwjgl3: address reads back 42');
      await expect(log).toContainText('lwjgl3: address plus four reads back 0');
      await expect(log).toContainText('lwjgl3: nothing has address 0');
      await expect(log).toContainText('lwjgl3: text round trips as flagon');
      expect(failures).toEqual([]);
    });

    test('refuses an address it never handed out', async ({ page }) => {
      await open(page, backend, 'program=lwjgl3');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('lwjgl3: an invented address is refused');
      await expect(log).not.toContainText('an invented address was followed');
    });

    test('gives a stack frame its bytes back when it closes', async ({ page }) => {
      await open(page, backend, 'program=lwjgl3');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('lwjgl3: the stack holds 854x480, reached again as 854');
      await expect(log).toContainText('lwjgl3: the frame gave back every byte');
    });
  });
}
