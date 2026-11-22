import { test, expect, type Page } from '@playwright/test';

test.describe.configure({ timeout: 90_000 });

async function write(page: Page, source: string): Promise<void> {
  await page.goto('/playground/');
  await expect(page.getByTestId('state')).toHaveText('ready', { timeout: 60_000 });
  await page.locator('#code').fill(source);
  await page.locator('#run').click();
}

test.describe('the playground', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear());
  });

  test('runs a program until its threads finish', async ({ page }) => {
    await write(
      page,
      `public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException stopped) {
                return;
            }
            System.out.println("worker done");
        });
        worker.start();
        System.out.println("main returns");
    }
}
`,
    );

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(page.getByTestId('log')).toHaveText('main returns\nworker done\n');
  });

  test('points at the line a compile error is on', async ({ page }) => {
    await write(page, 'public class Main {\n    public static void main(String[] args) {\n        int x = "text";\n    }\n}\n');

    await expect(page.getByTestId('state')).toHaveText('failed', { timeout: 30_000 });
    const problem = page.locator('#problems button');
    await expect(problem).toHaveCount(1);
    await expect(problem).toContainText('3:');
    await problem.click();
    const selected = await page
      .locator('#code')
      .evaluate((area: HTMLTextAreaElement) => area.value.slice(area.selectionStart, area.selectionEnd));
    expect(selected).toBe('        int x = "text";');
  });

  test('stops a program that never ends', async ({ page }) => {
    await write(page, 'public class Main {\n    public static void main(String[] args) {\n        while (true) {\n        }\n    }\n}\n');

    await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
    await page.locator('#stop').click();
    await expect(page.getByTestId('state')).toHaveText('exited');
    await expect(page.getByTestId('log')).toContainText('stopped');
  });
});
