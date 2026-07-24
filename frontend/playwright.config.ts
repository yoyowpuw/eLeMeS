import { defineConfig, devices } from "@playwright/test";

/**
 * Ch.14 §4 / NFR-032: axe-core CI gating, previously unimplemented (listed
 * as a real deferred-scope gap in project memory before this redesign).
 * Runs against the already-running dev server + backend stack (this
 * project's established practice is to verify against real running
 * services, never mocks — see feedback-verify-with-real-execution) — start
 * `npm run dev` and the 6 backend services + Keycloak yourself before
 * running `npx playwright test`, same as any manual verification pass.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false, // shared Keycloak/OPA state across tests — run serially to avoid cross-test interference
  retries: 0,
  reporter: "list",
  use: {
    baseURL: "http://localhost:5173",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
