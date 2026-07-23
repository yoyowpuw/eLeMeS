import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, SERVICE_URLS } from "./http";
import type { PathProgress } from "./types";

interface EnrollInPathResponse {
  pathProgressId: string;
  pathId: string;
  pathVersionId: string;
  status: string;
  currentStepEnrollmentId: string;
  currentStepCourseId: string;
}

export function useEnrollInPath() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: { learnerId: string; pathId: string }) =>
      apiFetch<EnrollInPathResponse>(SERVICE_URLS.enrollments, "/api/v1/path-enrollments", auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify(body),
      }),
    onSuccess: (enrollment) => {
      queryClient.setQueryData(["myPathEnrollments"], (existing: EnrollInPathResponse[] = []) => [...existing, enrollment]);
    },
  });
}

export function usePathProgress(pathProgressId: string | undefined) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["pathProgress", pathProgressId],
    queryFn: () => apiFetch<PathProgress>(SERVICE_URLS.enrollments, `/api/v1/path-enrollments/${pathProgressId}`, auth.user?.access_token),
    enabled: !!pathProgressId && !!auth.user,
    // Same reasoning as useEnrollment's polling — a step's completion
    // auto-advances PathProgress server-side with nothing pushed to the
    // client, so the UI has to ask again to notice.
    refetchInterval: (query) => (query.state.data?.status === "COMPLETED" ? false : 2000),
  });
}

/** Same session-scoped-list approach as `useMyEnrollments` — no "list my path enrollments" endpoint exists. */
export function useMyPathEnrollments() {
  return useQuery<EnrollInPathResponse[]>({
    queryKey: ["myPathEnrollments"],
    queryFn: () => Promise.resolve([]),
    staleTime: Infinity,
    initialData: [],
  });
}
