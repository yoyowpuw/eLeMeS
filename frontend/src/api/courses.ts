import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, SERVICE_URLS } from "./http";
import type { Course } from "./types";

export function useCourse(courseId: string | undefined) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["course", courseId],
    queryFn: () => apiFetch<Course>(SERVICE_URLS.courses, `/api/v1/courses/${courseId}`, auth.user?.access_token),
    enabled: !!courseId && !!auth.user,
  });
}

export function useCreateCourse() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: { code: string; title: string; initialContentHash: string }) =>
      apiFetch<Course>(SERVICE_URLS.courses, "/api/v1/courses", auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify(body),
      }),
    onSuccess: (course) => {
      queryClient.setQueryData(["knownCourses"], (existing: Course[] = []) => [...existing, course]);
    },
  });
}

/**
 * course-management has no "list all courses" endpoint (every real caller
 * so far has known the specific courseId it wants) — this frontend keeps a
 * client-side list of courses created during the current session instead
 * of inventing a new backend endpoint just for a listing UI.
 */
export function useKnownCourses() {
  return useQuery<Course[]>({
    queryKey: ["knownCourses"],
    queryFn: () => Promise.resolve([]),
    staleTime: Infinity,
    initialData: [],
  });
}
