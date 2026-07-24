import { Link } from "react-router-dom";
import type { ColumnDef } from "@tanstack/react-table";
import type { Certificate } from "../../../api/types";
import { DataTableColumnHeader } from "../../../components/data-table/DataTableColumnHeader";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { Button } from "../../../components/ui/button";
import { RevokeCertificateDialog } from "./RevokeCertificateDialog";

export function buildCertificateColumns(canRevoke: boolean): ColumnDef<Certificate>[] {
  return [
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
      accessorKey: "issuedAt",
      header: ({ column }) => <DataTableColumnHeader column={column} title="Issued" />,
      cell: ({ row }) => new Date(row.original.issuedAt).toLocaleString(),
    },
    {
      id: "pathSteps",
      header: "Path steps",
      enableSorting: false,
      cell: ({ row }) => row.original.realizedStepCourseIds?.join(" → ") ?? "—",
    },
    {
      id: "actions",
      enableHiding: false,
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" asChild>
            <Link to={`/verify?certificateId=${row.original.certificateId}`}>Verify</Link>
          </Button>
          {canRevoke && row.original.status === "ISSUED" && <RevokeCertificateDialog certificateId={row.original.certificateId} />}
        </div>
      ),
    },
  ];
}
