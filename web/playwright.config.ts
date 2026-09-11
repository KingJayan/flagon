import { defineConfig, devices } from '@playwright/test';

// building a site and starting its proxy is only worth it for the run that points at them
const chosen = process.argv.filter((arg) => arg.startsWith('--project'));
const servedRuns = chosen.length === 0 || chosen.some((arg) => arg.includes('served'));

export default defineConfig({
  testDir: './tests',
  // a program that runs 900 frames on the javascript backend takes most of a minute under a full
  // parallel run, and several specs already wait 60s for one; the default 30s killed them first
  timeout: 90_000,
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
      name: 'bundles',
      testMatch: /.*\.setup\.ts/,
    },
    {
      name: 'chromium',
      dependencies: ['bundles'],
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: { args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] },
      },
    },
    {
      name: 'firefox',
      dependencies: ['bundles'],
      // firefox ships wasm gc, so it proves both backends, not just the fallback one
      // headless firefox has neither a gpu nor sound on a ci runner, so anything that opens a
      // display stays chromium's. coverage.spec.ts checks this list against what the specs say.
      testIgnore: [
        '**/a11y.spec.ts',
        '**/arrays.spec.ts',
        '**/audio.spec.ts',
        '**/core-profile.spec.ts',
        '**/design.spec.ts',
        '**/display.spec.ts',
        '**/example.spec.ts',
        '**/fixedfunction.spec.ts',
        '**/framebuffer.spec.ts',
        '**/glcore.spec.ts',
        '**/glfw.spec.ts',
        '**/halfshader.spec.ts',
        '**/input.spec.ts',
        '**/play.spec.ts',
        '**/render.spec.ts',
        '**/shaders.spec.ts',
        '**/shell.spec.ts',
        '**/texture.spec.ts',
        '**/threading.spec.ts',
        '**/thirdparty.spec.ts',
      ],
      use: { ...devices['Desktop Firefox'] },
    },
    {
      // the sockets again, against the server that ships rather than against vite
      name: 'served',
      dependencies: ['bundles'],
      testMatch: /net\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: 'http://localhost:4273' },
    },
  ],
  webServer: [
    {
      command: process.env.CI ? 'bun run preview -- --port 5273' : 'bun run dev',
      url: 'http://localhost:5273',
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
    ...(servedRuns
      ? [
          {
            command: 'bun run build && bun run proxy -- --root dist --port 4273 --probes',
            url: 'http://localhost:4273',
            reuseExistingServer: !process.env.CI,
            timeout: 180_000,
          },
        ]
      : []),
  ],
});
