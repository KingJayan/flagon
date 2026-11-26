import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test(`starts bundle workers and talks to them on ${backend}`, async ({ page }) => {
    const { failures } = watchForFailures(page);
    await open(page, backend, 'program=workers');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
    const log = page.getByTestId('log');
    for (const line of [
      'workers: available true parent false',
      '[worker-echo] echo: a worker is running with no display true',
      "workers: text came back 'HELLO'",
      'workers: bytes came back reversed true and the sent array is whole true',
      "workers: then 'BYE' and the end true open false",
      '[worker-echo] echo: done',
      'workers: a closed worker ends true and polls nothing true',
      'workers: done',
    ]) {
      await expect(log).toContainText(line);
    }
    expect(failures).toEqual([]);
  });
}
