import { test, expect, type Page } from '@playwright/test';

async function runDemo(page: Page): Promise<void> {
  await page.goto('/?program=files');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('files: done');
}

test.describe('java.io on the browser filesystem', () => {
  test('runs against the origin private file system, not a heap stand-in', async ({ page }) => {
    const failures: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        failures.push(message.text());
      }
    });
    page.on('pageerror', (error) => failures.push(error.message));

    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('file store: opfs');
    expect(failures).toEqual([]);
  });

  test('writes and reads a file back through streams', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('wrote /flagon/notes.txt (18 bytes)');
    await expect(page.getByTestId('log')).toContainText('read back: hello from java.io');
  });

  /** a whole-file store still has to make a seek look like a seek. */
  test('overwrites in place through RandomAccessFile', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('after seek: hello java.io says');
  });

  test('creates, lists, renames and deletes', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('renamed: true');
    await expect(page.getByTestId('log')).toContainText('saves holds [kept.dat]');
    await expect(page.getByTestId('log')).toContainText('scratch is gone: true');
    await expect(page.getByTestId('log')).toContainText('deleted kept.dat: true');
  });

  /** the point of OPFS over the in-memory default: the file is still there next load. */
  test('keeps files across a reload', async ({ page }) => {
    await runDemo(page);
    await expect(page.getByTestId('log')).toContainText('run 1');

    await page.reload();
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(page.getByTestId('log')).toContainText('run 2');
  });
});
