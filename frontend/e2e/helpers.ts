import type { Page } from "@playwright/test";

/** Real OIDC Authorization Code + PKCE login against the actual Keycloak dev realm — no mocking, matching this project's established verification practice. */
export async function login(page: Page, username: string, password: string) {
  await page.goto("/");
  await page.getByRole("button", { name: "Sign in" }).first().click();
  await page.waitForURL(/localhost:8080\/realms\/elemes/, { timeout: 15000 });
  await page.fill("#username", username);
  await page.fill("#password", password);
  await page.click("#kc-login");
  await page.waitForURL(/localhost:5173/, { timeout: 15000 });
  await page.waitForTimeout(500);
}
