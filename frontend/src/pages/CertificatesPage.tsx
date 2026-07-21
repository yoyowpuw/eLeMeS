import { Link } from "react-router-dom";
import { useMyEnrollments } from "../api/enrollments";
import { useCertificateByEnrollment } from "../api/certificates";
import type { Enrollment } from "../api/types";

function CertificateRow({ enrollment }: { enrollment: Enrollment }) {
  const { data: certificate } = useCertificateByEnrollment(enrollment.enrollmentId, enrollment.status === "COMPLETED");
  if (!certificate) return null;

  return (
    <li className="card">
      <div>Course: {certificate.courseId}</div>
      <div>Status: <strong>{certificate.status}</strong></div>
      <div>Issued: {new Date(certificate.issuedAt).toLocaleString()}</div>
      {certificate.realizedStepCourseIds && (
        <div>Learning Path steps completed: {certificate.realizedStepCourseIds.join(" → ")}</div>
      )}
      <div className="entity-actions">
        <Link to={`/verify?certificateId=${certificate.certificateId}`}>Verify</Link>
      </div>
    </li>
  );
}

export function CertificatesPage() {
  const { data: enrollments } = useMyEnrollments();

  return (
    <div>
      <h1>My Certificates</h1>
      {enrollments.length === 0 ? (
        <p>No enrollments this session yet.</p>
      ) : (
        <ul className="entity-list">
          {enrollments.map((enrollment) => (
            <CertificateRow key={enrollment.enrollmentId} enrollment={enrollment} />
          ))}
        </ul>
      )}
    </div>
  );
}
