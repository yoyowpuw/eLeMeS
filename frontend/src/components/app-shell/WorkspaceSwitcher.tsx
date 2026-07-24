import { useNavigate } from "react-router-dom";
import { Check, ChevronsUpDown } from "lucide-react";
import { useRoles } from "../../auth/useRoles";
import { Button } from "../ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import { WORKSPACE_LABEL, WORKSPACE_ROOT } from "./nav-config";
import type { Workspace } from "./nav-config";

export function WorkspaceSwitcher({ current }: { current: Workspace }) {
  const navigate = useNavigate();
  const { isManager, isAdmin, isPlatformAdmin } = useRoles();

  const available: Workspace[] = [
    "learner",
    ...(isManager ? (["manager"] as const) : []),
    ...(isAdmin || isPlatformAdmin ? (["admin"] as const) : []),
  ];

  if (available.length <= 1) {
    // Nothing to switch to — a plain learner never sees this control at all.
    return <span className="text-sm font-medium text-slate-900 dark:text-slate-100">{WORKSPACE_LABEL[current]}</span>;
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm" className="gap-2">
          {WORKSPACE_LABEL[current]}
          <ChevronsUpDown className="size-3.5 text-slate-400" aria-hidden="true" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        <DropdownMenuLabel>Switch workspace</DropdownMenuLabel>
        <DropdownMenuSeparator />
        {available.map((workspace) => (
          <DropdownMenuItem key={workspace} onClick={() => navigate(WORKSPACE_ROOT[workspace])}>
            {workspace === current && <Check className="size-4" aria-hidden="true" />}
            <span className={workspace === current ? "" : "pl-6"}>{WORKSPACE_LABEL[workspace]}</span>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
