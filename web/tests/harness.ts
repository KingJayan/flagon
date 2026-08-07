import { expect, type Page } from '@playwright/test';

/** every backend the shell can run a bundle on; specs that care run once per entry. */
export const BACKENDS = ['js', 'wasm'] as const;

export type Backend = (typeof BACKENDS)[number];

/** opens a program on one backend. `query` is the rest of the shell's url parameters. */
export async function open(page: Page, backend: Backend, query: string): Promise<void> {
  await page.goto(`/?backend=${backend}${query ? `&${query}` : ''}`);
}

/** collects anything the browser itself considers an error. */
export function watchForFailures(page: Page): { failures: string[] } {
  const failures: string[] = [];
  page.on('console', (message) => {
    if (message.type() === 'error' && !message.text().includes('Navigated away from page')) {
      failures.push(`console.error: ${message.text()}`);
    }
  });
  page.on('pageerror', (error) => failures.push(`pageerror: ${error.message}`));
  return { failures };
}

/** reads one pixel straight out of the live webgl2 framebuffer. */
export async function readPixel(page: Page, x: number, y: number): Promise<number[]> {
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
export function expectColor(actual: number[], expected: number[], label: string): void {
  for (let channel = 0; channel < 3; channel++) {
    expect(
      Math.abs(actual[channel] - expected[channel]),
      `${label}: channel ${channel} was ${actual} expected ~${expected}`,
    ).toBeLessThanOrEqual(2);
  }
}
