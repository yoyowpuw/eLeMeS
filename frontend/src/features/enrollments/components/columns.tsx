import { Link } from "react-router-dom";
import type { ColumnDef } from "@tanstack/react-table";
import type { Enrollment } from "../../../api/types";
import { DataTableColumnHeader } from "../../../components/data-table/DataTableColumnHeader";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { Progress } from "../../../components/ui/progress";
import { Button } from "../../../components/ui/button";

export const enrollmentColumns: ColumnDef<Enrollment>[] = [
  {
    accessorKey: "courseId",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Course" />,
    cell: ({ row }) => <code className="text-xs">{row.original.courseId}</code>,
  },
  {
    accessorKey: "status",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Status" />,
    cell: ({ row }) => <StatusBadge status={row.original.status} />,
  },
  {
    accessorKey: "progressPercent",
    header: ({ column }) => <DataTableColumnHeader column={column} title="Progress" />,
    cell: ({ row }) => (
      <div className="flex items-center gap-2">
        <Progress value={row.original.progressPercent} className="w-24" />
        <span className="text-xs text-slate-500 dark:text-slate-400">{row.original.progressPercent}%</span>
      </div>
    ),
  },
  {
    id: "actions",
    enableHiding: false,
    cell: ({ row }) => (
      <Button variant="outline" size="sm" asChild>
        <Link to={`/enrollments/${row.original.enrollmentId}`}>Open</Link>
      </Button>
    ),
  },
];
