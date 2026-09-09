import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`glfw windowing on ${backend}`, () => {
    test('opens a window and runs a frame loop through it', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=glfw');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('glfw: init true');
      await expect(log).toContainText('glfw: framebuffer 640x360');
      await expect(log).toContainText('glfw: monitor 640x360 at 60Hz');
      await expect(log).toContainText('glfw: drew 30 frames');
      await expect(log).toContainText('glfw: done');
      expect(failures).toEqual([]);
    });

    test('draws to the canvas the window opened', async ({ page }) => {
      await open(page, backend, 'program=glfw');

      await expect(page.locator('canvas')).toBeVisible();
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    });

    test('reports keys in glfw numbering', async ({ page }) => {
      await open(page, backend, 'program=glfw');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('glfw: escape is up');
      await expect(log).toContainText('glfw: the clock reached forward');
    });
  });
}
