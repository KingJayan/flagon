import { defineConfig, devices } from '@playwright/test';

const chosen = process.argv.filter((arg) => arg.startsWith('--project'));
const servedRuns = chosen.length === 0 || chosen.some((arg) => arg.includes('served'));

export default defineConfig({
  testDir: './tests',
  timeout: 90_000,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  // CI shards each project across runners
  workers: process.env.CI ? '100%' : undefined,
  reporter: process.env.CI ? [['github'], ['blob']] : 'list',
  use: {
    baseURL: 'http://127.0.0.1:5273',
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
      // firefox runs the specs named here
      testMatch: [
        '**/boot.spec.ts',
        '**/classloader.spec.ts',
        '**/concurrency.spec.ts',
        '**/coverage.spec.ts',
        '**/crypto.spec.ts',
        '**/docs.spec.ts',
        '**/files.spec.ts',
        '**/image.spec.ts',
        '**/jdkgaps.spec.ts',
        '**/liveness.spec.ts',
        '**/lwjgl3.spec.ts',
        '**/net.spec.ts',
        '**/pausing.spec.ts',
        '**/playground.spec.ts',
        '**/policy.spec.ts',
        '**/proxy.spec.ts',
        '**/resources.spec.ts',
        '**/serial.spec.ts',
        '**/shapes.spec.ts',
        '**/shutdownhook.spec.ts',
        '**/tls.spec.ts',
        '**/tunnel.spec.ts',
      ],
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'served',
      dependencies: ['bundles'],
      testMatch: /(net|policy)\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: 'http://127.0.0.1:4273' },
    },
  ],
  webServer: [
    {
      command: process.env.CI ? 'bun run preview -- --port 5273' : 'bun run dev',
      url: 'http://127.0.0.1:5273',
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
    ...(servedRuns
      ? [
          {
            // ci builds dist in an earlier job
            command: (process.env.CI ? '' : 'bun run build && ')
              + 'bun run proxy -- --root dist --host 127.0.0.1 --port 4273 --probes',
            url: 'http://127.0.0.1:4273',
            reuseExistingServer: !process.env.CI,
            timeout: 180_000,
          },
        ]
      : []),
  ],
});
