import { defineConfig, devices } from '@playwright/test';

const chosen = process.argv.filter((arg) => arg.startsWith('--project'));
const servedRuns = chosen.length === 0 || chosen.some((arg) => arg.includes('served'));

export default defineConfig({
  testDir: './tests',
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
        '**/reflection.spec.ts',
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
      name: 'served',
      dependencies: ['bundles'],
      testMatch: /(net|policy)\.spec\.ts/,
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
            command: process.env.CI
              ? 'bun run proxy -- --root dist --port 4273 --probes'
              : 'bun run build && bun run proxy -- --root dist --port 4273 --probes',
            url: 'http://localhost:4273',
            reuseExistingServer: !process.env.CI,
            timeout: 180_000,
          },
        ]
      : []),
  ],
});
