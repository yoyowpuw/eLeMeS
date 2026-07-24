import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { login } from "./helpers";

/**
 * Ch.14 §4 / NFR-032. Scans the surfaces named in the redesign plan's Phase
 * 6 scope: the 3 dashboards, a data table, a Dialog form in its open state,
 * and the public (signed-out) Verify page. `wcag2a`/`wcag2aa` tags match
 * NFR-028's WCAG 2.1 AA target.
 */
async function expectNoViolations(page: import("@playwright/test").Page) {
  const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();
  expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
}

test.describe("Accessibility (axe-core, WCAG 2.1 AA)", () => {
  test("Learner dashboard", async ({ page }) => {
    await login(page, "learner1", "learner1");
    await page.waitForSelector("text=Welcome back");
    await expectNoViolations(page);
  });

  test("Manager dashboard", async ({ page }) => {
    await login(page, "maya", "maya");
    await page.getByRole("button", { name: "My Learning" }).click();
    await page.getByRole("menuitem", { name: "Team Management" }).click();
    await page.waitForSelector("text=Team Management");
    await expectNoViolations(page);
  });

  test("Admin dashboard (platform-admin)", async ({ page }) => {
    await login(page, "platform-ops", "platform-ops");
    await page.goto("/admin");
    await page.waitForSelector("text=Administration");
    await expectNoViolations(page);
  });

  test("Courses data table", async ({ page }) => {
    await login(page, "admin1", "admin1");
    await page.getByRole("link", { name: "Courses", exact: true }).click();
    await page.waitForSelector("text=Courses created this session");
    await expectNoViolations(page);
  });

  test("Create-course dialog (open state)", async ({ page }) => {
    await login(page, "admin1", "admin1");
    await page.getByRole("link", { name: "Courses", exact: true }).click();
    await page.getByRole("button", { name: "Create course" }).click();
    await page.waitForSelector("text=Create a course");
    await expectNoViolations(page);
  });

  test("Tenants data table (platform-admin)", async ({ page }) => {
    await login(page, "platform-ops", "platform-ops");
    await page.getByRole("button", { name: "My Learning" }).click();
    await page.getByRole("menuitem", { name: "Administration" }).click();
    await page.getByRole("link", { name: "Tenants", exact: true }).click();
    await page.waitForSelector("text=Platform-wide tenant lifecycle");
    await expectNoViolations(page);
  });

  test("Verify page (public, signed out)", async ({ page }) => {
    await page.goto("/verify");
    await page.waitForSelector("text=Verify a Certificate");
    await expectNoViolations(page);
  });
});
