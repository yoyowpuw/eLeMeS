import { Link } from "react-router-dom";
import type { ColumnDef } from "@tanstack/react-table";
import type { Course } from "../../../api/types";
import { DataTableColumnHeader } from "../../../components/data-table/DataTableColumnHeader";
import { Button } from "../../../components/ui/button";

export const courseColumns: ColumnDef<Course>[] = [
  {
    accessorKey: "code",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Code" />,
    cell: ({ row }) => <code className="text-xs">{row.original.code}</code>,
  },
  {
    accessorKey: "title",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Title" />,
    cell: ({ row }) => <span className="font-medium text-slate-900 dark:text-slate-100">{row.original.title}</span>,
  },
  {
    id: "actions",
    enableHiding: false,
    cell: ({ row }) => (
      <Button variant="outline" size="sm" asChild>
        <Link to={`/enrollments?courseId=${row.original.courseId}`}>Enroll</Link>
      </Button>
    ),
  },
];
