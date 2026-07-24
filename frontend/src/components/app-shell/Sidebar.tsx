import { useState } from "react";
import { NavLink } from "react-router-dom";
import { ChevronsLeft, ChevronsRight } from "lucide-react";
import { cn } from "../../lib/utils";
import type { NavItem } from "./nav-config";

const COLLAPSE_KEY = "elemes.sidebar.collapsed";

export function Sidebar({ items, workspaceLabel }: { items: NavItem[]; workspaceLabel: string }) {
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem(COLLAPSE_KEY) === "1");

  function toggle() {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem(COLLAPSE_KEY, next ? "1" : "0");
      return next;
    });
  }

  return (
    <aside
      className={cn(
        "hidden shrink-0 flex-col border-r border-slate-200 bg-white transition-[width] duration-150 lg:flex dark:border-slate-800 dark:bg-slate-950",
        collapsed ? "w-14" : "w-56",
      )}
    >
      <nav className="flex flex-1 flex-col gap-0.5 p-2" aria-label={`${workspaceLabel} navigation`}>
        {items.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            title={collapsed ? label : undefined}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-900",
                isActive && "bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300",
              )
            }
          >
            <Icon className="size-4 shrink-0" aria-hidden="true" />
            {!collapsed && <span className="truncate">{label}</span>}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-slate-200 p-2 dark:border-slate-800">
        <button
          onClick={toggle}
          className="flex w-full items-center gap-3 rounded-md px-2.5 py-2 text-sm text-slate-500 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-900"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? <ChevronsRight className="size-4" aria-hidden="true" /> : <ChevronsLeft className="size-4" aria-hidden="true" />}
          {!collapsed && <span>Collapse</span>}
        </button>
      </div>
    </aside>
  );
}
