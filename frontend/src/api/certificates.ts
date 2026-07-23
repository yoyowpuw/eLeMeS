import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, ApiError, SERVICE_URLS } from "./http";
import type { Certificate } from "./types";

export function useCertificateByEnrollment(enrollmentId: string | undefined, enabled: boolean) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["certificateByEnrollment", enrollmentId],
    queryFn: () => apiFetch<Certificate>(SERVICE_URLS.certificates, `/api/v1/certificates/by-enrollment/${enrollmentId}`, auth.user?.access_token),
    enabled: !!enrollmentId && !!auth.user && enabled,
    retry: false,
  });
}

/**
 * Ch.26 §6: deliberately callable with NO access token at all — this is
 * the one screen in the app that works for a signed-out visitor, proving
 * a certificate is independently verifiable without platform access.
 */
export function useVerifyCertificate(certificateId: string | undefined) {
  return useQuery({
    queryKey: ["verify", certificateId],
    queryFn: () => apiFetch<{ valid: boolean }>(SERVICE_URLS.certificates, `/api/v1/certificates/${certificateId}/verify`, undefined),
    enabled: !!certificateId,
    retry: false,
  });
}

export function useCertificate(certificateId: string | undefined) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["certificate", certificateId],
    queryFn: () => apiFetch<Certificate>(SERVICE_URLS.certificates, `/api/v1/certificates/${certificateId}`, auth.user?.access_token),
    enabled: !!certificateId && !!auth.user,
    retry: false,
  });
}

/** Ch.19: the backend independently enforces this is admin-tenant-wide / manager-subtree-only — this button being visible is a UX convenience, not the actual authorization boundary. */
export function useRevokeCertificate() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ certificateId, reason }: { certificateId: string; reason: string }) =>
      apiFetch<Certificate>(SERVICE_URLS.certificates, `/api/v1/certificates/${certificateId}/revoke`, auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify({ reason }),
      }),
    onSuccess: (certificate) => {
      queryClient.setQueryData(["certificateByEnrollment", certificate.enrollmentId], certificate);
    },
  });
}

export { ApiError };
