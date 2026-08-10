import { test, expect, type Page } from '@playwright/test';
import {
  BACKENDS,
  expectColor,
  expectChanging,
  expectColorSoft,
  open,
  readPixel,
  readPixels,
  watchForFailures,
  type Backend,
} from './harness';

async function runDemo(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=lwjgl');
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
}

for (const backend of BACKENDS) {
  test.describe(`lwjgl rendering on ${backend}`, () => {
    test('draws through the compatibility layer into a webgl2 canvas', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await runDemo(page, backend);

      const log = page.getByTestId('log');
      await expect(log).toContainText('display created at 640x360');

      expectColor(await readPixel(page, 320, 180), [0, 204, 51], 'center panel');
      expectColor(await readPixel(page, 5, 5), [26, 38, 77], 'clear color');

      expect(failures).toEqual([]);
    });


    test('answers the queries a program checks its context with', async ({ page }) => {
      await open(page, backend, 'program=lwjgl&arg=--frames&arg=90');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

      const log = page.getByTestId('log');
      await expect(log).toContainText(/gl WebGL .+ textures up to \d+/);
      await expect(log).toContainText('center pixel 0,204,51');
      await expect(log).toContainText('no gl errors');
    });

    test('draws each fixed-function feature the demo exercises', async ({ page }) => {
      await runDemo(page, backend);

      const green = [0, 204, 51];
      const clear = [26, 38, 77];
      const checks: [number, number, number[], string][] = [

        [165, 95, green, 'quad bottom-left corner'],
        [475, 95, green, 'quad bottom-right corner'],
        [165, 265, green, 'quad top-left corner'],
        [475, 265, green, 'quad top-right corner'],
        [155, 180, clear, 'left of the panel'],
        [485, 180, clear, 'right of the panel'],

        [550, 320, [230, 230, 26], 'inside the scissor box'],
        [550, 280, clear, 'below the scissor box'],
        [320, 180, green, 'the panel the scissor box spared'],

        [50, 320, clear, 'the culled quad'],
        [130, 320, [230, 0, 230], 'the quad left facing forward'],

        [240, 240, [0, 204, 204], 'the display list quad, recorded before the loop'],
        [240, 210, green, 'below the display list quad, the panel'],

        [320, 50, [51, 102, 255], 'after the glPushAttrib pop'],

        [575, 245, [230, 128, 0], 'the quad the loaded matrix translated'],
        [545, 245, clear, 'left of the translated quad'],

        [420, 140, [255, 102, 0], 'inside the stencil mask'],
        [390, 140, green, 'outside the stencil mask, the panel'],

        [50, 80, [102, 51, 204], 'the quad lit by its normal and material'],
        [115, 80, [178, 178, 178], 'the quad faded into the fog by its distance'],

        [520, 240, [128, 128, 128], 'the quad blended against the GL14 constant colour'],

        [620, 240, [0, 255, 0], 'the capability quad'],

        [620, 190, [0, 153, 204], 'the gluProject and gluUnProject quad'],
      ];

      const seen = await readPixels(
        page,
        checks.map(([x, y]) => [x, y]),
      );
      checks.forEach(([, , expected, label], at) => expectColorSoft(seen[at], expected, label));
    });

    test('keeps presenting frames', async ({ page }) => {
      await runDemo(page, backend);

      await expectChanging(
        () =>
          page.evaluate(() => {
            const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement;
            const gl = canvas.getContext('webgl2')!;
            const pixels = new Uint8Array(640 * 4);
            gl.readPixels(0, 20, 640, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
            let signature = '';
            for (let x = 0; x < 640; x += 16) {
              signature += pixels[x * 4] > 128 ? '1' : '0';
            }
            return signature;
          }),
        'the animated marker never moved',
      );
    });

    test('reports the frame loop finishing', async ({ page }) => {
      await open(page, backend, 'program=lwjgl');
      await expect(page.getByTestId('log')).toContainText('frame 60', { timeout: 30_000 });
    });
  });
}
