import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, open, watchForFailures, type Backend } from './harness';

async function runDemo(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=files');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('files: done');
}

for (const backend of BACKENDS) {
  test.describe(`java.io on the browser filesystem on ${backend}`, () => {
    test('runs against the origin private file system, not a heap stand-in', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('file store: opfs');
      expect(failures).toEqual([]);
    });

    test('writes and reads a file back through streams', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('wrote /flagon/notes.txt (18 bytes)');
      await expect(page.getByTestId('log')).toContainText('read back: hello from java.io');
    });

    test('overwrites in place through RandomAccessFile', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('after seek: hello java.io says');
    });

    test('creates, lists, renames and deletes', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('renamed: true');
      await expect(page.getByTestId('log')).toContainText('saves holds [kept.dat]');
      await expect(page.getByTestId('log')).toContainText('scratch is gone: true');
      await expect(page.getByTestId('log')).toContainText('deleted kept.dat: true');
    });

    test('keeps files across a reload', async ({ page }) => {
      await runDemo(page, backend);
      await expect(page.getByTestId('log')).toContainText('run 1');

      await page.reload();
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText('run 2');
    });
  });
}
