import { test, expect, type Page } from '@playwright/test';
import {
  BACKENDS,
  expectChanging,
  open,
  readPixel,
  watchForFailures,
  type Backend,
} from './harness';

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

      await expectChanging(rotation, 'the square never rotated');
    });

    test('reports the application exiting on its own terms', async ({ page }) => {
      await open(page, backend, 'bundle=example&program=spinner&arg=--frames&arg=30');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText('spinner: 30 frames');
      await expect(page.getByTestId('log')).toContainText('exit code 0');
    });
  });
}

/** where the bright pixels of the paddle row average out to, in canvas pixels from the left */
async function paddlecenter(page: Page): Promise<number> {
  return page.evaluate(() => {
    const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement;
    const gl = canvas.getContext('webgl2')!;
    const row = new Uint8Array(640 * 4);
    gl.readPixels(0, 360 - 335, 640, 1, gl.RGBA, gl.UNSIGNED_BYTE, row);
    let sum = 0;
    let lit = 0;
    for (let x = 0; x < 640; x++) {
      if (row[x * 4] > 150) {
        sum += x;
        lit++;
      }
    }
    return lit === 0 ? -1 : Math.round(sum / lit);
  });
}

/** the leftmost bright pixel below the bricks, which is the ball unless the paddle is under it */
async function ballAt(page: Page): Promise<number> {
  return page.evaluate(() => {
    const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement;
    const gl = canvas.getContext('webgl2')!;
    const band = new Uint8Array(640 * 180 * 4);
    gl.readPixels(0, 40, 640, 180, gl.RGBA, gl.UNSIGNED_BYTE, band);
    for (let at = 0; at < band.length; at += 4) {
      if (band[at] > 150) {
        return at / 4;
      }
    }
    return -1;
  });
}

/** the same jar's other main: a whole game, with sound, a hud and a saved high score */
async function runBlocks(page: Page, backend: Backend, frames = 3600): Promise<void> {
  await open(page, backend, `bundle=example&program=blocks&arg=--frames&arg=${frames}`);
  await expect(page.getByTestId('log')).toContainText('blocks: ready 640x360', {
    timeout: 30_000,
  });
}

for (const backend of BACKENDS) {
  test.describe(`a complete game out of the example jar on ${backend}`, () => {
    test('opens a display, a keyboard and a mixer without complaint', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await runBlocks(page, backend);

      await expect(page.getByTestId('log')).toContainText('blocks: high score');
      expect(failures).toEqual([]);
    });

    test('draws the bricks, the paddle and the hud', async ({ page }) => {
      await runBlocks(page, backend);
      await page.waitForTimeout(300);

      // a brick in the top row, which is textured and lit
      const brick = await readPixel(page, 60, 360 - 54);
      expect(brick.slice(0, 3).some((channel) => channel > 60), `brick was ${brick}`).toBe(true);

      // the paddle, near the bottom, where nothing else is drawn
      const paddle = await readPixel(page, 320, 360 - 335);
      expect(paddle[0], `paddle was ${paddle}`).toBeGreaterThan(150);

      // the score, drawn in the top left out of little quads
      const hud = await readPixel(page, 13, 360 - 14);
      expect(hud[0], `hud was ${hud}`).toBeGreaterThan(150);
    });

    test('runs a simulation the player can watch', async ({ page }) => {
      await runBlocks(page, backend);

      await expectChanging(() => ballAt(page), 'nothing moved');
    });

    test('takes the paddle where the keyboard says', async ({ page }) => {
      await runBlocks(page, backend);

      const before = await paddlecenter(page);

      await page.keyboard.down('ArrowLeft');
      await page.waitForTimeout(400);
      await page.keyboard.up('ArrowLeft');

      const after = await paddlecenter(page);
      expect(after, `the paddle went from ${before} to ${after}`).toBeLessThan(before - 40);
    });

    /** the game stops simulating when the canvas is not the thing being typed at */
    test('pauses itself when it loses the keyboard', async ({ page }) => {
      await runBlocks(page, backend);
      const log = page.getByTestId('log');

      await page.evaluate(() => document.getElementById('flagon-canvas')!.blur());
      await expect(log).toContainText('blocks: paused', { timeout: 10_000 });

      await page.evaluate(() => document.getElementById('flagon-canvas')!.focus());
      await expect(log).toContainText('blocks: resumed', { timeout: 10_000 });
    });

    test('keeps a high score across a reload', async ({ page }) => {
      await runBlocks(page, backend, 300);
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 60_000 });
      await expect(page.getByTestId('log')).toContainText(/blocks: (saved )?high score/);

      await runBlocks(page, backend, 60);

      await expect(page.getByTestId('log')).toContainText(/blocks: high score [1-9]\d*/, {
        timeout: 30_000,
      });
    });
  });
}

test('names an unknown bundle rather than failing silently', async ({ page }) => {
  await page.goto('/?bundle=nope');

  await expect(page.getByTestId('state')).toHaveText('failed', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText("no bundle named 'nope'");
});
