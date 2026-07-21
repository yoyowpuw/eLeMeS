import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useVerifyCertificate } from "../api/certificates";

/**
 * Ch.26 §6: no `<RequireAuth>` wrapper — this page must work for a signed-out
 * visitor, since a certificate is supposed to be independently verifiable
 * by anyone (an employer, a regulator) with no platform access at all.
 */
export function VerifyPage() {
  const [searchParams] = useSearchParams();
  const [certificateId, setCertificateId] = useState(searchParams.get("certificateId") ?? "");
  const [submittedId, setSubmittedId] = useState(searchParams.get("certificateId") ?? undefined);
  const { data, isFetching, isError } = useVerifyCertificate(submittedId);

  return (
    <div>
      <h1>Verify a Certificate</h1>
      <p>No sign-in required — paste a certificate ID to independently check its authenticity.</p>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          setSubmittedId(certificateId);
        }}
        className="card"
      >
        <label>
          Certificate ID
          <input value={certificateId} onChange={(e) => setCertificateId(e.target.value)} required />
        </label>
        <button type="submit">Verify</button>
      </form>

      {isFetching && <p>Checking…</p>}
      {isError && <p role="alert">Could not verify — certificate not found.</p>}
      {data && (
        <p className={data.valid ? "verify-result verify-result--valid" : "verify-result verify-result--invalid"}>
          {data.valid ? "✓ Valid — signature checks out." : "✗ Invalid — signature does not match."}
        </p>
      )}
    </div>
  );
}
