import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: 'http://localhost:5273',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // a page that has not been clicked gets a suspended AudioContext, and the audio demo
        // runs from the url rather than from a button
        launchOptions: { args: ['--autoplay-policy=no-user-gesture-required', '--mute-audio'] },
      },
    },
  ],
  webServer: {
    command: 'bun run dev',
    url: 'http://localhost:5273',
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
});
