import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`shutdown hooks on ${backend}`, () => {
    test('run when main ends, in order, and skip the one that was removed', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=shutdownhook');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('removed a hook: true');
      await expect(log).toContainText('first hook ran');
      await expect(log).toContainText('second hook ran');
      await expect(log).not.toContainText('this hook should never run');

      const lines = (await log.innerText()).split('\n');
      const ended = lines.findIndex((line) => line.includes('main is done'));
      const first = lines.findIndex((line) => line.includes('first hook ran'));
      const second = lines.findIndex((line) => line.includes('second hook ran'));
      expect(ended).toBeLessThan(first);
      expect(first).toBeLessThan(second);
      expect(failures).toEqual([]);
    });
  });
}
