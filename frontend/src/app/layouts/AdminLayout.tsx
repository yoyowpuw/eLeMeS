import { AppShell } from "../../components/app-shell/AppShell";
import { RequireRole } from "../../components/guards/RequireRole";
import { ROLE } from "../../auth/roles";
import { adminNav } from "../../components/app-shell/nav-config";

export function AdminLayout() {
  return (
    <RequireRole role={[ROLE.ADMIN, ROLE.PLATFORM_ADMIN]}>
      <AppShell workspace="admin" items={adminNav} />
    </RequireRole>
  );
}
