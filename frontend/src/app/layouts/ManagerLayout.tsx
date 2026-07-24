import { AppShell } from "../../components/app-shell/AppShell";
import { RequireRole } from "../../components/guards/RequireRole";
import { ROLE } from "../../auth/roles";
import { managerNav } from "../../components/app-shell/nav-config";

export function ManagerLayout() {
  return (
    <RequireRole role={ROLE.MANAGER}>
      <AppShell workspace="manager" items={managerNav} />
    </RequireRole>
  );
}
