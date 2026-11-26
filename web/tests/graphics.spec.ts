import { readFileSync } from 'node:fs';
import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

// the jdk wrote these lines; GraphicsChecksTest keeps them true
const EXPECTED = readFileSync(
  new URL('../../modules/bundles/sandbox/src/test/resources/graphics.txt', import.meta.url),
  'utf8',
)
  .split('\n')
  .filter((line) => line.length > 0)
  .map((line) => `graphics: ${line}`);

for (const backend of BACKENDS) {
  test(`draws java.awt and decodes images as the jdk does on ${backend}`, async ({ page }) => {
    const { failures } = watchForFailures(page);
    await open(page, backend, 'program=graphics');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
    const log = page.getByTestId('log');
    await expect(log).toContainText('graphics: done');
    const text = await log.innerText();
    expect(EXPECTED.filter((line) => !text.includes(line))).toEqual([]);
    for (const line of [
      'graphics: text paints true',
      'graphics: metrics true true true',
      'graphics: an oval fills true misses its corner true blends its edge true',
      'graphics: a bilinear stretch blends true',
      'graphics: webp 2x1 ff3366cc',
    ]) {
      expect(text).toContain(line);
    }
    expect(failures).toEqual([]);
  });
}
