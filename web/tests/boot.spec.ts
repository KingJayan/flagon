import { test, expect } from '@playwright/test';
import { BACKENDS, open, watchForFailures } from './harness';

for (const backend of BACKENDS) {
  test.describe(`booting a bundle on ${backend}`, () => {
    test('boots the runtime and reports every phase', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, '');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('phases').locator('li')).toHaveText([
        'platform: browser',
        'select: hello',
        'program: hello',
      ]);
      expect(failures).toEqual([]);
    });

    test('runs a simple java program', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=hello');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('Hello, world!');
      await expect(log).toContainText('exit code 0');
      expect(failures).toEqual([]);
    });

    test('passes arguments through to the program', async ({ page }) => {
      await open(page, backend, 'program=hello&arg=flagon');

      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(page.getByTestId('log')).toContainText('Hello, flagon!');
    });

    test('java semantics survive compilation to the browser', async ({ page }) => {
      const { failures } = watchForFailures(page);
      await open(page, backend, 'program=selftest&arg=alpha&arg=beta');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).not.toContainText('selftest failure');

      await expect(log).toContainText('arguments: [alpha,beta]');

      const summary = await log.textContent();
      const match = /selftest: (\d+)\/(\d+) checks passed/.exec(summary ?? '');
      expect(match, `no selftest summary in log:\n${summary}`).not.toBeNull();
      expect(match![1]).toBe(match![2]);
      expect(Number(match![2])).toBeGreaterThanOrEqual(106);
      expect(failures).toEqual([]);
    });

    test('reflects over the members of opted-in classes only', async ({ page }) => {
      await open(page, backend, 'program=selftest');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('reflection: Square reports 0 methods and 0 fields');
      await expect(log).toContainText('reflection: Reflected reports 2 methods and 2 fields');
      await expect(log).not.toContainText('selftest failure');
    });

    test('reads what a class file says beyond its members', async ({ page }) => {
      await open(page, backend, 'program=selftest');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText(
        'reflection: Point has 2 components and Box extends java.util.ArrayList<T>',
      );
      await expect(log).not.toContainText('selftest failure');
    });

    test('cannot take a class that arrives after the compile', async ({ page }) => {
      await open(page, backend, 'program=selftest');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText('selftest: bytecode arriving at runtime is impossible here');
      await expect(log).not.toContainText('selftest failure');
    });

    test('keeps the constructors of a class the build listed', async ({ page }) => {
      await open(page, backend, 'program=selftest');

      const log = page.getByTestId('log');
      await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
      await expect(log).toContainText(
        'reflection: ConstructedByName reports 2 constructors and 0 methods',
      );
      await expect(log).toContainText('reflection: Proxied stands in, saying greet world');
      await expect(log).not.toContainText('selftest failure');
    });

    test('reports an unknown program as a fatal boot failure', async ({ page }) => {
      await open(page, backend, 'program=does-not-exist');

      await expect(page.getByTestId('state')).toHaveText('failed', { timeout: 30_000 });
      const log = page.getByTestId('log');
      await expect(log).toContainText('fatal in select');
      await expect(log).toContainText("unknown program 'does-not-exist'");
      await expect(log).toContainText('available: hello, chatty, selftest');
    });

    test('still runs when the page installs no host bridge', async ({ page }) => {
      const lines: string[] = [];
      page.on('console', (message) => lines.push(`${message.type()}:${message.text()}`));
      const crashes: string[] = [];
      page.on('pageerror', (error) => crashes.push(error.message));

      await page.goto(`/bare.html?backend=${backend}`);
      await expect
        .poll(() => lines.some((line) => line.includes('Hello, bare!')), { timeout: 30_000 })
        .toBe(true);

      expect(lines.some((line) => line.includes('no window.__flagonHost installed'))).toBe(true);
      expect(crashes).toEqual([]);
    });
  });
}

test('reports a backend that does not exist', async ({ page }) => {
  await page.goto('/?backend=asm');

  await expect(page.getByTestId('state')).toHaveText('failed');
  await expect(page.getByTestId('log')).toContainText("no backend named 'asm'; try js, wasm");
});

test('runs on wasm gc when the browser has it', async ({ page }) => {
  const { failures } = watchForFailures(page);
  await page.goto('/?program=hello');

  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.locator('#backend')).toHaveValue('wasm');
  await expect(page.getByTestId('log')).toContainText('Hello, world!');
  expect(failures).toEqual([]);
});

test.describe('a browser without wasm gc', () => {
  test.beforeEach(async ({ page }) => {
    // the only thing the shell asks the engine, so refusing it is the whole condition
    await page.addInitScript(() => {
      WebAssembly.validate = () => false;
    });
  });

  test('falls back to the javascript bundle and says why', async ({ page }) => {
    const { failures } = watchForFailures(page);
    await page.goto('/?program=hello');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(page.locator('#backend')).toHaveValue('js');
    await expect(page.getByTestId('log')).toContainText('WASM-GC is unavailable');
    await expect(page.getByTestId('log')).toContainText('Hello, world!');
    expect(failures).toEqual([]);
  });

  test('refuses a wasm bundle it was asked for outright', async ({ page }) => {
    await page.goto('/?backend=wasm&program=hello');

    await expect(page.getByTestId('state')).toHaveText('failed', { timeout: 30_000 });
    await expect(page.getByTestId('log')).toContainText('no WASM-GC support');
  });
});

test('runs the jspi build when the browser has jspi', async ({ page }) => {
  const fetched: string[] = [];
  page.on('request', (request) => fetched.push(new URL(request.url()).pathname));
  await open(page, 'wasm', 'program=selftest');

  await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
  await expect(page.getByTestId('log')).not.toContainText('selftest failure');
  expect(fetched).toContain('/flagon/jspi/sandbox.wasm');
  expect(fetched).not.toContain('/flagon/wasm/sandbox.wasm');
});

test.describe('a browser without jspi', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      Object.defineProperty(WebAssembly, 'Suspending', { value: undefined });
    });
  });

  test('runs the coroutine build and keeps java semantics', async ({ page }) => {
    const fetched: string[] = [];
    page.on('request', (request) => fetched.push(new URL(request.url()).pathname));
    await open(page, 'wasm', 'program=selftest');

    await expect(page.getByTestId('state')).toHaveText('exited', { timeout: 30_000 });
    await expect(page.getByTestId('log')).not.toContainText('selftest failure');
    expect(fetched).toContain('/flagon/wasm/sandbox.wasm');
    expect(fetched).not.toContain('/flagon/jspi/sandbox.wasm');
  });
});
