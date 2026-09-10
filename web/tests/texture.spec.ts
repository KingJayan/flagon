// needs a display
import { test, expect, type Page } from '@playwright/test';
import { BACKENDS, open, readPixel, expectColor, watchForFailures, type Backend } from './harness';

async function runDemo(page: Page, backend: Backend): Promise<void> {
  await open(page, backend, 'program=texture');
  await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
  await expect(page.getByTestId('log')).toContainText('uploaded texture');
}

for (const backend of BACKENDS) {
  test.describe(`textures on ${backend}`, () => {
    test('samples every texel of an uploaded image', async ({ page }) => {
      const { failures } = watchForFailures(page);

      await runDemo(page, backend);

      // the 2x2 texture, magnified: one quadrant of the quad per texel
      expectColor(await readPixel(page, 120, 260), [255, 0, 0], 'top left texel');
      expectColor(await readPixel(page, 280, 260), [0, 255, 0], 'top right texel');
      expectColor(await readPixel(page, 120, 120), [0, 0, 255], 'bottom left texel');
      expectColor(await readPixel(page, 280, 120), [255, 255, 0], 'bottom right texel');
      expect(failures).toEqual([]);
    });

    /** 0xAARRGGBB has to survive the trip to the typed array in that order. */
    test('keeps the channel order the pixels were given in', async ({ page }) => {
      await runDemo(page, backend);

      const red = await readPixel(page, 120, 260);
      expect(red[3], `top left alpha was ${red}`).toBe(255);

      // half-opaque white blended over the clear color
      expectColor(await readPixel(page, 560, 260), [135, 135, 141], 'faded quad');
    });

    test('modulates the texel by the vertex color', async ({ page }) => {
      await runDemo(page, backend);

      expectColor(await readPixel(page, 420, 260), [128, 0, 0], 'tinted quad');
    });

    /** GL_REPEAT: coordinates past 1 wrap round instead of smearing the edge texel. */
    test('tiles a texture whose coordinates run past its edge', async ({ page }) => {
      await runDemo(page, backend);

      expectColor(await readPixel(page, 630, 285), [255, 0, 0], 'first tile, top left texel');
      expectColor(await readPixel(page, 650, 285), [0, 255, 0], 'first tile, top right texel');
      expectColor(await readPixel(page, 630, 215), [0, 0, 255], 'first tile, bottom left texel');
      expectColor(await readPixel(page, 630, 145), [255, 0, 0], 'second tile, top left texel');
    });

    test('samples the mipmap level the minification asks for', async ({ page }) => {
      await runDemo(page, backend);

      expectColor(await readPixel(page, 750, 290), [255, 0, 0], 'the base level, drawn one to one');
      expectColor(await readPixel(page, 724, 206), [0, 0, 255], 'a lower level, minified');
    });

    test('combines two texture units through the environment mode each was given', async ({ page }) => {
      await runDemo(page, backend);

      expectColor(await readPixel(page, 830, 290), [128, 0, 0], 'unit 1 modulating unit 0');
      expectColor(await readPixel(page, 900, 290), [128, 128, 128], 'unit 1 replacing unit 0');
    });

    test('widens a texture uploaded in a narrower format', async ({ page }) => {
      await runDemo(page, backend);

      expectColor(await readPixel(page, 830, 180), [32, 160, 64], 'the rgb texture');
      expectColor(await readPixel(page, 900, 180), [128, 128, 128], 'the luminance texture');
    });

    /** GL_BGRA with the packed int type: what an application uploads its own images with */
    test('takes a texture uploaded as packed ints', async ({ page }) => {
      await runDemo(page, backend);

      expectColor(await readPixel(page, 830, 110), [32, 128, 192], 'the packed texture');
    });
  });
}
