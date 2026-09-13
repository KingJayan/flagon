import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`the crypto online mode needs on ${backend}`, () => {
    test('gives the digests the answers their standards publish', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=crypto');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText(
        'crypto: sha-1 of abc is a9993e364706816aba3e25717850c26c9cd0d89d',
      );
      await expect(log).toContainText(
        'crypto: sha-256 of abc is ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
      );
      await expect(log).toContainText('crypto: md5 of abc is 900150983cd24fb0d6963f7d28e17f72');
      await expect(log).toContainText(
        'crypto: sha-1 of nothing is da39a3ee5e6b4b0d3255bfef95601890afd80709',
      );
      await expect(log).toContainText(
        'crypto: sha-1 of a million a\'s is 34aa973cd4c4daa4f61eeb2bdbad27316534016f',
      );
      await expect(log).toContainText('crypto: a digest fed one byte at a time matches true');
      expect(failures).toEqual([]);
    });

    test('runs the aes stream both ways and agrees on a key over rsa', async ({ page }) => {
      await open(page, backend, 'program=crypto');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('crypto: the first aes keystream byte is 69');
      await expect(log).toContainText(
        "crypto: a split stream still reads back 'a line the server would read'",
      );
      await expect(log).toContainText(
        'crypto: a 1024 bit key writes itself as 162 X.509 bytes and reads back true',
      );
      await expect(log).toContainText('crypto: the shared key arrives whole true in 128 sealed bytes');
      await expect(log).toContainText(
        'crypto: a signature checks out true and a changed message does not true',
      );
      await expect(log).toContainText('crypto: a datagram socket is refused by name');
      await expect(log).toContainText('crypto: java serialisation is refused by name');
    });
  });
}
