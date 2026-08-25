import { test, expect, Page } from '@playwright/test';

const SALES_SUMMARY_FIXTURE = {
  startDate: '2025-10-01T00:00:00.000+00:00',
  endDate: '2025-10-03T00:00:00.000+00:00',
  totalSales: 25,
  totalItemCount: 48,
  totalAmount: 12500,
  averageAmount: 500,
  stores: [
    { storeId: 1, storeName: 'サンプル店舗', salesCount: 25, itemCount: 48, amount: 12500 },
  ],
  items: [
    { itemId: 1, itemName: 'サンプル商品A', unitPrice: 100, quantity: 30, amount: 3000 },
    { itemId: 2, itemName: 'サンプル商品B', unitPrice: 500, quantity: 18, amount: 9000 },
  ],
  daily: [
    { date: '2025-10-01', salesCount: 8, itemCount: 15, amount: 4000 },
    { date: '2025-10-02', salesCount: 9, itemCount: 18, amount: 4500 },
    { date: '2025-10-03', salesCount: 8, itemCount: 15, amount: 4000 },
  ],
};

// 売上レポート画面は開いた時点の日付で集計 API を呼ぶため、固定しないと日が変わるたびに
// スクリーンショットが変わる。集計結果を固定値に差し替えたうえで期間を入れ直し、
// 描画が終わるまで待ってからスクリーンショットを撮る。
async function openSalesReport(page: Page) {
  await page.route('**/api/reports/sales/summary*', (route) =>
    route.fulfill({ json: SALES_SUMMARY_FIXTURE }),
  );

  await page.goto('/reports/sales');
  await page.waitForSelector('h1');
  await page.waitForSelector('#storeId option', { state: 'attached' });

  await page.evaluate(() => {
    const startDate = document.getElementById('startDate') as HTMLInputElement;
    const endDate = document.getElementById('endDate') as HTMLInputElement;
    startDate.value = '2025-10-01';
    endDate.value = '2025-10-03';
    const year = document.getElementById('year') as HTMLSelectElement;
    year.innerHTML = '<option value="2025" selected>2025年</option>';
    (document.getElementById('month') as HTMLSelectElement).value = '10';
    endDate.dispatchEvent(new Event('change'));
  });

  await expect(page.locator('#summaryBody')).toBeVisible();
  await expect(page.locator('#summaryPeriod')).toHaveText('2025-10-01 ～ 2025-10-03');
  await expect(page.locator('#statTotalAmount')).toHaveText('¥12,500');
}

test.describe('Visual Regression Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Wait for the application to be ready
    await page.waitForLoadState('networkidle');
  });

  test('Homepage', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/KidsPOS/);

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


  test('Sales Report Page', async ({ page }) => {
    await openSalesReport(page);

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
    await openSalesReport(page);

    await expect(page).toHaveScreenshot('mobile-sales-report.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });
});

test.describe('Interactive Elements', () => {
  test.skip('Report Form Interactions', async ({ page }) => {
    await page.goto('/reports/sales');
    await page.waitForSelector('h1');

    // Open date picker
    await page.click('#startDate');
    await page.waitForSelector('.datepicker-calendar, .react-datepicker, .MuiPickersPopper-root', { state: 'visible' });

    await expect(page).toHaveScreenshot('report-date-picker.png', {
      animations: 'disabled',
    });

    // Select store dropdown
    await page.click('#storeId');
    await page.waitForSelector('.ant-select-dropdown'); // Wait for dropdown options to appear

    await expect(page).toHaveScreenshot('report-store-dropdown.png', {
      animations: 'disabled',
    });
  });
});
