import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? '50%' : undefined,
  reporter: process.env.CI ? [['github'], ['blob']] : 'list',
  use: {
    baseURL: 'http://localhost:5273',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: { args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] },
      },
    },
    {
      name: 'firefox',
      // the fallback backend is why a second engine is here; wasm gc is chromium's to prove
      grepInvert: /on wasm/,
      // headless firefox has neither a gpu nor sound on a ci runner, so the demos that need
      // one stay chromium's. what firefox is here to prove is the engine under them.
      testIgnore: [
        '**/arrays.spec.ts',
        '**/audio.spec.ts',
        '**/display.spec.ts',
        '**/example.spec.ts',
        '**/input.spec.ts',
        '**/render.spec.ts',
        '**/shaders.spec.ts',
        '**/texture.spec.ts',
      ],
      use: { ...devices['Desktop Firefox'] },
    },
  ],
  webServer: {
    command: 'bun run dev',
    url: 'http://localhost:5273',
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
});
