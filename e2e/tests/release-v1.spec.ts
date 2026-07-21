import { test, expect } from '@playwright/test';
import path from 'path';

const CV_FIXTURE = path.resolve(__dirname, '../fixtures/sample-cv.pdf');

test('release v1.0: employer post+pay, candidate apply, admin moderate', async ({ page }) => {
  const stamp = Date.now();
  const employerEmail = `e2e-employer-${stamp}@jobstack.ma`;
  const candidateEmail = `e2e-candidate-${stamp}@jobstack.ma`;
  const password = 'E2ePassw0rd!1';
  const jobTitle = `E2E QA Engineer ${stamp}`;
  const sector = `E2ESector${stamp}`;
  const city = `E2ECity${stamp}`;

  // --- Employer: register, register company, post a job, pay (mock CMI success) ---
  await page.goto('/register');
  await page.getByLabel('Email').fill(employerEmail);
  await page.getByLabel('Password').fill(password);
  await page.getByLabel('I am a...').click();
  await page.getByRole('option', { name: 'Employer' }).click();
  await page.getByRole('button', { name: 'Register' }).click();
  await expect(page).toHaveURL(/\/login/);

  await page.getByLabel('Email').fill(employerEmail);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page).toHaveURL(/\/employer$/);

  await page.getByLabel('Company name').fill('E2E Test Co');
  await page.getByLabel('Sector').fill(sector);
  await page.getByLabel('City').fill(city);
  await page.getByRole('button', { name: 'Save company' }).click();

  await page.getByLabel('Title').fill(jobTitle);
  await page.getByLabel('Sector').fill(sector);
  await page.getByLabel('City').fill(city);
  await page.getByLabel('Contract type').fill('CDI');
  await page.getByLabel('Description').fill('E2E release-gate test posting — safe to remove.');
  await page.getByRole('button', { name: 'Continue' }).click();

  await page.getByRole('button', { name: /Continue to Payment/ }).click();
  await page.getByRole('button', { name: 'Pay (simulate success)' }).click();
  await expect(page.getByText('Your job is live for 30 days.')).toBeVisible();

  await page.getByRole('button', { name: 'Log out' }).click();

  // --- Candidate: register, complete profile, upload CV, apply to the live posting ---
  await page.goto('/register');
  await page.getByLabel('Email').fill(candidateEmail);
  await page.getByLabel('Password').fill(password);
  await page.getByLabel('I am a...').click();
  await page.getByRole('option', { name: 'Candidate' }).click();
  await page.getByRole('button', { name: 'Register' }).click();
  await expect(page).toHaveURL(/\/login/);

  await page.getByLabel('Email').fill(candidateEmail);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page).toHaveURL(/\/candidate$/);

  await page.getByLabel('Full name').fill('E2E Candidate');
  await page.getByLabel('Phone').fill('0600000000');
  await page.getByLabel('Sector').fill(sector);
  await page.getByLabel('City').fill(city);
  await page.getByRole('button', { name: 'Save profile' }).click();
  await expect(page.getByText('Profile saved.')).toBeVisible();

  await page.setInputFiles('input[type="file"]', CV_FIXTURE);
  await expect(page.getByText('A CV is on file.')).toBeVisible();

  await page.goto('/jobs');
  await page.getByLabel('Sector').fill(sector);
  await page.getByRole('button', { name: 'Search' }).click();
  await page.getByRole('link', { name: jobTitle }).click();
  await page.getByRole('button', { name: 'Apply' }).click();
  await expect(page.getByText('Application submitted.')).toBeVisible();

  await page.getByRole('button', { name: 'Log out' }).click();

  // --- Admin: review the moderation queue, approve the posting ---
  await page.goto('/login');
  await page.getByLabel('Email').fill(process.env.ADMIN_SEED_EMAIL!);
  await page.getByLabel('Password').fill(process.env.ADMIN_SEED_PASSWORD!);
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page).toHaveURL(/\/admin$/);

  await page.goto('/admin/moderation');
  const row = page.locator('div[style*="border-bottom"]', { hasText: jobTitle });
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: 'Approve' }).click();
  await expect(row).toHaveCount(0);
});
