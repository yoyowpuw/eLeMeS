import { Fragment } from "react";
import { Link, useLocation } from "react-router-dom";
import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from "../ui/breadcrumb";
import { ROUTE_TITLES, WORKSPACE_LABEL, WORKSPACE_ROOT } from "./nav-config";
import type { Workspace } from "./nav-config";

/**
 * Built from the manually-maintained `ROUTE_TITLES` map in nav-config.ts,
 * not `useMatches()`/route `handle` — App.tsx uses declarative `<Routes>`
 * (not a data router), so the manual map is the lower-risk default. Handles
 * one level of dynamic segment (e.g. `/enrollments/:id`) by walking up to
 * the nearest known static ancestor; deeper dynamic paths fall back to a
 * generic trailing "Details" crumb rather than a wrong label.
 */
export function Breadcrumbs({ workspace }: { workspace: Workspace }) {
  const { pathname } = useLocation();
  const root = WORKSPACE_ROOT[workspace];
  const rootLabel = WORKSPACE_LABEL[workspace];

  const crumbs: { label: string; to?: string }[] = [{ label: rootLabel, to: pathname === root ? undefined : root }];

  if (pathname !== root) {
    const knownTitle = ROUTE_TITLES[pathname];
    if (knownTitle) {
      crumbs.push({ label: knownTitle });
    } else {
      const parent = pathname.slice(0, pathname.lastIndexOf("/"));
      const parentTitle = ROUTE_TITLES[parent];
      if (parentTitle) crumbs.push({ label: parentTitle, to: parent });
      crumbs.push({ label: "Details" });
    }
  }

  return (
    <Breadcrumb>
      <BreadcrumbList>
        {crumbs.map((crumb, index) => (
          <Fragment key={`${crumb.label}-${index}`}>
            {index > 0 && <BreadcrumbSeparator />}
            <BreadcrumbItem>
              {crumb.to ? (
                <BreadcrumbLink asChild>
                  <Link to={crumb.to}>{crumb.label}</Link>
                </BreadcrumbLink>
              ) : (
                <BreadcrumbPage>{crumb.label}</BreadcrumbPage>
              )}
            </BreadcrumbItem>
          </Fragment>
        ))}
      </BreadcrumbList>
    </Breadcrumb>
  );
}
