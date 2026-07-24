import { Link } from "react-router-dom";
import type { ColumnDef } from "@tanstack/react-table";
import type { OrgUnit } from "../../../api/types";
import { DataTableColumnHeader } from "../../../components/data-table/DataTableColumnHeader";
import { Button } from "../../../components/ui/button";

export const orgUnitColumns: ColumnDef<OrgUnit>[] = [
  {
    accessorKey: "name",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
    cell: ({ row }) => <span className="font-medium text-slate-900 dark:text-slate-100">{row.original.name}</span>,
  },
  {
    accessorKey: "unitType",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Type" />,
  },
  {
    accessorKey: "managerUserId",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Manager" />,
    cell: ({ row }) => row.original.managerUserId ?? "—",
  },
  {
    id: "actions",
    enableHiding: false,
    cell: ({ row }) => (
      // Relative, not absolute — this page is mounted at both /manage/org-units and /admin/org-units.
      <Button variant="outline" size="sm" asChild>
        <Link to={row.original.orgUnitId}>Open</Link>
      </Button>
    ),
  },
];
