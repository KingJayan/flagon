import { test, expect, type Page } from '@playwright/test';

async function runDemo(page: Page): Promise<void> {
  await page.goto('/?program=audio');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('audio: done');
}

test.describe('OpenAL on web audio', () => {
  test('runs against a live web audio context, not a silent stand-in', async ({ page }) => {
    const failures: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        failures.push(message.text());
      }
    });
    page.on('pageerror', (error) => failures.push(error.message));

    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('audio device: webaudio');
    await expect(page.getByTestId('log')).toContainText('output live: true');
    expect(failures).toEqual([]);
  });

  test('takes raw pcm and reports it back', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('buffered 11024 bytes at 22050 Hz');
  });

  /** the mixer decides when a source is done */
  test('plays a source through to its end', async ({ page }) => {
    await runDemo(page);

    const log = await page.getByTestId('log').textContent();
    expect(log).toContain('source state: playing');

    const ended = /source state: stopped after (\d+) ms/.exec(log ?? '');
    expect(ended, 'the source never stopped').not.toBeNull();
    const millis = Number(ended![1]);
    expect(millis).toBeGreaterThanOrEqual(200);
    expect(millis).toBeLessThan(2_000);
  });

  test('reports a bad name through alGetError rather than throwing', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('missing source reports AL_INVALID_NAME');
    await expect(page.getByTestId('log')).toContainText('error cleared: true');
  });

  test('releases its buffers and sources', async ({ page }) => {
    await runDemo(page);

    await expect(page.getByTestId('log')).toContainText('released: true true');
    await expect(page.getByTestId('log')).toContainText('teardown: no error');
  });
});
