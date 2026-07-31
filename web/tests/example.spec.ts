import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, open, readPixel, watchForFailures, type Backend } from './harness';

/** the application jar, compiled against the real lwjgl and never modified. */
async function runSpinner(page: Page, backend: Backend, frames = 600): Promise<void> {
  await open(page, backend, `bundle=example&program=spinner&arg=--frames&arg=${frames}`);
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
}

for (const backend of BACKENDS) {
  test.describe(`an application jar in the browser on ${backend}`, () => {
    test('boots a main() out of a jar built against real LWJGL', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await runSpinner(page, backend);

      // System.out from the application, not from anything Flagon wrote
      await expect(page.getByTestId('log')).toContainText('spinner: 640x360');
      expect(failures).toEqual([]);
    });

    test('renders the application into the canvas', async ({ page }) => {
      await runSpinner(page, backend);

      const background = await readPixel(page, 5, 5);
      expect(Math.abs(background[0] - 18)).toBeLessThanOrEqual(2);

      // the square is 140px across at the center of a 640x360 canvas, whatever its rotation
      const center = await readPixel(page, 320, 180);
      expect(center.slice(0, 3).some((channel) => channel > 40)).toBe(true);
    });

    test('drives the application with real key events', async ({ page }) => {
      await runSpinner(page, backend);

      const rotation = async (): Promise<string> =>
        page.evaluate(() => {
          const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement;
          const gl = canvas.getContext('webgl2')!;
          const pixels = new Uint8Array(640 * 4);
          gl.readPixels(0, 180, 640, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
          let signature = '';
          for (let x = 0; x < 640; x += 16) {
            signature += pixels[x * 4 + 1] > 60 ? '1' : '0';
          }
          return signature;
        });

      // arrow up raises the spin speed the application keeps in a field of its own
      await page.keyboard.press('ArrowUp');
      await page.keyboard.press('ArrowUp');

      const samples = new Set<string>();
      for (let attempt = 0; attempt < 10; attempt++) {
        samples.add(await rotation());
        await page.waitForTimeout(100);
      }
      expect(samples.size, 'the square never rotated').toBeGreaterThan(1);
    });

    test('reports the application exiting on its own terms', async ({ page }) => {
      await open(page, backend, 'bundle=example&program=spinner&arg=--frames&arg=30');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText('spinner: 30 frames');
      await expect(page.getByTestId('log')).toContainText('exit code 0');
    });
  });
}

test('names an unknown bundle rather than failing silently', async ({ page }) => {
  await page.goto('/?bundle=nope');

  await expect(page.getByTestId('state')).toHaveText('failed', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText("no bundle named 'nope'");
});
