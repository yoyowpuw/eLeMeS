import type { ReactNode } from "react";
import { useAuth } from "react-oidc-context";
import { LogIn } from "lucide-react";
import { Button } from "../ui/button";
import { EmptyState } from "../feedback/EmptyState";

export function RequireAuth({ children }: { children: ReactNode }) {
  const auth = useAuth();

  if (auth.isLoading) {
    return (
      <div className="flex min-h-svh items-center justify-center text-sm text-slate-500">
        <Button variant="ghost" disabled isLoading className="pointer-events-none">
          Loading session…
        </Button>
      </div>
    );
  }
  if (auth.error) {
    return (
      <div className="flex min-h-svh items-center justify-center p-6">
        <p role="alert" className="text-sm text-red-600">
          Authentication error: {auth.error.message}
        </p>
      </div>
    );
  }
  if (!auth.isAuthenticated) {
    return (
      <div className="flex min-h-svh items-center justify-center p-6">
        <EmptyState
          icon={LogIn}
          title="Sign in required"
          description="You need to sign in to see this page."
          action={<Button onClick={() => auth.signinRedirect()}>Sign in</Button>}
        />
      </div>
    );
  }
  return <>{children}</>;
}
