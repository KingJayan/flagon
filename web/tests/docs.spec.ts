import { test, expect } from '@playwright/test';

const PAGES = ['start', 'packaging', 'compatibility', 'architecture', 'api'];

test.describe('the docs', () => {
  test('render every page with working links', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (error) => errors.push(error.message));
    const broken: string[] = [];
    for (const id of PAGES) {
      await page.goto(`/docs/?page=${id}`);
      await expect(page.locator('#doc h1')).toHaveCount(1);
      await expect(page.locator(`#pages a[href="?page=${id}"]`)).toHaveAttribute('aria-current', 'page');
      const jumps = await page.locator('#doc a[href^="#"], #pages a[href^="#"]').evaluateAll((anchors) =>
        anchors.map((anchor) => anchor.getAttribute('href')!.slice(1)),
      );
      for (const jump of jumps) {
        if ((await page.locator(`[id="${jump}"]`).count()) === 0) {
          broken.push(`${id}#${jump}`);
        }
      }
    }
    expect(broken).toEqual([]);
    expect(errors).toEqual([]);
  });

  test('links across pages land on a heading', async ({ page }) => {
    const targets = new Set<string>();
    for (const id of PAGES) {
      await page.goto(`/docs/?page=${id}`);
      const links = await page.locator('#doc a[href^="?page="]').evaluateAll((anchors) =>
        anchors.map((anchor) => anchor.getAttribute('href')!),
      );
      links.forEach((link) => targets.add(link));
    }
    const missing: string[] = [];
    for (const link of targets) {
      await page.goto(`/docs/${link}`);
      const hash = link.split('#')[1];
      if (hash && (await page.locator(`[id="${hash}"]`).count()) === 0) {
        missing.push(link);
      }
    }
    expect(missing).toEqual([]);
  });
});
