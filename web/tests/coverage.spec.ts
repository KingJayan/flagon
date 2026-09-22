import { test, expect } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));

/**
 * headless firefox on a runner has no gpu and no sound. a spec needing either is chromium's.
 * a spec says '// needs a display' on its first line, and the firefox project lists the rest.
 */

test.describe('which engine runs which spec', () => {
  test('firefox runs exactly the specs that need no display', async () => {
    const config = await fs.readFile(path.join(here, '../playwright.config.ts'), 'utf8');
    const listed = /testMatch: \[(.*?)\]/s.exec(config);
    const matched = new Set(
      [...(listed?.[1] ?? '').matchAll(/\*\*\/([\w.-]+\.spec\.ts)/g)].map((found) => found[1]),
    );

    // readdir gives no order, so sort once and everything below stays reportable
    // eslint-disable-next-line unicorn/no-array-sort
    const names = (await fs.readdir(here)).filter((name) => name.endsWith('.spec.ts')).sort();
    const declared = new Set<string>();
    for (const name of names) {
      const source = await fs.readFile(path.join(here, name), 'utf8');
      if (source.startsWith('// needs a display')) {
        declared.add(name);
      }
    }

    const headless = names.filter((name) => !declared.has(name));
    const missing = headless.filter((name) => !matched.has(name));
    const wrong = [...matched].filter((name) => declared.has(name));

    expect(missing, 'these need no display; add them to the firefox testMatch').toEqual([]);
    expect(wrong, 'these open a display; drop them from the firefox testMatch').toEqual([]);
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
