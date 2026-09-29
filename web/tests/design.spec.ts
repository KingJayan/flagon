// needs a display
import { test, expect } from '@playwright/test';
import { open } from './harness';

test.describe('the shell as a designed page', () => {
  test('wears the mark the readme leads with', async ({ page }) => {
    await open(page, 'js', 'program=hello');

    await expect(page.locator('link[rel=icon]')).toHaveAttribute('href', '/favicon.ico');
    const mark = page.locator('.mark');
    await expect(mark).toHaveAttribute('src', '/logo.png');
    expect(
      await mark.evaluate((image: HTMLImageElement) => image.naturalWidth),
    ).toBeGreaterThan(0);
  });

  test('gives the display more weight than the chrome around it', async ({ page }) => {
    await open(page, 'js', 'program=lwjgl');
    await expect(page.locator('#display')).toBeVisible({ timeout: 30_000 });

    // how far a surface is lifted off the page, whatever the shadow is spelled as
    const liftOf = (selector: string) =>
      page.locator(selector).evaluate((node) => {
        const shadow = getComputedStyle(node).boxShadow;
        if (shadow === 'none') {
          return 0;
        }
        return [...shadow.matchAll(/(-?[\d.]+)px/g)].reduce(
          (total, [, value]) => total + Math.abs(Number(value)),
          0,
        );
      });

    const stage = await liftOf('#display');
    expect(stage).toBeGreaterThan(await liftOf('.controls'));
    expect(stage).toBeGreaterThan(await liftOf('.console'));
  });

  test('sizes every text role from one scale', async ({ page }) => {
    await open(page, 'js', 'program=hello');
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });

    const { sizes, scale } = await page.evaluate(() => {
      const root = getComputedStyle(document.documentElement);
      const measure = (selector: string) =>
        getComputedStyle(document.querySelector(selector)!).fontSize;
      return {
        sizes: [
          'body',
          'h1',
          'h2',
          'label',
          '#run',
          '.badge',
          '.log',
          '.tagline',
          '.phases li',
        ].map(measure),
        // the scale is declared once, in custom properties, and this is that list
        scale: ['--small', '--detail', '--body', '--title'].map((name) => {
          const probe = document.createElement('span');
          probe.style.fontSize = root.getPropertyValue(name);
          document.body.append(probe);
          const size = getComputedStyle(probe).fontSize;
          probe.remove();
          return size;
        }),
      };
    });

    expect(scale).toHaveLength(new Set(scale).size);
    for (const size of sizes) {
      expect(scale, `${size} is not a step of the scale`).toContain(size);
    }
  });
});

test.describe('one design language across both pages', () => {
  const SHARED = ['--ink', '--muted', '--accent', '--bad', '--ok', '--line', '--serif', '--mono'];

  async function tokensOf(page: import('@playwright/test').Page, url: string) {
    await page.goto(url);
    return page.evaluate((names: string[]) => {
      const read = getComputedStyle(document.documentElement);
      return Object.fromEntries(names.map((name) => [name, read.getPropertyValue(name).trim()]));
    }, SHARED);
  }

  test('resolves every shared token on the shell and on /play/', async ({ page }) => {
    const shell = await tokensOf(page, '/');
    const play = await tokensOf(page, '/play/?bundle=nonesuch');

    for (const name of SHARED) {
      expect(shell[name], `the shell defines ${name}`).not.toBe('');
      expect(play[name], `/play/ defines ${name}`).not.toBe('');
    }
    expect(play['--serif']).toBe(shell['--serif']);
    expect(play['--mono']).toBe(shell['--mono']);
  });

  test('gives /play/ the dark end of the same palette, not a palette of its own', async ({
    page,
  }) => {
    await page.emulateMedia({ colorScheme: 'light' });
    const shell = await tokensOf(page, '/');
    const play = await tokensOf(page, '/play/?bundle=nonesuch');

    // the shell follows the reader; /play/ presents a program, so it stays dark either way
    expect(play['--ink']).not.toBe(shell['--ink']);

    await page.emulateMedia({ colorScheme: 'dark' });
    const shellAtNight = await tokensOf(page, '/');
    expect(play['--ink']).toBe(shellAtNight['--ink']);
    expect(play['--accent']).toBe(shellAtNight['--accent']);
  });

  test('leaves no token defined and never used', async ({ page }) => {
    await page.goto('/');
    const unused = await page.evaluate(async () => {
      const sheets = await Promise.all(
        [...document.querySelectorAll<HTMLLinkElement>('link[rel=stylesheet]')].map((link) =>
          fetch(link.href).then((response) => response.text()),
        ),
      );
      const css = sheets.join('\n');
      const defined = [...css.matchAll(/^\s*(--[\w-]+):/gm)].map((found) => found[1]);
      return defined.filter((name) => !new RegExp(`var\\(\\s*${name}\\b`).test(css));
    });

    expect(unused).toEqual([]);
  });
});
