import { useAuth } from "react-oidc-context";
import { AppShell } from "../components/app-shell/AppShell";
import { learnerNav } from "../components/app-shell/nav-config";
import { LandingPage } from "../features/landing/pages/LandingPage";
import { LearnerDashboard } from "../features/dashboard/pages/LearnerDashboard";
import { Button } from "../components/ui/button";
import { useDeniedRoleToast } from "./useDeniedRoleToast";

/**
 * `/` itself — deliberately NOT inside `LearnerLayout`'s `RequireAuth` gate,
 * unlike every other learner route. A signed-out visitor gets a real public
 * landing page here (see LandingPage's own doc comment for why that used to
 * be a bare "Sign in required" box); once authenticated, this renders
 * exactly what the nested `/courses` etc. routes get — the same AppShell,
 * just with its content passed directly instead of via `<Outlet/>`, since
 * this route sits outside that nested tree.
 */
export function RootRoute() {
  const auth = useAuth();
  useDeniedRoleToast();

  if (auth.isLoading) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <Button variant="ghost" disabled isLoading className="pointer-events-none">
          Loading…
        </Button>
      </div>
    );
  }

  if (auth.isAuthenticated) {
    return (
      <AppShell workspace="learner" items={learnerNav}>
        <LearnerDashboard />
      </AppShell>
    );
  }

  return <LandingPage />;
}
