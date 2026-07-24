import type { ColumnDef } from "@tanstack/react-table";
import type { UseMutationResult } from "@tanstack/react-query";
import type { Tenant } from "../../../api/types";
import { DataTableColumnHeader } from "../../../components/data-table/DataTableColumnHeader";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { Badge } from "../../../components/ui/badge";
import { Button } from "../../../components/ui/button";

interface TenantMutations {
  activate: UseMutationResult<Tenant, unknown, string>;
  offboard: UseMutationResult<Tenant, unknown, string>;
  migrate: UseMutationResult<Tenant, unknown, string>;
}

export function buildTenantColumns({ activate, offboard, migrate }: TenantMutations): ColumnDef<Tenant>[] {
  return [
    {
      accessorKey: "name",
      header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
      cell: ({ row }) => (
        <div>
          <div className="font-medium text-slate-900 dark:text-slate-100">{row.original.name}</div>
          <code className="text-xs text-slate-500 dark:text-slate-400">{row.original.tenantId}</code>
        </div>
      ),
    },
    {
      accessorKey: "isolationTier",
      header: ({ column }) => <DataTableColumnHeader column={column} title="Isolation" />,
      cell: ({ row }) => <Badge variant={row.original.isolationTier === "SILO" ? "accent" : "default"}>{row.original.isolationTier}</Badge>,
    },
    {
      accessorKey: "region",
      header: ({ column }) => <DataTableColumnHeader column={column} title="Region" />,
    },
    {
      accessorKey: "status",
      header: ({ column }) => <DataTableColumnHeader column={column} title="Status" />,
      cell: ({ row }) => <StatusBadge status={row.original.status} />,
    },
    {
      id: "actions",
      enableHiding: false,
      cell: ({ row }) => {
        const tenant = row.original;
        const migratingThis = migrate.isPending && migrate.variables === tenant.tenantId;
        return (
          <div className="flex items-center gap-2">
            {tenant.status === "PROVISIONING" && (
              <Button size="sm" isLoading={activate.isPending && activate.variables === tenant.tenantId} onClick={() => activate.mutate(tenant.tenantId)}>
                Activate
              </Button>
            )}
            {tenant.isolationTier === "POOLED" && tenant.status === "ACTIVE" && (
              <Button size="sm" isLoading={migratingThis} disabled={migratingThis} onClick={() => migrate.mutate(tenant.tenantId)}>
                {migratingThis ? "Migrating…" : "Migrate to Silo"}
              </Button>
            )}
            {tenant.status !== "OFFBOARDED" && tenant.status !== "MIGRATING" && (
              <Button
                variant="danger"
                size="sm"
                isLoading={offboard.isPending && offboard.variables === tenant.tenantId}
                onClick={() => offboard.mutate(tenant.tenantId)}
              >
                Offboard
              </Button>
            )}
          </div>
        );
      },
    },
  ];
}
