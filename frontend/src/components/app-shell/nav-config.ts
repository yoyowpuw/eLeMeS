import type { LucideIcon } from "lucide-react";
import { Award, BookOpen, Building2, ClipboardList, LayoutDashboard, Network, Route } from "lucide-react";

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
}

export type Workspace = "learner" | "manager" | "admin";

export const WORKSPACE_LABEL: Record<Workspace, string> = {
  learner: "My Learning",
  manager: "Team Management",
  admin: "Administration",
};

export const WORKSPACE_ROOT: Record<Workspace, string> = {
  learner: "/",
  manager: "/manage",
  admin: "/admin",
};

/** Breadcrumb title lookup — declarative route->title map, since App.tsx uses <Routes> (not a data router), not `useMatches()`/route `handle`. */
export const ROUTE_TITLES: Record<string, string> = {
  "/": "Dashboard",
  "/courses": "Courses",
  "/paths": "Learning Paths",
  "/enrollments": "My Enrollments",
  "/certificates": "My Certificates",
  "/manage": "Dashboard",
  "/manage/org-units": "Org Hierarchy",
  "/admin": "Dashboard",
  "/admin/org-units": "Org Hierarchy",
  "/admin/tenants": "Tenants",
};

export const learnerNav: NavItem[] = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/courses", label: "Courses", icon: BookOpen },
  { to: "/paths", label: "Learning Paths", icon: Route },
  { to: "/enrollments", label: "My Enrollments", icon: ClipboardList },
  { to: "/certificates", label: "My Certificates", icon: Award },
];

export const managerNav: NavItem[] = [
  { to: "/manage", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/manage/org-units", label: "Org Hierarchy", icon: Network },
];

export const adminNav: NavItem[] = [
  { to: "/admin", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/admin/org-units", label: "Org Hierarchy", icon: Network },
  { to: "/admin/tenants", label: "Tenants", icon: Building2 },
];
