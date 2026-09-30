import { test, expect } from '@playwright/test';
import { readdirSync, statSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const BUNDLES = join(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'flagon');

const LOADER = 'wasm-gc-module-runtime.js';

function onDisk(): string[] {
  return ['js', 'wasm'].flatMap((backend) => {
    try {
      return readdirSync(join(BUNDLES, backend))
        .filter((name) => name !== LOADER && /\.(js|wasm)$/.test(name))
        .map((name) => `${backend}/${name}`);
    } catch {
      return [];
    }
  });
}

test('the server is serving the bundles that are on disk', async ({ request }) => {
  const names = onDisk();
  expect(names, `no bundles in ${BUNDLES} — run 'make bundles'`).not.toHaveLength(0);

  for (const name of names) {
    // identity, so a compressed response cannot be mistaken for a different bundle
    const response = await request.head(`/flagon/${name}`, {
      headers: { 'accept-encoding': 'identity' },
    });
    expect(response.status(), `/flagon/${name} is not being served`).toBe(200);

    const served = Number(response.headers()['content-length']);
    const built = statSync(join(BUNDLES, name)).size;
    expect(
      served,
      `/flagon/${name} is served as ${served} bytes but is ${built} on disk. Something else is `
        + `answering on this port, or the bundles were rebuilt after it started. Stop it and let `
        + `the suite start its own: lsof -ti:5273 | xargs kill`,
    ).toBe(built);
  }
});
