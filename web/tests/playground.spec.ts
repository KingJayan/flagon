import { test, expect, type Page } from '@playwright/test';

test.describe.configure({ timeout: 90_000 });

/** replaces the editor's text the way a paste does */
async function paste(page: Page, source: string): Promise<void> {
  await page.locator('#code').evaluate((code, text) => {
    code.focus();
    getSelection()!.selectAllChildren(code);
    const data = new DataTransfer();
    data.setData('text/plain', text);
    const paste = new ClipboardEvent('paste', { cancelable: true });
    // firefox drops clipboardData passed to the constructor
    Object.defineProperty(paste, 'clipboardData', { value: data });
    code.dispatchEvent(paste);
  }, source);
}

async function write(page: Page, source: string): Promise<void> {
  await page.goto('/playground/');
  await expect(page.getByTestId('state')).toHaveText('ready', { timeout: 60_000 });
  await paste(page, source);
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
    expect(await page.evaluate(() => getSelection()!.toString())).toBe('        int x = "text";');
  });

  test('highlights java and completes words', async ({ page }) => {
    await page.goto('/playground/');
    const code = page.locator('#code');
    await paste(page, 'public class Main {\n    // note\n    String text = "hi";\n}');
    await page.keyboard.press('Enter');
    await expect(code.locator('.keyword').first()).toHaveText('public');
    await expect(code.locator('.comment')).toHaveText('// note');
    await expect(code.locator('.string')).toHaveText('"hi"');
    await expect(code.locator('.type').first()).toHaveText('Main');

    await page.keyboard.type('Sys');
    const listed = page.locator('#completions [role="option"]');
    await expect(listed.first()).toHaveText('System');
    await page.keyboard.press('Enter');
    await expect(page.locator('#completions')).toBeHidden();
    await expect(code).toHaveText(/\}\nSystem$/);
    expect(await page.evaluate(() => localStorage.getItem('flagon:playground:source'))).toMatch(/System$/);

    await page.keyboard.press('Enter');
    await page.keyboard.press('Tab');
    await page.keyboard.type('x');
    await expect(code).toHaveText(/\nSystem\n {4}x\n?$/);
  });

  test('stops a program that never ends', async ({ page }) => {
    await write(page, 'public class Main {\n    public static void main(String[] args) {\n        while (true) {\n        }\n    }\n}\n');

    await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
    await page.locator('#stop').click();
    await expect(page.getByTestId('state')).toHaveText('exited');
    await expect(page.getByTestId('log')).toContainText('stopped');
  });
});
