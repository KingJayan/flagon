import { readFileSync } from 'node:fs';
import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

// the jdk wrote these lines; ConcurrencyChecksTest keeps them true
const EXPECTED = readFileSync(
  new URL('../../modules/bundles/sandbox/src/test/resources/concurrency.txt', import.meta.url),
  'utf8',
)
  .split('\n')
  .filter((line) => line.length > 0)
  .map((line) => `concurrency: ${line}`);

for (const backend of BACKENDS) {
  test.describe(`java.util.concurrent on ${backend}`, () => {
    test('says what the jdk says', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=concurrency');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
      const log = await page.getByTestId('log').innerText();
      await expect(page.getByTestId('log')).toContainText('concurrency: done');
      expect(EXPECTED.filter((line) => !log.includes(line))).toEqual([]);
      expect(failures).toEqual([]);
    });
  });
}
