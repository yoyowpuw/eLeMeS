import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useCreatePath, useKnownPaths } from "../api/paths";
import { useKnownCourses } from "../api/courses";
import { rolesFromAccessToken } from "../auth/jwt";
import { ApiError } from "../api/http";

export function PathsPage() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const canAuthor = roles.includes("admin") || roles.includes("manager");
  const { data: courses } = useKnownCourses();
  const { data: paths } = useKnownPaths();
  const createPath = useCreatePath();

  const [name, setName] = useState("");
  const [steps, setSteps] = useState<string[]>([""]);

  function updateStep(index: number, courseId: string) {
    setSteps((prev) => prev.map((step, i) => (i === index ? courseId : step)));
  }

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    const courseIds = steps.filter(Boolean);
    createPath.mutate({ name, courseIds });
    setName("");
    setSteps([""]);
  }

  return (
    <div>
      <h1>Learning Paths</h1>
      <p>Ordered, multi-step sequences of courses — a learner completes each step's course in order.</p>

      {canAuthor ? (
        <form onSubmit={handleCreate} className="card">
          <h2>Create a path</h2>
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </label>
          {steps.map((step, index) => (
            <label key={index}>
              Step {index + 1}
              <select value={step} onChange={(e) => updateStep(index, e.target.value)} required>
                <option value="">— choose a course —</option>
                {courses.map((course) => (
                  <option key={course.courseId} value={course.courseId}>
                    {course.code} — {course.title}
                  </option>
                ))}
              </select>
            </label>
          ))}
          <button type="button" onClick={() => setSteps((prev) => [...prev, ""])} disabled={courses.length === 0}>
            + Add step
          </button>
          <button type="submit" disabled={createPath.isPending || steps.every((s) => !s)}>
            {createPath.isPending ? "Creating…" : "Create path"}
          </button>
          {courses.length === 0 && <p>Create at least one course first (see the Courses page).</p>}
          {createPath.isError && (
            <p role="alert">{createPath.error instanceof ApiError ? createPath.error.message : "Failed to create path"}</p>
          )}
        </form>
      ) : (
        <p>Only an admin or manager can create learning paths — you're signed in as a learner.</p>
      )}

      <h2>Paths created this session</h2>
      {paths.length === 0 ? (
        <p>No paths yet.</p>
      ) : (
        <ul className="entity-list">
          {paths.map((path) => (
            <li key={path.pathId} className="card">
              <strong>{path.name}</strong>
              <div className="entity-actions">
                <Link to={`/paths/${path.pathId}/enroll`}>Enroll in this path</Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
