import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, open, watchForFailures, type Backend } from './harness';

async function runDemo(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=audio');
  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('audio: done');
}

for (const backend of BACKENDS) {
  test.describe(`OpenAL on web audio on ${backend}`, () => {
    test('runs against a live web audio context, not a silent stand-in', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('audio device: webaudio');
      await expect(page.getByTestId('log')).toContainText('output live: true');
      expect(failures).toEqual([]);
    });

    test('shifts a moving source, since web audio has no doppler of its own', async ({ page }) => {
      await open(page, backend, 'program=audio');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('doppler: still 1.0');
      await expect(log).toContainText('doppler: approaching 1.1');
      await expect(log).toContainText('doppler: receding 0.91');
      await expect(log).toContainText('doppler: switched off 1.0');
      await expect(log).toContainText('doppler: no error');
    });

    test('keeps a source its rolloff when the model stops using it', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText(
        'distance model: true, rolloff kept: true',
      );
    });

    test('takes raw pcm and reports it back', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('buffered 11024 bytes at 22050 Hz');
    });

    /** the mixer decides when a source is done */
    test('plays a source through to its end', async ({ page }) => {
      await runDemo(page, backend);

      const log = await page.getByTestId('log').textContent();
      expect(log).toContain('source state: playing');

      const ended = /source state: stopped after (\d+) ms/.exec(log ?? '');
      expect(ended, 'the source never stopped').not.toBeNull();
      const millis = Number(ended![1]);
      expect(millis).toBeGreaterThanOrEqual(200);
      expect(millis).toBeLessThan(2_000);
    });

    /** music is buffers queued ahead, taken back as they play, and filled again */
    test('streams a queue of buffers and takes back what it has played', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('streamed: queued 2');
      await expect(page.getByTestId('log')).toContainText('streamed: took back true, queued 1');
      await expect(page.getByTestId('log')).toContainText('streamed: refilled to 2');
      await expect(page.getByTestId('log')).toContainText('streaming: no error');
    });

    test('opens its mixer through the context layer a game expects', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText(
        'alc: webaudio, current: true, no error: true',
      );
    });

    test('reads a wave file into a buffer', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('wave: mono16 true at 22050 Hz, 11024 bytes');
      await expect(page.getByTestId('log')).toContainText('wave: buffered 11024 bytes');
    });

    test('reports a bad name through alGetError rather than throwing', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('missing source reports AL_INVALID_NAME');
      await expect(page.getByTestId('log')).toContainText('error cleared: true');
    });

    test('releases its buffers and sources', async ({ page }) => {
      await runDemo(page, backend);

      await expect(page.getByTestId('log')).toContainText('released: true true');
      await expect(page.getByTestId('log')).toContainText('teardown: no error');
    });
  });
}
