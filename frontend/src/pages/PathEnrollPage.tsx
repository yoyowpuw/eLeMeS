import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { usePathCurrentVersion } from "../api/paths";
import { useEnrollInPath, useMyPathEnrollments } from "../api/pathEnrollments";
import { ApiError } from "../api/http";

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
    enrollInPath.mutate(
      { learnerId, pathId },
      { onSuccess: (enrollment) => navigate(`/path-enrollments/${enrollment.pathProgressId}`) },
    );
  }

  if (isLoading) return <p>Loading…</p>;
  if (!version) return <p>Path not found.</p>;

  return (
    <div>
      <h1>Enroll in a Learning Path</h1>
      <div className="card">
        <h2>Steps ({version.steps.length})</h2>
        <ol>
          {version.steps.map((step) => (
            <li key={step.stepOrder}>{step.courseId}</li>
          ))}
        </ol>
        {alreadyEnrolled ? (
          <p>Already enrolled — <Link to={`/path-enrollments/${alreadyEnrolled.pathProgressId}`}>view progress</Link>.</p>
        ) : (
          <button onClick={handleEnroll} disabled={enrollInPath.isPending}>
            {enrollInPath.isPending ? "Enrolling…" : "Enroll in this path"}
          </button>
        )}
        {enrollInPath.isError && (
          <p role="alert">{enrollInPath.error instanceof ApiError ? enrollInPath.error.message : "Failed to enroll"}</p>
        )}
      </div>
    </div>
  );
}
