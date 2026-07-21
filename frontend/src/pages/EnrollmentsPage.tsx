import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useEnroll, useMyEnrollments } from "../api/enrollments";
import { ApiError } from "../api/http";

export function EnrollmentsPage() {
  const auth = useAuth();
  const [searchParams] = useSearchParams();
  const { data: enrollments } = useMyEnrollments();
  const enroll = useEnroll();

  const [courseId, setCourseId] = useState(searchParams.get("courseId") ?? "");
  const learnerId = auth.user?.profile.preferred_username ?? "";

  function handleEnroll(e: React.FormEvent) {
    e.preventDefault();
    enroll.mutate({ learnerId, courseId });
    setCourseId("");
  }

  return (
    <div>
      <h1>My Enrollments</h1>

      <form onSubmit={handleEnroll} className="card">
        <h2>Enroll in a course</h2>
        <label>
          Course ID
          <input value={courseId} onChange={(e) => setCourseId(e.target.value)} required placeholder="from the Courses page" />
        </label>
        <button type="submit" disabled={enroll.isPending || !courseId}>
          {enroll.isPending ? "Enrolling…" : "Enroll"}
        </button>
        {enroll.isError && (
          <p role="alert">{enroll.error instanceof ApiError ? enroll.error.message : "Failed to enroll"}</p>
        )}
      </form>

      <h2>Enrollments this session</h2>
      {enrollments.length === 0 ? (
        <p>No enrollments yet — enroll in a course above.</p>
      ) : (
        <ul className="entity-list">
          {enrollments.map((enrollment) => (
            <li key={enrollment.enrollmentId} className="card">
              <div>Course: {enrollment.courseId}</div>
              <div>Status: <strong>{enrollment.status}</strong> ({enrollment.progressPercent}%)</div>
              <div className="entity-actions">
                <Link to={`/enrollments/${enrollment.enrollmentId}`}>Open</Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
