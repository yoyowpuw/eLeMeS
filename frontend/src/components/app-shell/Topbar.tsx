import { Link, useLocation } from "react-router-dom";
import { Menu, Search } from "lucide-react";
import { Breadcrumbs } from "./Breadcrumbs";
import { WorkspaceSwitcher } from "./WorkspaceSwitcher";
import { UserMenu } from "./UserMenu";
import { WORKSPACE_ROOT } from "./nav-config";
import type { Workspace } from "./nav-config";

export function Topbar({
  workspace,
  onMenuClick,
  onCommandPaletteClick,
}: {
  workspace: Workspace;
  onMenuClick: () => void;
  onCommandPaletteClick: () => void;
}) {
  const { pathname } = useLocation();
  const atWorkspaceRoot = pathname === WORKSPACE_ROOT[workspace];
  const isMac = typeof navigator !== "undefined" && navigator.platform.toUpperCase().includes("MAC");

  return (
    <header className="flex h-14 shrink-0 items-center gap-4 border-b border-slate-200 px-4 dark:border-slate-800">
      <button onClick={onMenuClick} className="rounded-md p-1.5 text-slate-500 hover:bg-slate-100 lg:hidden dark:text-slate-400 dark:hover:bg-slate-900" aria-label="Open navigation menu">
        <Menu className="size-5" aria-hidden="true" />
      </button>
      <Link to="/" className="text-base font-bold text-blue-600">
        eLeMeS
      </Link>
      <div className="hidden h-5 w-px bg-slate-200 lg:block dark:bg-slate-800" aria-hidden="true" />
      <WorkspaceSwitcher current={workspace} />
      {/* Redundant with the switcher's own label when already at the workspace root — the breadcrumb trail only earns its place once you're somewhere beneath it. */}
      {!atWorkspaceRoot && (
        <div className="hidden flex-1 md:block">
          <Breadcrumbs workspace={workspace} />
        </div>
      )}
      <div className="ml-auto flex items-center gap-3">
        <button
          onClick={onCommandPaletteClick}
          className="hidden items-center gap-2 rounded-md border border-slate-200 px-2.5 py-1.5 text-xs text-slate-500 hover:bg-slate-50 sm:flex dark:border-slate-800 dark:text-slate-400 dark:hover:bg-slate-900"
        >
          <Search className="size-3.5" aria-hidden="true" />
          Jump to…
          <kbd className="rounded border border-slate-200 px-1 font-sans dark:border-slate-700">{isMac ? "⌘K" : "Ctrl K"}</kbd>
        </button>
        <UserMenu />
      </div>
    </header>
  );
}
