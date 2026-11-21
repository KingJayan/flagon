import { test, expect } from '@playwright/test';

test.describe('the public site', () => {
  test('lists demos only and hides the workbench fields', async ({ page }) => {
    await page.goto('/?view=demos&backend=js');

    await expect(page.locator('label[for="program"]')).toHaveText('Demo');
    await expect(page.locator('#bundle')).toBeHidden();
    await expect(page.locator('#args')).toBeHidden();
    await expect(page.locator('#program')).toHaveValue('example/blocks');
    const listed = await page.locator('#program option').evaluateAll((options) =>
      options.map((option) => (option as HTMLOptionElement).value),
    );
    expect(listed).not.toContain('sandbox/selftest');
    expect(listed).not.toContain('sandbox/jdkgaps');
  });

  test('lists only programs the bundles carry', async ({ page }) => {
    await page.goto('/?view=demos&backend=js');
    const missing = await page.locator('#program option').evaluateAll(async (options) => {
      const gone: string[] = [];
      for (const option of options as HTMLOptionElement[]) {
        const [bundle, program] = option.value.split('/');
        const catalog = (await (await fetch(`/flagon/${bundle}.catalog.json`)).json()) as {
          id: string;
        }[];
        if (!catalog.some((entry) => entry.id === program)) {
          gone.push(option.value);
        }
      }
      return gone;
    });
    expect(missing).toEqual([]);
  });

  test('runs the demo it is given', async ({ page }) => {
    await page.goto('/?view=demos&backend=js');
    await page.locator('#program').selectOption('sandbox/files');
    await page.locator('#run').click();

    await expect(page).toHaveURL(/bundle=sandbox&backend=js&program=files&view=demos/);
    await expect(page.locator('#program')).toHaveValue('sandbox/files');
  });
});

test('the workbench lists every program of the bundle', async ({ page }) => {
  await page.goto('/?bundle=sandbox&backend=js&program=hello');

  await expect(page.locator('#bundle')).toBeVisible();
  await expect(page.locator('#program option[value="selftest"]')).toHaveCount(1);
});
