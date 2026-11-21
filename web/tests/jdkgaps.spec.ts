import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`the jdk calls the browser classlib has none of on ${backend}`, () => {
    test('answers a memory ceiling and refuses what a page cannot do', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=jdkgaps');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText(/jdkgaps: the memory ceiling is \d+mb/);
      await expect(log).toContainText('jdkgaps: starting a process is refused by name');
      await expect(log).toContainText('jdkgaps: loading a native library is refused by name');
      expect(failures).toEqual([]);
    });

    test('answers the class, name and time calls a game reads', async ({ page }) => {
      await open(page, backend, 'program=jdkgaps');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('jdkgaps: the superclass of a string is class java.lang.Object');
      await expect(log).toContainText('jdkgaps: an array list extends java.util.AbstractList<E>');
      await expect(log).toContainText('jdkgaps: a string is anonymous false');
      await expect(log).toContainText('jdkgaps: punycode writes a name as xn--mnchen-3ya.de');
      await expect(log).toContainText('jdkgaps: a replacement with an unmatched group gives %s and %2$s');
      await expect(log).toContainText('jdkgaps: the sql day is 2026-01-02');
      await expect(log).toContainText('jdkgaps: uptime is at least 0ms true');
      await expect(log).toContainText('jdkgaps: a wrapped runnable answers null');
      await expect(log).toContainText('jdkgaps: an array of arrays is 2x3 holding 7');
      await expect(log).toContainText('jdkgaps: a catch for a failed class setup compiles');
      await expect(log).toContainText('jdkgaps: a catch for a missing class compiles');
      await expect(log).toContainText('jdkgaps: four bytes make an ipv4 address true at 127.0.0.1');
      await expect(log).toContainText('jdkgaps: sixteen bytes make a loopback ipv6 address true');
      await expect(log).toContainText('jdkgaps: a connection through a named proxy opens true');
      await expect(log).toContainText(
        'jdkgaps: with nothing to take a deflater writes its header, 2 bytes, then 0, and an '
          + 'inflater reads 0');
      await expect(log).toContainText('jdkgaps: references to nothing read null and null');
      await expect(log).toContainText('jdkgaps: an empty select waits out its time true');
      await expect(log).toContainText('jdkgaps: another thread wakes a select holding 0 keys');
    });
  });
}
