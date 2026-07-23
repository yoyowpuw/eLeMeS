import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "react-oidc-context";
import { apiFetch, SERVICE_URLS } from "./http";
import type { LearningPath, PathVersion } from "./types";

export function usePathCurrentVersion(pathId: string | undefined) {
  const auth = useAuth();
  return useQuery({
    queryKey: ["pathCurrentVersion", pathId],
    queryFn: () => apiFetch<PathVersion>(SERVICE_URLS.courses, `/api/v1/learning-paths/${pathId}/current-version`, auth.user?.access_token),
    enabled: !!pathId && !!auth.user,
  });
}

export function useCreatePath() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: { name: string; courseIds: string[] }) =>
      apiFetch<LearningPath>(SERVICE_URLS.courses, "/api/v1/learning-paths", auth.user?.access_token, {
        method: "POST",
        body: JSON.stringify(body),
      }),
    onSuccess: (path) => {
      queryClient.setQueryData(["knownPaths"], (existing: LearningPath[] = []) => [...existing, path]);
    },
  });
}

/** Same session-scoped-list approach as `useKnownCourses` — course-management has no "list all paths" endpoint. */
export function useKnownPaths() {
  return useQuery<LearningPath[]>({
    queryKey: ["knownPaths"],
    queryFn: () => Promise.resolve([]),
    staleTime: Infinity,
    initialData: [],
  });
}
