import type { ReactNode } from "react";
import { useAuth } from "react-oidc-context";

export function RequireAuth({ children }: { children: ReactNode }) {
  const auth = useAuth();

  if (auth.isLoading) return <p><span className="spinner" aria-hidden="true" />Loading session…</p>;
  if (auth.error) return <p role="alert">Authentication error: {auth.error.message}</p>;
  if (!auth.isAuthenticated) {
    return (
      <div className="empty-state">
        <p>You need to sign in to see this page.</p>
        <button onClick={() => auth.signinRedirect()}>Sign in</button>
      </div>
    );
  }
  return <>{children}</>;
}
