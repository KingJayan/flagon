import { test, expect } from '@playwright/test';

// the webassembly loader sets up every string before a thread exists, and this bundle makes that
// setup code that can pause
test('loads a bundle whose string setup can pause', async ({ page }) => {
  const lines: string[] = [];
  const crashes: string[] = [];
  page.on('console', (message) => lines.push(message.text()));
  page.on('pageerror', (error) => crashes.push(error.message));
  await page.route('**/pausing.html', (route) =>
    route.fulfill({ contentType: 'text/html', body: '<!doctype html><title>pausing</title>' }),
  );
  await page.goto('/pausing.html');

  const loaded = await page.evaluate(async () => {
    try {
      const loader = '/flagon/wasm/wasm-gc-module-runtime.js';
      const { load } = (await import(loader)) as {
        load(path: string): Promise<{ exports: { main(args: string[]): void } }>;
      };
      const bundle = await load('/flagon/wasm/pausing.wasm');
      bundle.exports.main([]);
      return 'loaded';
    } catch (failure) {
      return String(failure);
    }
  });

  expect(loaded).toBe('loaded');
  await expect
    .poll(() => lines.some((line) => line.includes('two names with one hash stay apart true')), {
      timeout: 30_000,
    })
    .toBe(true);
  await expect
    .poll(() => lines.some((line) => line.includes('a deep pause comes back with 17 frames')), {
      timeout: 30_000,
    })
    .toBe(true);
  expect(crashes).toEqual([]);
});
