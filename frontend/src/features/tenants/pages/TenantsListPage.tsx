import { useMemo, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import { useActivateTenant, useCreateTenant, useMigrateTenantToSilo, useOffboardTenant, useTenant, useTenants } from "../../../api/tenants";
import { ApiError } from "../../../api/http";
import { useRoles } from "../../../auth/useRoles";
import { createTenantSchema } from "../schema";
import type { CreateTenantInput } from "../schema";
import { buildTenantColumns } from "../components/columns";
import { DataTable } from "../../../components/data-table/DataTable";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../../components/ui/select";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../../../components/ui/dialog";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { Badge } from "../../../components/ui/badge";
import { EmptyState } from "../../../components/feedback/EmptyState";

export function TenantsListPage() {
  const { isPlatformAdmin, tenantId } = useRoles();
  return isPlatformAdmin ? <PlatformAdminView /> : <OwnTenantView tenantId={tenantId} />;
}

function PlatformAdminView() {
  const { data: tenants, isLoading } = useTenants(true);
  const createTenant = useCreateTenant();
  const activate = useActivateTenant();
  const offboard = useOffboardTenant();
  const migrate = useMigrateTenantToSilo();
  const [open, setOpen] = useState(false);

  const form = useForm<CreateTenantInput>({
    resolver: zodResolver(createTenantSchema),
    defaultValues: { tenantId: "", name: "", isolationTier: "POOLED", region: "id-jkt" },
  });
  const isolationTier = form.watch("isolationTier");
  const columns = useMemo(() => buildTenantColumns({ activate, offboard, migrate }), [activate, offboard, migrate]);

  function onSubmit(values: CreateTenantInput) {
    createTenant.mutate(values, { onSuccess: () => { setOpen(false); form.reset({ tenantId: "", name: "", isolationTier: "POOLED", region: "id-jkt" }); } });
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Tenants</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Platform-wide tenant lifecycle management — visible only to <code>platform-admin</code>. "Migrate to Silo" write-freezes a pooled tenant
            while its data is copied into a brand-new dedicated database.
          </p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button className="gap-1.5">
              <Plus className="size-4" />
              Create tenant
            </Button>
          </DialogTrigger>
          <DialogContent>
            <form onSubmit={form.handleSubmit(onSubmit)}>
              <DialogHeader>
                <DialogTitle>Create a tenant</DialogTitle>
              </DialogHeader>
              <div className="flex flex-col gap-4 py-4">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="tenant-id">Tenant ID</Label>
                  <Input id="tenant-id" placeholder="e.g. acme-corp" {...form.register("tenantId")} />
                  {form.formState.errors.tenantId && <p className="text-xs text-red-600">{form.formState.errors.tenantId.message}</p>}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="tenant-name">Name</Label>
                  <Input id="tenant-name" {...form.register("name")} />
                  {form.formState.errors.name && <p className="text-xs text-red-600">{form.formState.errors.name.message}</p>}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label>Isolation tier</Label>
                  <Controller
                    control={form.control}
                    name="isolationTier"
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="POOLED">Pooled (shared database)</SelectItem>
                          <SelectItem value="SILO">Silo (dedicated database, provisioned synchronously)</SelectItem>
                        </SelectContent>
                      </Select>
                    )}
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="tenant-region">Region</Label>
                  <Input id="tenant-region" {...form.register("region")} />
                  {form.formState.errors.region && <p className="text-xs text-red-600">{form.formState.errors.region.message}</p>}
                </div>
                {createTenant.isError && (
                  <p role="alert" className="text-sm text-red-600">
                    {createTenant.error instanceof ApiError ? createTenant.error.message : "Failed to create tenant"}
                  </p>
                )}
              </div>
              <DialogFooter>
                <Button type="submit" isLoading={createTenant.isPending}>
                  {createTenant.isPending ? (isolationTier === "SILO" ? "Provisioning silo…" : "Creating…") : "Create tenant"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {isLoading ? (
        <p className="text-sm text-slate-500">Loading…</p>
      ) : (
        <DataTable
          columns={columns}
          data={tenants ?? []}
          searchPlaceholder="Search tenants…"
          exportFilename="tenants"
          emptyTitle="No tenants yet"
        />
      )}
      {(activate.isError || offboard.isError || migrate.isError) && (
        <p role="alert" className="text-sm text-red-600">
          {(activate.error ?? offboard.error ?? migrate.error) instanceof ApiError
            ? ((activate.error ?? offboard.error ?? migrate.error) as ApiError).message
            : "Action failed"}
        </p>
      )}
    </div>
  );
}

function OwnTenantView({ tenantId }: { tenantId: string | undefined }) {
  const { data: tenant, isLoading, isError, error } = useTenant(tenantId);

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">My Tenant</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Only a <code>platform-admin</code> can manage tenant lifecycle — this is a read-only view of your own tenant.
        </p>
      </div>
      {isLoading ? (
        <p className="text-sm text-slate-500">Loading…</p>
      ) : isError ? (
        <p role="alert" className="text-sm text-red-600">
          {error instanceof ApiError ? error.message : "Failed to load your tenant"}
        </p>
      ) : tenant ? (
        <Card className="max-w-sm">
          <CardHeader>
            <CardTitle>{tenant.name}</CardTitle>
            <code className="text-xs text-slate-500 dark:text-slate-400">{tenant.tenantId}</code>
          </CardHeader>
          <CardContent className="flex items-center gap-2">
            <Badge variant={tenant.isolationTier === "SILO" ? "accent" : "default"}>{tenant.isolationTier}</Badge>
            <span className="text-sm text-slate-500 dark:text-slate-400">{tenant.region}</span>
            <StatusBadge status={tenant.status} />
          </CardContent>
        </Card>
      ) : (
        <EmptyState title="Tenant not found" />
      )}
    </div>
  );
}
