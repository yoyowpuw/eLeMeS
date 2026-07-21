import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useCreateCourse, useKnownCourses } from "../api/courses";
import { rolesFromAccessToken } from "../auth/jwt";
import { ApiError } from "../api/http";

export function CoursesPage() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const canAuthor = roles.includes("admin") || roles.includes("manager");
  const { data: courses } = useKnownCourses();
  const createCourse = useCreateCourse();

  const [code, setCode] = useState("");
  const [title, setTitle] = useState("");

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    createCourse.mutate({ code, title, initialContentHash: `sha256-${crypto.randomUUID()}` });
    setCode("");
    setTitle("");
  }

  return (
    <div>
      <h1>Courses</h1>

      {canAuthor && (
        <form onSubmit={handleCreate} className="card">
          <h2>Create a course</h2>
          <label>
            Code
            <input value={code} onChange={(e) => setCode(e.target.value)} required />
          </label>
          <label>
            Title
            <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </label>
          <button type="submit" disabled={createCourse.isPending}>
            {createCourse.isPending ? "Creating…" : "Create course"}
          </button>
          {createCourse.isError && (
            <p role="alert">
              {createCourse.error instanceof ApiError ? createCourse.error.message : "Failed to create course"}
            </p>
          )}
        </form>
      )}
      {!canAuthor && <p>Only an admin or manager can create courses — you're signed in as a learner.</p>}

      <h2>Courses created this session</h2>
      {courses.length === 0 ? (
        <p>No courses yet. {canAuthor ? "Create one above." : "Ask an admin/manager to create one."}</p>
      ) : (
        <ul className="entity-list">
          {courses.map((course) => (
            <li key={course.courseId} className="card">
              <strong>{course.code}</strong> — {course.title}
              <div className="entity-actions">
                <Link to={`/enrollments?courseId=${course.courseId}`}>Enroll in this course</Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
