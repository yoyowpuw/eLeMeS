import { useState } from "react";
import { Outlet } from "react-router-dom";
import { NavLink } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import { Sheet, SheetContent, SheetTitle } from "../ui/sheet";
import { cn } from "../../lib/utils";
import { WORKSPACE_LABEL } from "./nav-config";
import type { NavItem, Workspace } from "./nav-config";

export function AppShell({ workspace, items }: { workspace: Workspace; items: NavItem[] }) {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="flex h-svh flex-col">
      <Topbar workspace={workspace} onMenuClick={() => setMobileOpen(true)} />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar items={items} workspaceLabel={WORKSPACE_LABEL[workspace]} />
        <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
          <SheetContent side="left" className="w-64 max-w-[80vw] p-0 lg:hidden">
            <SheetTitle className="sr-only">{WORKSPACE_LABEL[workspace]} navigation</SheetTitle>
            <nav className="flex flex-col gap-0.5 p-2" aria-label={`${WORKSPACE_LABEL[workspace]} navigation`}>
              {items.map(({ to, label, icon: Icon, end }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={end}
                  onClick={() => setMobileOpen(false)}
                  className={({ isActive }) =>
                    cn(
                      "flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-900",
                      isActive && "bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300",
                    )
                  }
                >
                  <Icon className="size-4 shrink-0" aria-hidden="true" />
                  <span>{label}</span>
                </NavLink>
              ))}
            </nav>
          </SheetContent>
        </Sheet>
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
