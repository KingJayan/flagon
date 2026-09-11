// needs a display
import { test, expect } from '@playwright/test';
import { BACKENDS } from './harness';

for (const backend of BACKENDS) {
  test.describe(`playing a bundle on ${backend}`, () => {
    test('runs a program with the viewport to itself', async ({ page }) => {
      const logged: string[] = [];
      page.on('console', (message) => logged.push(message.text()));

      await page.goto(`/play/?bundle=sandbox&backend=${backend}&program=hello`);

      await expect(page.getByTestId('headline')).toHaveText('The program exited', {
        timeout: 30_000,
      });
      // the program's own output has nowhere on the page to go, so it goes to the console
      expect(logged.some((line) => line.includes('Hello, world!'))).toBe(true);

      const canvas = page.locator('#flagon-canvas');
      const box = (await canvas.boundingBox())!;
      const viewport = page.viewportSize()!;
      expect(box.width).toBe(viewport.width);
      expect(box.height).toBe(viewport.height);
    });

    test('shows nothing over the program once it is running', async ({ page }) => {
      await page.goto(`/play/?bundle=example&backend=${backend}&program=spinner&arg=--frames&arg=90`);

      await expect(page.getByTestId('veil')).toBeHidden({ timeout: 30_000 });
      await expect(page.locator('#flagon-canvas')).toBeFocused();
    });
  });
}

test('says how to make the app bundle when there is none', async ({ page }) => {
  await page.route('**/flagon/app.*', (route) => route.fulfill({ status: 404 }));
  await page.goto('/play/');

  await expect(page.getByTestId('headline')).toHaveText('Nothing to run', { timeout: 30_000 });
  await expect(page.getByTestId('detail')).toContainText('Put a jar in vendor/');
});

test('runs the bundle built from a jar in vendor/', async ({ page, request }) => {
  // a served page is not a bundle: the server answers an unknown path with html
  const built = await request.head('/flagon/app.js');
  const html = built.headers()['content-type']?.includes('text/html') ?? false;
  test.skip(!built.ok() || html, "no app bundle — put a jar in vendor/ and run 'make app'");

  const logged: string[] = [];
  page.on('console', (message) => logged.push(message.text()));
  await page.goto('/play/?backend=js');

  await expect(page.getByTestId('veil')).toBeHidden({ timeout: 30_000 });
  expect(logged.some((line) => line.includes('platform'))).toBe(true);
});

test('names the bundles it will run when asked for one it will not', async ({ page }) => {
  await page.goto('/play/?bundle=nonesuch');

  await expect(page.getByTestId('headline')).toHaveText('Nothing to run');
  await expect(page.getByTestId('detail')).toContainText('sandbox, example, app');
});

test('reports a backend that does not exist', async ({ page }) => {
  await page.goto('/play/?bundle=sandbox&backend=asm');

  await expect(page.getByTestId('headline')).toHaveText('Nothing to run');
  await expect(page.getByTestId('detail')).toContainText('js, wasm');
});

test.describe('the two pages read one url', () => {
  const asked = 'bundle=nope&backend=js';

  test('and refuse an unknown bundle with the same words', async ({ page }) => {
    await page.goto(`/?${asked}`);
    await expect(page.getByTestId('log')).toContainText("no bundle named 'nope'");

    await page.goto(`/play/?${asked}`);
    await expect(page.getByTestId('detail')).toContainText("no bundle named 'nope'");
  });

  test('and offer the same bundles, so neither can drift from the other', async ({ page }) => {
    await page.goto('/');
    const offered = await page.locator('#bundle option').evaluateAll((nodes) =>
      nodes.map((node) => (node as HTMLOptionElement).value),
    );

    for (const bundle of offered) {
      await page.goto(`/play/?bundle=${bundle}&backend=js`);
      await expect(page.getByTestId('detail')).not.toContainText('no bundle named');
    }
  });
});
