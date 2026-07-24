import { useState } from "react";
import { Link } from "react-router-dom";
import { Network, Search } from "lucide-react";
import { DEFAULT_HIERARCHY_TYPE, useKnownOrgUnits, useMyScope } from "../../../api/orgUnits";
import { useCertificateByEnrollment } from "../../../api/certificates";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { EmptyState } from "../../../components/feedback/EmptyState";
import { RevokeCertificateDialog } from "../../../features/certificates/components/RevokeCertificateDialog";

/**
 * NFR-035/FR-033 asks for a team-compliance overview, but there is no
 * endpoint anywhere in the 6 services that lists learners/enrollments by
 * org unit — confirmed during the redesign's research pass. Rather than
 * fabricate headcount/completion numbers with nothing behind them, this
 * dashboard shows only what's real: the org-unit *structure* this manager
 * scopes over (Ch.19's my-scope), plus a lookup tool that reuses the
 * already-real revoke capability. A genuine compliance rollup is a
 * backend gap to raise, not something to paper over here.
 */
export function ManagerDashboard() {
  const { data: scopeIds } = useMyScope(DEFAULT_HIERARCHY_TYPE);
  const { data: knownUnits } = useKnownOrgUnits();
  const [lookupId, setLookupId] = useState("");
  const [submittedId, setSubmittedId] = useState<string | undefined>();
  const { data: certificate, isFetching, isError } = useCertificateByEnrollment(submittedId, !!submittedId);

  const scopedUnits = (scopeIds ?? []).map((id) => knownUnits.find((u) => u.orgUnitId === id) ?? { orgUnitId: id, name: id, unitType: undefined });

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Team Management</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">Org units in your scope, and quick actions for your team.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Your org units ({scopedUnits.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {scopedUnits.length === 0 ? (
            <EmptyState icon={Network} title="No org units in scope" description="Ask an admin to assign you as a manager on a unit." />
          ) : (
            <ul className="flex flex-col gap-2">
              {scopedUnits.map((unit) => (
                <li key={unit.orgUnitId} className="flex items-center justify-between text-sm">
                  <span>
                    {unit.name} {unit.unitType && <span className="text-slate-500 dark:text-slate-400">({unit.unitType})</span>}
                  </span>
                  <Button variant="link" size="sm" asChild className="px-0">
                    <Link to={`/manage/org-units/${unit.orgUnitId}`}>Open</Link>
                  </Button>
                </li>
              ))}
            </ul>
          )}
          <Button variant="outline" size="sm" asChild className="mt-3">
            <Link to="/manage/org-units">Manage org hierarchy</Link>
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Certificate lookup</CardTitle>
          <p className="text-sm text-slate-500 dark:text-slate-400">Look up a certificate by enrollment ID and revoke it if needed.</p>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              setSubmittedId(lookupId);
            }}
            className="flex items-end gap-2"
          >
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="lookup-enrollment-id">Enrollment ID</Label>
              <Input id="lookup-enrollment-id" value={lookupId} onChange={(e) => setLookupId(e.target.value)} required />
            </div>
            <Button type="submit" variant="outline" isLoading={isFetching} className="gap-1.5">
              <Search className="size-4" />
              Look up
            </Button>
          </form>
          {isError && submittedId && (
            <p role="alert" className="text-sm text-red-600">
              {"No certificate found for that enrollment"}
            </p>
          )}
          {certificate && (
            <div className="flex items-center justify-between rounded-md border border-slate-200 p-3 text-sm dark:border-slate-800">
              <div className="flex items-center gap-2">
                <code className="text-xs">{certificate.courseId}</code>
                <StatusBadge status={certificate.status} />
              </div>
              {certificate.status === "ISSUED" && <RevokeCertificateDialog certificateId={certificate.certificateId} />}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Button variant="outline" className="justify-start gap-2" asChild>
          <Link to="/courses">Create a course</Link>
        </Button>
        <Button variant="outline" className="justify-start gap-2" asChild>
          <Link to="/paths">Create a learning path</Link>
        </Button>
        <Button variant="outline" className="justify-start gap-2" asChild>
          <Link to="/manage/org-units">Create an org unit</Link>
        </Button>
      </div>
    </div>
  );
}
