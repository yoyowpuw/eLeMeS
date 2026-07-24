import { Link } from "react-router-dom";
import type { ColumnDef } from "@tanstack/react-table";
import type { LearningPath } from "../../../api/types";
import { DataTableColumnHeader } from "../../../components/data-table/DataTableColumnHeader";
import { Button } from "../../../components/ui/button";

export const pathColumns: ColumnDef<LearningPath>[] = [
  {
    accessorKey: "name",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
    cell: ({ row }) => <span className="font-medium text-slate-900 dark:text-slate-100">{row.original.name}</span>,
  },
  {
    id: "actions",
    enableHiding: false,
    cell: ({ row }) => (
      <Button variant="outline" size="sm" asChild>
        <Link to={`/paths/${row.original.pathId}/enroll`}>Enroll</Link>
      </Button>
    ),
  },
];
