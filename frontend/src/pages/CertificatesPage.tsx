import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useMyEnrollments } from "../api/enrollments";
import { useCertificateByEnrollment, useRevokeCertificate } from "../api/certificates";
import { rolesFromAccessToken } from "../auth/jwt";
import { ApiError } from "../api/http";
import type { Enrollment } from "../api/types";

function CertificateRow({ enrollment, canRevoke }: { enrollment: Enrollment; canRevoke: boolean }) {
  const { data: certificate } = useCertificateByEnrollment(enrollment.enrollmentId, enrollment.status === "COMPLETED");
  const revoke = useRevokeCertificate();
  const [reason, setReason] = useState("");
  const [showRevokeForm, setShowRevokeForm] = useState(false);

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
        {canRevoke && certificate.status === "ISSUED" && !showRevokeForm && (
          <button className="button-danger" onClick={() => setShowRevokeForm(true)}>Revoke…</button>
        )}
      </div>
      {showRevokeForm && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            revoke.mutate({ certificateId: certificate.certificateId, reason });
          }}
        >
          <label>
            Reason
            <input value={reason} onChange={(e) => setReason(e.target.value)} required />
          </label>
          <button type="submit" className="button-danger" disabled={revoke.isPending}>
            {revoke.isPending ? "Revoking…" : "Confirm revoke"}
          </button>
          {revoke.isError && (
            <p role="alert">
              {revoke.error instanceof ApiError ? revoke.error.message : "Failed to revoke — you may be outside this certificate's org scope"}
            </p>
          )}
        </form>
      )}
    </li>
  );
}

export function CertificatesPage() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const canRevoke = roles.includes("admin") || roles.includes("manager");
  const { data: enrollments } = useMyEnrollments();

  return (
    <div>
      <h1>My Certificates</h1>
      {enrollments.length === 0 ? (
        <p>No enrollments this session yet.</p>
      ) : (
        <ul className="entity-list">
          {enrollments.map((enrollment) => (
            <CertificateRow key={enrollment.enrollmentId} enrollment={enrollment} canRevoke={canRevoke} />
          ))}
        </ul>
      )}
    </div>
  );
}
