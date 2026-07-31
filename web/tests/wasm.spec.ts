import { test, expect } from '@playwright/test';
import { watchForFailures } from './harness';

test.describe('a bundle compiled to wasm gc', () => {
  test('boots the runtime and reports every phase', async ({ page }) => {
    const { failures } = watchForFailures(page);
    await page.goto('/?backend=wasm');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(page.getByTestId('phases').locator('li')).toHaveText([
      'platform: browser',
      'select: hello',
      'program: hello',
    ]);
    expect(failures).toEqual([]);
  });

  test('runs a java program and exits', async ({ page }) => {
    const { failures } = watchForFailures(page);
    await page.goto('/?backend=wasm&program=hello');

    const log = page.getByTestId('log');
    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(log).toContainText('Hello, world!');
    await expect(log).toContainText('exit code 0');
    expect(failures).toEqual([]);
  });

  test('passes arguments through to the program', async ({ page }) => {
    await page.goto('/?backend=wasm&program=hello&arg=flagon');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(page.getByTestId('log')).toContainText('Hello, flagon!');
  });

  test('keeps java semantics the javascript backend already keeps', async ({ page }) => {
    const { failures } = watchForFailures(page);
    await page.goto('/?backend=wasm&program=selftest');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(page.getByTestId('log')).toContainText(/selftest: (\d+)\/\1 checks passed/);
    expect(failures).toEqual([]);
  });

  test('starts the example bundle too', async ({ page }) => {
    const { failures } = watchForFailures(page);
    await page.goto('/?backend=wasm&bundle=example&program=spinner');

    await expect(page.getByTestId('state')).toHaveText('running', { timeout: 30_000 });
    expect(failures).toEqual([]);
  });

  test('reports a backend that does not exist', async ({ page }) => {
    await page.goto('/?backend=asm');

    await expect(page.getByTestId('state')).toHaveText('failed');
    await expect(page.getByTestId('log')).toContainText("no backend named 'asm'; try js, wasm");
  });
});
