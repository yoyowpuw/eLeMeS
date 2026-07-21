import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, SERVICE_URLS } from "./http";
import type { Enrollment } from "./types";

export function useEnrollment(enrollmentId: string | undefined) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["enrollment", enrollmentId],
    queryFn: () => apiFetch<Enrollment>(SERVICE_URLS.enrollments, `/api/v1/enrollments/${enrollmentId}`, auth.user?.access_token),
    enabled: !!enrollmentId && !!auth.user,
    // Polling: certificate issuance happens two Kafka hops after completion,
    // and (via the assessment path) enrollment status itself changes
    // asynchronously too — short polling is the simplest way for the UI to
    // reflect that without building a websocket/SSE channel for this slice.
    refetchInterval: (query) => (query.state.data?.status === "COMPLETED" ? false : 2000),
  });
}

export function useEnroll() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: { learnerId: string; courseId: string }) =>
      apiFetch<Enrollment>(SERVICE_URLS.enrollments, "/api/v1/enrollments", auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify(body),
      }),
    onSuccess: (enrollment) => {
      queryClient.setQueryData(["myEnrollments"], (existing: Enrollment[] = []) => [...existing, enrollment]);
    },
  });
}

export function useStartEnrollment() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (enrollmentId: string) =>
      apiFetch<Enrollment>(SERVICE_URLS.enrollments, `/api/v1/enrollments/${enrollmentId}/start`, auth.user?.access_token, {
        method: "POST",
      }),
    onSuccess: (enrollment) => queryClient.setQueryData(["enrollment", enrollment.enrollmentId], enrollment),
  });
}

export function useCompleteEnrollment() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (enrollmentId: string) =>
      apiFetch<Enrollment>(SERVICE_URLS.enrollments, `/api/v1/enrollments/${enrollmentId}/complete`, auth.user?.access_token, {
        method: "POST",
      }),
    onSuccess: (enrollment) => queryClient.setQueryData(["enrollment", enrollment.enrollmentId], enrollment),
  });
}

/** Same session-scoped-list approach as `useKnownCourses` — no "list my enrollments" backend endpoint exists yet. */
export function useMyEnrollments() {
  return useQuery<Enrollment[]>({
    queryKey: ["myEnrollments"],
    queryFn: () => Promise.resolve([]),
    staleTime: Infinity,
    initialData: [],
  });
}
