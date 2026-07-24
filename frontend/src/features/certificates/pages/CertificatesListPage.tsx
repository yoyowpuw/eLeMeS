import { useMemo } from "react";
import { useRoles } from "../../../auth/useRoles";
import { useMyCertificates } from "../useMyCertificates";
import { buildCertificateColumns } from "../components/columns";
import { DataTable } from "../../../components/data-table/DataTable";

export function CertificatesListPage() {
  const { isManager, isAdmin } = useRoles();
  const canRevoke = isManager || isAdmin;
  const certificates = useMyCertificates();
  const columns = useMemo(() => buildCertificateColumns(canRevoke), [canRevoke]);

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">My Certificates</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">Certificates earned from completed enrollments this session.</p>
      </div>
      <DataTable
        columns={columns}
        data={certificates}
        searchPlaceholder="Search certificates…"
        exportFilename="certificates"
        emptyTitle="No certificates yet"
        emptyDescription="Complete a course to earn one."
      />
    </div>
  );
}
