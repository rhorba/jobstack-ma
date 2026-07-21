import { defineConfig } from '@playwright/test';
import { config as loadEnv } from 'dotenv';
import path from 'path';

loadEnv({ path: path.resolve(__dirname, '../.env') });

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  retries: 0,
  fullyParallel: false,
  workers: 1,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8090',
    video: 'on',
    trace: 'retain-on-failure',
  },
  outputDir: './test-results',
});
