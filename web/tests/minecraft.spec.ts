// needs a display
// 1.8.9 boots to main menu
// runs only where vendor/ holds the game
import { test, expect } from '@playwright/test';

const GAME = '/play/?backend=wasm&program=main'
  + '&arg=--accessToken&arg=0&arg=--version&arg=1.8.9';

test('1.8.9 reaches the main menu', async ({ page, request, browserName }) => {
  test.skip(browserName !== 'chromium', 'the milestone is chromium');
  const catalog = await request.get('/flagon/app.catalog.json');
  const minecraft = catalog.ok() && (await catalog.text()).includes('net.minecraft.client.main.Main');
  test.skip(!minecraft, "no 1.8.9 app bundle; put its jars in vendor/ and run 'make app'");
  test.setTimeout(180_000);

  const logged: string[] = [];
  page.on('console', (message) => logged.push(message.text()));
  await page.goto(GAME);

  await expect.poll(() => logged.some((line) => line.includes('Sound engine started')), {
    timeout: 120_000,
  }).toBe(true);
  // the menu draws its panorama and buttons for a while without the game giving up
  await page.waitForTimeout(10_000);
  expect(logged.filter((line) => line.includes('Game crashed'))).toEqual([]);
  await expect(page.getByTestId('headline')).not.toHaveText('The program exited');

  // the logo, buttons and text, not one flat colour
  const shot = (await page.locator('#flagon-canvas').screenshot()).toString('base64');
  const shades = await page.evaluate(async (png) => {
    const image = new Image();
    image.src = `data:image/png;base64,${png}`;
    await image.decode();
    const copy = document.createElement('canvas');
    copy.width = 64;
    copy.height = 36;
    const context = copy.getContext('2d')!;
    context.drawImage(image, 0, 0, 64, 36);
    const pixels = context.getImageData(0, 0, 64, 36).data;
    const seen = new Set<string>();
    for (let at = 0; at < pixels.length; at += 4) {
      seen.add(`${pixels[at] >> 4},${pixels[at + 1] >> 4},${pixels[at + 2] >> 4}`);
    }
    return seen.size;
  }, shot);
  expect(shades).toBeGreaterThan(20);
});
