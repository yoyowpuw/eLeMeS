import { useQueries } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, SERVICE_URLS } from "../../api/http";
import { useMyEnrollments } from "../../api/enrollments";
import type { Certificate } from "../../api/types";

/**
 * A DataTable needs its full row array up front, but certificates are only
 * fetchable one enrollment at a time (`GET /certificates/by-enrollment/:id`,
 * no tenant-wide list endpoint) — `useQueries` runs one query per completed
 * enrollment in parallel and this hook flattens the results into a single
 * array. Same queryKey shape as `useCertificateByEnrollment` in
 * api/certificates.ts (["certificateByEnrollment", enrollmentId]) so the
 * cache entries are shared, not duplicated, with the enrollment detail page.
 */
export function useMyCertificates() {
  const auth = useAuth();
  const { data: enrollments } = useMyEnrollments();
  const completed = enrollments.filter((e) => e.status === "COMPLETED");

  const results = useQueries({
    queries: completed.map((enrollment) => ({
      queryKey: ["certificateByEnrollment", enrollment.enrollmentId],
      queryFn: () =>
        apiFetch<Certificate>(SERVICE_URLS.certificates, `/api/v1/certificates/by-enrollment/${enrollment.enrollmentId}`, auth.user?.access_token),
      enabled: !!auth.user,
      retry: false,
    })),
  });

  return results.map((r) => r.data).filter((c): c is Certificate => !!c);
}
