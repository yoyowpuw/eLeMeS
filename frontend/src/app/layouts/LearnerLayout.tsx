import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { AppShell } from "../../components/app-shell/AppShell";
import { RequireAuth } from "../../components/guards/RequireAuth";
import { learnerNav } from "../../components/app-shell/nav-config";
import { toast } from "../../components/ui/use-toast";

/**
 * `/` is where `RequireRole` redirects a caller lacking `/manage` or
 * `/admin` access — surfaces why, then clears the nav state so it doesn't
 * re-fire on a later revisit. The `firedFor` ref guard is needed because
 * `toast()` is an impure side effect (mutates a module-level store) —
 * without it, React StrictMode's intentional dev-mode double-invocation of
 * effects fires it twice on every single denial.
 */
function useDeniedRoleToast() {
  const location = useLocation();
  const navigate = useNavigate();
  const deniedRole = (location.state as { deniedRole?: string } | null)?.deniedRole;
  const firedFor = useRef<string | null>(null);

  useEffect(() => {
    if (!deniedRole || firedFor.current === deniedRole) return;
    firedFor.current = deniedRole;
    toast({ variant: "danger", title: "Access denied", description: `You need the "${deniedRole}" role to view that workspace.` });
    navigate(location.pathname, { replace: true, state: null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deniedRole]);
}

export function LearnerLayout() {
  useDeniedRoleToast();
  return (
    <RequireAuth>
      <AppShell workspace="learner" items={learnerNav} />
    </RequireAuth>
  );
}
