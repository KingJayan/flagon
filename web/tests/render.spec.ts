import { test, expect, type Page } from '@playwright/test';

/**
 * reads one pixel straight out of the live webgl2 framebuffer,
 * sees exactly what was rendered rather than a re-encoded screenshot
 */
async function readPixel(page: Page, x: number, y: number): Promise<number[]> {
  return page.evaluate(
    ([px, py]) => {
      const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement | null;
      if (!canvas) {
        throw new Error('no canvas on the page');
      }
      const gl = canvas.getContext('webgl2');
      if (!gl) {
        throw new Error('no webgl2 context');
      }
      const pixel = new Uint8Array(4);
      gl.readPixels(px, py, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel);
      return Array.from(pixel);
    },
    [x, y],
  );
}

/** allows for the small differences between float shader output and 8-bit framebuffers. */
function expectColor(actual: number[], expected: number[], label: string): void {
  for (let channel = 0; channel < 3; channel++) {
    expect(
      Math.abs(actual[channel] - expected[channel]),
      `${label}: channel ${channel} was ${actual} expected ~${expected}`,
    ).toBeLessThanOrEqual(2);
  }
}

test.describe('lwjgl rendering', () => {
  test('draws through the compatibility layer into a webgl2 canvas', async ({ page }) => {
    const failures: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        failures.push(message.text());
      }
    });
    page.on('pageerror', (error) => failures.push(error.message));

    await page.goto('/?program=lwjgl');
    await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
    await expect(page.getByTestId('log')).toContainText('display created at 640x360');

    expectColor(await readPixel(page, 320, 180), [0, 204, 51], 'centre panel');

    expectColor(await readPixel(page, 5, 5), [26, 38, 77], 'clear colour');

    expect(failures).toEqual([]);
  });

  test('keeps presenting frames', async ({ page }) => {
    await page.goto('/?program=lwjgl');
    await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });

    const samples = new Set<string>();
    for (let attempt = 0; attempt < 12; attempt++) {
      const row = await page.evaluate(() => {
        const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement;
        const gl = canvas.getContext('webgl2')!;
        const pixels = new Uint8Array(640 * 4);
        gl.readPixels(0, 20, 640, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
        let signature = '';
        for (let x = 0; x < 640; x += 16) {
          signature += pixels[x * 4] > 128 ? '1' : '0';
        }
        return signature;
      });
      samples.add(row);
      await page.waitForTimeout(120);
    }

    expect(samples.size, 'the animated marker never moved').toBeGreaterThan(1);
  });

  test('reports the frame loop finishing', async ({ page }) => {
    await page.goto('/?program=lwjgl');
    await expect(page.getByTestId('log')).toContainText('frame 60', { timeout: 30_000 });
  });
});
