import { Link, useParams } from "react-router-dom";
import { usePathProgress } from "../api/pathEnrollments";

export function PathProgressPage() {
  const { pathProgressId } = useParams<{ pathProgressId: string }>();
  const { data: progress, isLoading } = usePathProgress(pathProgressId);

  if (isLoading) return <p>Loading…</p>;
  if (!progress) return <p>Path progress not found.</p>;

  return (
    <div>
      <h1>Path Progress</h1>
      <div className="card">
        <div>Status: <strong>{progress.status}</strong></div>
        <div>Step {Math.min(progress.currentStepIndex + 1, progress.totalSteps)} of {progress.totalSteps}</div>
        {progress.realizedStepCourseIds.length > 0 && (
          <div>Completed so far: {progress.realizedStepCourseIds.join(" → ")}</div>
        )}

        {progress.status === "COMPLETED" ? (
          <p>
            Path complete — the final certificate carries this exact realized step sequence.{" "}
            <Link to="/certificates">View your certificates</Link>.
          </p>
        ) : (
          progress.currentStepEnrollmentId && (
            <Link to={`/enrollments/${progress.currentStepEnrollmentId}`}>
              Continue current step →
            </Link>
          )
        )}
      </div>
    </div>
  );
}
