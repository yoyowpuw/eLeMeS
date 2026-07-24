import { useAuth } from "react-oidc-context";
import { LogOut } from "lucide-react";
import { useRoles } from "../../auth/useRoles";
import { Avatar, AvatarFallback } from "../ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import { RoleBadge } from "../feedback/RoleBadge";

export function UserMenu() {
  const auth = useAuth();
  const { roles, tenantId, username } = useRoles();
  const initials = (username ?? "?").slice(0, 2).toUpperCase();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2">
          <Avatar>
            <AvatarFallback>{initials}</AvatarFallback>
          </Avatar>
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel className="flex flex-col gap-0.5">
          <span className="text-sm font-medium text-slate-900 dark:text-slate-100">{username}</span>
          <span className="text-xs font-normal text-slate-500 dark:text-slate-400">{tenantId}</span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <div className="flex flex-wrap gap-1 px-2 py-1.5">
          {roles.map((role) => (
            <RoleBadge key={role} role={role} />
          ))}
        </div>
        <DropdownMenuSeparator />
        {/*
          `removeUser()` only clears this app's own local session — it never
          touches Keycloak's SSO session cookie, so the next sign-in would
          silently succeed with no login prompt at all. `signoutRedirect()`
          does real RP-Initiated Logout.
        */}
        <DropdownMenuItem onClick={() => auth.signoutRedirect()}>
          <LogOut className="size-4" aria-hidden="true" />
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
