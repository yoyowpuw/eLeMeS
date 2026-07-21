/**
 * Ch.13 §6's BFF layer isn't built yet (see README's deferred notes) — this
 * frontend calls each of the four data-plane services directly over REST,
 * the same APIs the curl-driven golden path already exercises.
 */
export const SERVICE_URLS = {
  courses: "http://localhost:8083",
  enrollments: "http://localhost:8081",
  assessments: "http://localhost:8082",
  certificates: "http://localhost:8084",
} as const;

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export async function apiFetch<T>(
  baseUrl: string,
  path: string,
  accessToken: string | undefined,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);

  const response = await fetch(`${baseUrl}${path}`, { ...init, headers });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiError(response.status, body.error ?? `${response.status} ${response.statusText}`);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}
