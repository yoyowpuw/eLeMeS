import { AppShell } from "../../components/app-shell/AppShell";
import { RequireAuth } from "../../components/guards/RequireAuth";
import { learnerNav } from "../../components/app-shell/nav-config";

export function LearnerLayout() {
  return (
    <RequireAuth>
      <AppShell workspace="learner" items={learnerNav} />
    </RequireAuth>
  );
}
