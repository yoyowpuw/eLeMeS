import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { usePathCurrentVersion } from "../../../api/paths";
import { useEnrollInPath, useMyPathEnrollments } from "../../../api/pathEnrollments";
import { ApiError } from "../../../api/http";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Button } from "../../../components/ui/button";
import { Timeline } from "../../../components/feedback/Timeline";
import { Skeleton } from "../../../components/ui/skeleton";

export function PathEnrollPage() {
  const { pathId } = useParams<{ pathId: string }>();
  const auth = useAuth();
  const navigate = useNavigate();
  const { data: version, isLoading } = usePathCurrentVersion(pathId);
  const enrollInPath = useEnrollInPath();
  const { data: myPathEnrollments } = useMyPathEnrollments();

  const learnerId = auth.user?.profile.preferred_username ?? "";
  const alreadyEnrolled = myPathEnrollments.find((e) => e.pathId === pathId);

  function handleEnroll() {
    if (!pathId) return;
    enrollInPath.mutate({ learnerId, pathId }, { onSuccess: (enrollment) => navigate(`/path-enrollments/${enrollment.pathProgressId}`) });
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full max-w-lg" />
      </div>
    );
  }
  if (!version) return <p className="text-sm text-slate-500">Path not found.</p>;

  return (
    <div className="flex max-w-lg flex-col gap-4">
      <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Enroll in a Learning Path</h1>
      <Card>
        <CardHeader>
          <CardTitle>Steps ({version.steps.length})</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Timeline steps={version.steps.map((step) => ({ label: step.courseId, status: "upcoming" }))} />
          {alreadyEnrolled ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Already enrolled —{" "}
              <Link to={`/path-enrollments/${alreadyEnrolled.pathProgressId}`} className="text-blue-600 hover:underline">
                view progress
              </Link>
              .
            </p>
          ) : (
            <Button onClick={handleEnroll} isLoading={enrollInPath.isPending} className="self-start">
              {enrollInPath.isPending ? "Enrolling…" : "Enroll in this path"}
            </Button>
          )}
          {enrollInPath.isError && (
            <p role="alert" className="text-sm text-red-600">
              {enrollInPath.error instanceof ApiError ? enrollInPath.error.message : "Failed to enroll"}
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
