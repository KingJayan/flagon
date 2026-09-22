import { expect, type Page } from '@playwright/test';

/** every backend the shell can run a bundle on; specs that care run once per entry. */
export const BACKENDS = ['js', 'wasm'] as const;

export type Backend = (typeof BACKENDS)[number];

/** opens a program on one backend. `query` is the rest of the shell's url parameters. */
export async function open(page: Page, backend: Backend, query: string): Promise<void> {
  // the frame is read back from a later task
  await page.goto(`/?preserve=1&backend=${backend}${query ? `&${query}` : ''}`);
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
    return wanted.map(([px, py]) => {
      const pixel = new Uint8Array(4);
      gl.readPixels(px, py, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel);
      return Array.from(pixel);
    });
  }, points);
}

/** reads one pixel straight out of the live webgl2 framebuffer. */
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

/** allows for the small differences between float shader output and 8-bit framebuffers. */
export function expectColor(actual: number[], expected: number[], label: string): void {
  checkColor(expect, actual, expected, label);
}

export function expectColorSoft(actual: number[], expected: number[], label: string): void {
  checkColor(expect.soft, actual, expected, label);
}

/**
 * how many options a bundle's picker should have: one for every program the build registered,
 * plus the empty one at the top. reading the catalog rather than writing a number down keeps
 * this from going stale every time a program is added.
 */
export async function programCount(bundle: string): Promise<number> {
  const response = await fetch(`http://localhost:5273/flagon/${bundle}.catalog.json`);
  if (!response.ok) {
    throw new Error(`no catalog for ${bundle}: run 'make build'`);
  }
  const programs = (await response.json()) as unknown[];
  return programs.length + 1;
}
