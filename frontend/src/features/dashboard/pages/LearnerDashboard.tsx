import { Link } from "react-router-dom";
import { Award, BookOpen, Route, ShieldCheck } from "lucide-react";
import { useMyEnrollments } from "../../../api/enrollments";
import { useMyCertificates } from "../../../features/certificates/useMyCertificates";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Button } from "../../../components/ui/button";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { Progress } from "../../../components/ui/progress";
import { EmptyState } from "../../../components/feedback/EmptyState";

/**
 * Real data only — every number here traces to an actual query hook, no
 * fabricated stats. "Continue learning" is ASSIGNED/IN_PROGRESS enrollments
 * this session (no backend list endpoint exists, so this genuinely is
 * "this session," not silently capped real data); certificates reuse the
 * same useMyCertificates() the Certificates table uses.
 */
export function LearnerDashboard() {
  const { data: enrollments } = useMyEnrollments();
  const certificates = useMyCertificates();
  const inProgress = enrollments.filter((e) => e.status === "ASSIGNED" || e.status === "IN_PROGRESS");

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Welcome back</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">Your learning this session.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Continue learning</CardTitle>
        </CardHeader>
        <CardContent>
          {inProgress.length === 0 ? (
            <EmptyState
              icon={BookOpen}
              title="Nothing in progress"
              description="Browse the catalog to enroll in a course."
              action={
                <Button variant="outline" size="sm" asChild>
                  <Link to="/courses">Browse courses</Link>
                </Button>
              }
            />
          ) : (
            <ul className="flex flex-col gap-3">
              {inProgress.map((enrollment) => (
                <li key={enrollment.enrollmentId} className="flex items-center gap-4 rounded-md border border-slate-200 p-3 dark:border-slate-800">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <code className="text-xs">{enrollment.courseId}</code>
                      <StatusBadge status={enrollment.status} />
                    </div>
                    <Progress value={enrollment.progressPercent} className="mt-2 max-w-64" />
                  </div>
                  <Button size="sm" asChild>
                    <Link to={`/enrollments/${enrollment.enrollmentId}`}>Resume</Link>
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Your certificates ({certificates.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {certificates.length === 0 ? (
            <EmptyState icon={Award} title="No certificates yet" description="Complete a course to earn one." />
          ) : (
            <ul className="flex flex-col gap-2">
              {certificates.slice(0, 5).map((certificate) => (
                <li key={certificate.certificateId} className="flex items-center justify-between text-sm">
                  <code className="text-xs">{certificate.courseId}</code>
                  <Link to={`/verify?certificateId=${certificate.certificateId}`} className="text-blue-600 hover:underline">
                    Verify
                  </Link>
                </li>
              ))}
            </ul>
          )}
          {certificates.length > 0 && (
            <Button variant="link" size="sm" asChild className="mt-2 px-0">
              <Link to="/certificates">View all certificates</Link>
            </Button>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Button variant="outline" className="justify-start gap-2" asChild>
          <Link to="/courses">
            <BookOpen className="size-4" /> Browse courses
          </Link>
        </Button>
        <Button variant="outline" className="justify-start gap-2" asChild>
          <Link to="/paths">
            <Route className="size-4" /> Learning paths
          </Link>
        </Button>
        <Button variant="outline" className="justify-start gap-2" asChild>
          <Link to="/verify">
            <ShieldCheck className="size-4" /> Verify a certificate
          </Link>
        </Button>
      </div>
    </div>
  );
}
