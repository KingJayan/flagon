import { test, expect } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));

/**
 * headless firefox on a runner has no gpu and no sound, so a spec that opens a display is
 * chromium's. that rule lived in a comment and was forgotten twice, so it is checked here:
 * a spec says '// needs a display' on its first line, and the firefox project ignores exactly
 * those. a spec that passes locally, where there is a gpu, is not evidence either way.
 */
test.describe('which engine runs which spec', () => {
  test('firefox ignores exactly the specs that say they need a display', async () => {
    const config = await fs.readFile(path.join(here, '../playwright.config.ts'), 'utf8');
    const listed = /testIgnore: \[(.*?)\]/s.exec(config);
    const ignored = new Set(
      [...(listed?.[1] ?? '').matchAll(/\*\*\/([\w.-]+\.spec\.ts)/g)].map((found) => found[1]),
    );

    const names = (await fs.readdir(here)).filter((name) => name.endsWith('.spec.ts'));
    const declared = new Set<string>();
    for (const name of names) {
      const source = await fs.readFile(path.join(here, name), 'utf8');
      if (source.startsWith('// needs a display')) {
        declared.add(name);
      }
    }

    const unlisted = [...declared].filter((name) => !ignored.has(name)).sort();
    const unnecessary = [...ignored].filter((name) => !declared.has(name)).sort();

    expect(unlisted, 'these open a display; add them to the firefox testIgnore').toEqual([]);
    expect(unnecessary, 'these no longer say they need a display; drop them').toEqual([]);
    expect(declared.size).toBeGreaterThan(0);
  });

  test('every spec file is one or the other, and none was left unnamed', async () => {
    const names = (await fs.readdir(here)).filter((name) => name.endsWith('.spec.ts'));

    expect(names.length).toBeGreaterThan(20);
    for (const name of names) {
      const source = await fs.readFile(path.join(here, name), 'utf8');
      expect(source, `${name} is empty`).not.toBe('');
    }
  });
});
