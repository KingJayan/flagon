import { readFileSync } from 'node:fs';
import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

// the jdk wrote these lines; LocaleChecksTest keeps them true
const EXPECTED = readFileSync(
  new URL('../../modules/bundles/sandbox/src/test/resources/locale.txt', import.meta.url),
  'utf8',
)
  .split('\n')
  .filter((line) => line.length > 0)
  .map((line) => `locale: ${line}`);

for (const backend of BACKENDS) {
  test.describe(`java.text and java.time on ${backend}`, () => {
    test('says what the jdk says', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=locale');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
      const log = await page.getByTestId('log').innerText();
      await expect(page.getByTestId('log')).toContainText('locale: done');
      expect(EXPECTED.filter((line) => !log.includes(line))).toEqual([]);
      expect(failures).toEqual([]);
    });
  });
}
