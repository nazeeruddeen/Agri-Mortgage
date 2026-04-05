import { expect, Page, test } from '@playwright/test';

const frontendBaseUrl = process.env.AGRI_E2E_BASE_URL ?? 'http://127.0.0.1:4400';
const apiBaseUrl = process.env.AGRI_E2E_API_BASE_URL ?? 'http://127.0.0.1:8011';
const username = process.env.AGRI_E2E_USERNAME ?? 'admin';
const password = requiredEnv('AGRI_E2E_PASSWORD');

test.describe('Agri mortgage golden path', () => {
  test('operator can register land-backed intake and run encumbrance verification', async ({ page }) => {
    const suffix = Date.now().toString();
    const applicantName = `Farmer ${suffix}`;
    const district = `District-${suffix}`;
    const surveyNumber = `SVY-${suffix.slice(-6)}`;

    await login(page);

    await page.getByTestId('agri-tab-intake').click();
    await page.getByTestId('agri-primary-applicant-name').fill(applicantName);
    await page.getByTestId('agri-primary-applicant-aadhaar').fill(`12341234${suffix.slice(-4)}`);
    await page.getByTestId('agri-primary-applicant-pan').fill(`AGRIP${suffix.slice(-4)}A`);
    await page.getByTestId('agri-primary-income').fill('180000');
    await page.getByTestId('agri-district').first().fill(district);
    await page.getByTestId('agri-taluka').first().fill(`Taluka-${suffix}`);
    await page.getByTestId('agri-village').first().fill(`Village-${suffix}`);
    await page.getByTestId('agri-requested-amount').fill('750000');
    await page.getByTestId('agri-requested-tenure').fill('24');
    await page.getByTestId('agri-purpose').fill('Irrigation and equipment modernization');
    await page.getByTestId('agri-coborrower-name').fill(`CoBorrower ${suffix}`);
    await page.getByLabel('Aadhaar').nth(1).fill(`22341234${suffix.slice(-4)}`);
    await page.getByLabel('PAN').nth(1).fill(`COPAN${suffix.slice(-4)}A`);
    await page.getByLabel('Monthly income').nth(1).fill('85000');
    await page.getByTestId('agri-land-survey-number').fill(surveyNumber);
    await page.getByTestId('agri-land-area').fill('4.5');
    await page.getByTestId('agri-district').nth(1).fill(district);
    await page.getByTestId('agri-taluka').nth(1).fill(`Taluka-${suffix}`);
    await page.getByTestId('agri-village').nth(1).fill(`Village-${suffix}`);
    await page.getByTestId('agri-land-type').selectOption('IRRIGATED');
    await page.getByTestId('agri-land-market-value').fill('2200000');
    await page.getByTestId('agri-land-circle-rate').fill('2000000');
    await page.getByTestId('agri-land-ownership-status').selectOption('SOLE');
    await page.getByTestId('agri-land-encumbrance-status').selectOption('CLEAR');
    await page.getByTestId('agri-create-application').click();

    const createdApplication = await apiJson<{ content: Array<{ id: number; primaryApplicantName: string }> }>(
      page,
      `/api/v1/agri-mortgage-applications?district=${encodeURIComponent(district)}&page=0&size=20`
    );
    const applicationId = createdApplication.content.find((item) => item.primaryApplicantName === applicantName)?.id;
    expect(applicationId, 'expected created agri application id').toBeTruthy();

    await page.goto(`${frontendBaseUrl}/operations?selectedApplicationId=${applicationId}`);
    await expect(page.getByText(applicantName)).toBeVisible();
    await expect(page.getByTestId('agri-selected-status')).toHaveText('DRAFT');

    await page.getByTestId('agri-evaluate-application').click();
    await expect(page.getByText(/Eligible|Not eligible/)).toBeVisible();

    await page.getByTestId('agri-run-encumbrance').click();
    await expect(page.getByTestId('agri-encumbrance-status')).toContainText('Encumbrance clear');
    await expect(page.getByTestId('agri-encumbrance-summary')).toContainText('clear');
  });
});

async function login(page: Page): Promise<void> {
  await page.goto(`${frontendBaseUrl}/overview`);
  await page.getByTestId('agri-login-username').fill(username);
  await page.getByTestId('agri-login-password').fill(password);
  await page.getByTestId('agri-login-submit').click();
  await expect(page.getByTestId('agri-tab-dashboard')).toBeVisible();
  await expect(page.getByTestId('agri-notice')).toContainText(/Loading|refreshed/i);
}

async function apiJson<T>(page: Page, path: string): Promise<T> {
  const response = await page.request.get(`${apiBaseUrl}${path}`);
  expect(response.ok(), `GET ${path} should succeed`).toBeTruthy();
  return (await response.json()) as T;
}

function requiredEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable ${name}`);
  }
  return value;
}
