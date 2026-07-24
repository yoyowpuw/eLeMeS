import { Link } from "react-router-dom";
import { Building2, Network } from "lucide-react";
import { useTenant, useTenants } from "../../../api/tenants";
import { useRoles } from "../../../auth/useRoles";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Button } from "../../../components/ui/button";
import { Badge } from "../../../components/ui/badge";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { Skeleton } from "../../../components/ui/skeleton";

export function AdminDashboard() {
  const { isPlatformAdmin, tenantId } = useRoles();
  const { data: tenant, isLoading } = useTenant(tenantId);
  const { data: allTenants } = useTenants(isPlatformAdmin);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Administration</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">Tenant configuration and platform oversight.</p>
      </div>

      <Card className="max-w-sm">
        <CardHeader>
          <CardTitle>Your tenant</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-6 w-32" />
          ) : tenant ? (
            <div className="flex items-center gap-2 text-sm">
              <span className="font-medium text-slate-900 dark:text-slate-100">{tenant.name}</span>
              <Badge variant={tenant.isolationTier === "SILO" ? "accent" : "default"}>{tenant.isolationTier}</Badge>
              <StatusBadge status={tenant.status} />
            </div>
          ) : (
            <p className="text-sm text-slate-500 dark:text-slate-400">No tenant record found.</p>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Button variant="outline" className="justify-start gap-2" asChild>
          <Link to="/admin/org-units">
            <Network className="size-4" /> Org hierarchy
          </Link>
        </Button>
        <Button variant="outline" className="justify-start gap-2" asChild>
          <Link to="/courses">Course &amp; path catalog</Link>
        </Button>
      </div>

      {isPlatformAdmin && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Building2 className="size-4" />
              Platform administration
            </CardTitle>
            <p className="text-sm text-slate-500 dark:text-slate-400">{allTenants?.length ?? 0} tenants registered across the platform.</p>
          </CardHeader>
          <CardContent>
            <Button asChild>
              <Link to="/admin/tenants">Manage tenants</Link>
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
