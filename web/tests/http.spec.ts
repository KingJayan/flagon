import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test(`sends java.net.http requests through fetch on ${backend}`, async ({ page }, info) => {
    const { failures } = watchForFailures(page);
    const origin = new URL(info.project.use.baseURL ?? '').origin;
    await open(page, backend, `program=http&arg=${encodeURIComponent(origin)}`);

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
    const log = page.getByTestId('log');
    await expect(log).toContainText('http: done');
    for (const line of [
      "http: get answered 200 'GET - ' echo yes",
      "http: post answered 'POST marked héllo'",
      'http: put read back 9 bytes',
      'http: a missing page answers 404',
      "http: async answered 'PATCH - async'",
      "http: a stream reads 'GET - '",
      'http: a redirect ends at from=redirect',
      'http: a client that never follows refuses the redirect',
      'http: a slow answer times out: request timed out',
      'http: restricted header name: "Host"',
      'http: headers ignore case true',
    ]) {
      await expect(log).toContainText(line);
    }
    // chromium logs the 404 the program asks for on purpose
    expect(failures.filter((line) => !line.includes('status of 404'))).toEqual([]);
  });
}
