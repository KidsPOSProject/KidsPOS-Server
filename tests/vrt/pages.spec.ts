import { test, expect } from '@playwright/test';

test.describe('Visual Regression Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Wait for the application to be ready
    await page.waitForLoadState('networkidle');
  });

  test('Homepage', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/KidsPos/);

    await expect(page).toHaveScreenshot('homepage.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('Items Management Page', async ({ page }) => {
    await page.goto('/items');
    await page.waitForSelector('h1');

    await expect(page).toHaveScreenshot('items-page.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('Stores Management Page', async ({ page }) => {
    await page.goto('/stores');
    await page.waitForSelector('h1');

    await expect(page).toHaveScreenshot('stores-page.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('Staff Management Page', async ({ page }) => {
    await page.goto('/staffs');
    await page.waitForSelector('h1');

    await expect(page).toHaveScreenshot('staff-page.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('Sales Report Page', async ({ page }) => {
    await page.goto('/reports/sales');
    await page.waitForSelector('h1');

    // Wait for store dropdown to be populated
    await page.waitForTimeout(1000);

    await expect(page).toHaveScreenshot('sales-report-page.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('Sales Page', async ({ page }) => {
    await page.goto('/sales');
    await page.waitForSelector('h1');

    await expect(page).toHaveScreenshot('sales-page.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });
});

test.describe('Mobile Visual Regression Tests', () => {
  test.use({
    viewport: { width: 375, height: 667 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1'
  });

  test('Mobile Homepage', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    await expect(page).toHaveScreenshot('mobile-homepage.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('Mobile Sales Report Page', async ({ page }) => {
    await page.goto('/reports/sales');
    await page.waitForSelector('h1');
    await page.waitForTimeout(1000);

    await expect(page).toHaveScreenshot('mobile-sales-report.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });
});

test.describe('Interactive Elements', () => {
  test('Report Form Interactions', async ({ page }) => {
    await page.goto('/reports/sales');
    await page.waitForSelector('h1');

    // Open date picker
    await page.click('#startDate');
    await page.waitForTimeout(500);

    await expect(page).toHaveScreenshot('report-date-picker.png', {
      animations: 'disabled',
    });

    // Select store dropdown
    await page.click('#storeId');
    await page.waitForTimeout(500);

    await expect(page).toHaveScreenshot('report-store-dropdown.png', {
      animations: 'disabled',
    });
  });
});