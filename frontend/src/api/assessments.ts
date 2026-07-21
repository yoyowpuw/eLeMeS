import { useMutation, useQuery } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, SERVICE_URLS } from "./http";
import type { Assessment, Question } from "./types";

export function useAssessment(assessmentId: string | undefined) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["assessment", assessmentId],
    queryFn: () => apiFetch<Assessment>(SERVICE_URLS.assessments, `/api/v1/assessments/${assessmentId}`, auth.user?.access_token),
    enabled: !!assessmentId && !!auth.user,
  });
}

export function useStartAssessment() {
  const auth = useAuth();
  return useMutation({
    mutationFn: (body: { enrollmentId: string; courseId: string; questions: Omit<Question, never>[]; passingScore: number }) =>
      apiFetch<Assessment>(SERVICE_URLS.assessments, "/api/v1/assessments", auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify(body),
      }),
  });
}

export function useSubmitAssessment() {
  const auth = useAuth();
  return useMutation({
    mutationFn: ({ assessmentId, answers }: { assessmentId: string; answers: Record<string, number> }) =>
      apiFetch<Assessment>(SERVICE_URLS.assessments, `/api/v1/assessments/${assessmentId}/submit`, auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify({ answers }),
      }),
  });
}
