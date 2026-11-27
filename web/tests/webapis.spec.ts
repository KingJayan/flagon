import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

const COMMON = [
  'indexeddb: a holds [1, 2, 3] and b holds text',
  'indexeddb: the keys are [a, b], 2 of them',
  'indexeddb: a is gone true and nothing holds null',
  'indexeddb: bytes from a string are refused',
  'indexeddb: a missing store is refused with NotFoundError',
  'indexeddb: version 1 upgrades to 2 with [files, more] and b still holds text',
  "indexeddb: 'a refused upgrade' leaves version 2",
  "rtc: the other peer hears 'hello over chat'",
  'codecs: one frame encodes to 1 key chunk',
  'codecs: the chunk decodes to [16x16]',
  'transport: a session closed before it connects is refused with WebTransportError',
  'done',
];

for (const backend of BACKENDS) {
  test(`drives the web api bindings on ${backend}`, async ({ page, browserName }) => {
    const { failures } = watchForFailures(page);
    await open(page, backend, 'program=webapis');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
    const log = await page.getByTestId('log').innerText();
    expect(COMMON.filter((line) => !log.includes(`webapis: ${line}`))).toEqual([]);
    if (browserName === 'firefox') {
      expect(log).toContain('webapis: bluetooth: not offered');
      expect(log).toContain('webapis: usb: not offered');
    } else {
      expect(log).toMatch(/webapis: bluetooth: (the adapter check answers|not offered)/);
      expect(log).toContain('webapis: usb: the page may use 0 devices');
    }
    // chromium logs the refused webtransport connection
    expect(failures.filter((line) => !line.includes('ERR_CONNECTION_REFUSED'))).toEqual([]);
  });
}
