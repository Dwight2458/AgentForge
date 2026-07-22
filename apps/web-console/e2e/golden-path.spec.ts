import { expect, test } from '@playwright/test'

test('moves from project task creation through Grill to the run console', async ({ page }) => {
  await page.goto('/projects')
  await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible()

  await page.getByRole('button', { name: 'Start clarification' }).click()
  await expect(page.getByRole('heading', { name: 'Clarify the task' })).toBeVisible()

  await page.getByRole('button', { name: 'Finalize and run automatically' }).click()
  await expect(page.getByRole('heading', { name: 'Payment status filter' })).toBeVisible()
  await expect(page.getByText('Execution timeline')).toBeVisible()
  await expect(page.getByText('Subagent topology')).toBeVisible()
})
