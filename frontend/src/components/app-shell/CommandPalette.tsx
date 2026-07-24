import { useNavigate } from "react-router-dom";
import { BookOpen, LayoutDashboard, Network, Route as RouteIcon, ShieldCheck } from "lucide-react";
import { useRoles } from "../../auth/useRoles";
import { CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "../ui/command";
import { WORKSPACE_LABEL, WORKSPACE_ROOT } from "./nav-config";
import type { Workspace } from "./nav-config";

/**
 * Pure keyboard nav-jump tool, deliberately not a content search — there is
 * nothing real to search over (no list/search endpoint exists for courses,
 * enrollments, paths, or org-units; see the redesign plan). Scoped to
 * whichever workspaces this user's JWT roles actually unlock, same rule
 * `WorkspaceSwitcher` uses. `open` is controlled by `AppShell` (which also
 * owns the Cmd+K keydown listener) so the Topbar's visible trigger button
 * and the keyboard shortcut share the same state.
 */
export function CommandPalette({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const navigate = useNavigate();
  const { isManager, isAdmin, isPlatformAdmin } = useRoles();

  function go(to: string) {
    onOpenChange(false);
    navigate(to);
  }

  const workspaces: Workspace[] = ["learner", ...(isManager ? (["manager"] as const) : []), ...(isAdmin || isPlatformAdmin ? (["admin"] as const) : [])];

  return (
    <CommandDialog open={open} onOpenChange={onOpenChange}>
      <CommandInput placeholder="Jump to…" />
      <CommandList>
        <CommandEmpty>No matches.</CommandEmpty>
        <CommandGroup heading="Workspaces">
          {workspaces.map((workspace) => (
            <CommandItem key={workspace} onSelect={() => go(WORKSPACE_ROOT[workspace])}>
              <LayoutDashboard />
              {WORKSPACE_LABEL[workspace]}
            </CommandItem>
          ))}
        </CommandGroup>
        <CommandGroup heading="Quick actions">
          <CommandItem onSelect={() => go("/courses")}>
            <BookOpen />
            Browse courses
          </CommandItem>
          <CommandItem onSelect={() => go("/paths")}>
            <RouteIcon />
            Browse learning paths
          </CommandItem>
          {(isManager || isAdmin) && (
            <CommandItem onSelect={() => go(isAdmin ? "/admin/org-units" : "/manage/org-units")}>
              <Network />
              Org hierarchy
            </CommandItem>
          )}
          <CommandItem onSelect={() => go("/verify")}>
            <ShieldCheck />
            Verify a certificate
          </CommandItem>
        </CommandGroup>
      </CommandList>
    </CommandDialog>
  );
}
