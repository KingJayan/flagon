import { expect, type Page } from '@playwright/test';

export const BACKENDS = ['js', 'wasm'] as const;

export type Backend = (typeof BACKENDS)[number];

export async function open(page: Page, backend: Backend, query: string): Promise<void> {
  await page.goto(`/?preserve=1&backend=${backend}${query ? `&${query}` : ''}`);
}

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

export async function readPixels(page: Page, points: number[][]): Promise<number[][]> {
  return page.evaluate((wanted) => {
    const canvas = document.getElementById('flagon-canvas') as HTMLCanvasElement | null;
    if (!canvas) {
      throw new Error('no canvas on the page');
    }
    const gl = canvas.getContext('webgl2');
    if (!gl) {
      throw new Error('no webgl2 context');
    }
    // what the canvas shows, not the back buffer the program is drawing into
    const drawing = gl.getParameter(gl.READ_FRAMEBUFFER_BINDING);
    gl.bindFramebuffer(gl.READ_FRAMEBUFFER, null);
    const pixels = wanted.map(([px, py]) => {
      const pixel = new Uint8Array(4);
      gl.readPixels(px, py, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel);
      return Array.from(pixel);
    });
    gl.bindFramebuffer(gl.READ_FRAMEBUFFER, drawing);
    return pixels;
  }, points);
}

export async function readPixel(page: Page, x: number, y: number): Promise<number[]> {
  return (await readPixels(page, [[x, y]]))[0];
}

export async function expectChanging<T>(sample: () => Promise<T>, message: string): Promise<void> {
  const seen = new Set<T>();
  await expect
    .poll(
      async () => {
        seen.add(await sample());
        return seen.size;
      },
      { message, intervals: [50, 50, 100, 100, 200, 200, 400], timeout: 15_000 },
    )
    .toBeGreaterThan(1);
}

const TOLERANCE = 2;

function checkColor(
  assert: typeof expect | typeof expect.soft,
  actual: number[],
  expected: number[],
  label: string,
): void {
  for (let channel = 0; channel < 3; channel++) {
    assert(
      Math.abs(actual[channel] - expected[channel]),
      `${label}: channel ${channel} was ${actual} expected ~${expected}`,
    ).toBeLessThanOrEqual(TOLERANCE);
  }
}

export function expectColor(actual: number[], expected: number[], label: string): void {
  checkColor(expect, actual, expected, label);
}

export function expectColorSoft(actual: number[], expected: number[], label: string): void {
  checkColor(expect.soft, actual, expected, label);
}

export async function programCount(bundle: string): Promise<number> {
  const response = await fetch(`http://127.0.0.1:5273/flagon/${bundle}.catalog.json`);
  if (!response.ok) {
    throw new Error(`no catalog for ${bundle}: run 'make bundles'`);
  }
  const programs = (await response.json()) as unknown[];
  return programs.length + 1;
}
