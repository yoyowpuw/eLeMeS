import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { Award, BookOpen, Network, Route as RouteIcon, ShieldCheck } from "lucide-react";
import { Button } from "../../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";

const FEATURES = [
  { icon: BookOpen, title: "Courses", description: "Author versioned course content and enroll learners, with every completion tracked end to end." },
  { icon: RouteIcon, title: "Learning Paths", description: "Chain courses into ordered, multi-step programs — each step's completion advances the next." },
  { icon: Award, title: "Certificates", description: "Every completion issues a cryptographically signed certificate, independently verifiable by anyone." },
  { icon: Network, title: "Org Hierarchy", description: "Matrixed reporting lines and cost centers, with manager oversight scoped to their own subtree." },
];

/**
 * The real public front door for a signed-out visitor at `/` — replaces
 * what used to be RequireAuth's generic "Sign in required" box (a real gap:
 * no actual product page existed before this). Own minimal shell, same
 * pattern as VerifyPage, since AppShell (sidebar/topbar) is only for
 * authenticated app usage.
 */
export function LandingPage() {
  const auth = useAuth();

  return (
    <div className="flex min-h-svh flex-col">
      <header className="flex h-14 items-center justify-between border-b border-slate-200 px-6 dark:border-slate-800">
        <span className="text-base font-bold text-blue-600">eLeMeS</span>
        <div className="flex items-center gap-4">
          <Link to="/verify" className="text-sm text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100">
            Verify a certificate
          </Link>
          <Button onClick={() => auth.signinRedirect()}>Sign in</Button>
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-4xl flex-1 flex-col gap-10 px-6 py-16">
        <div className="flex flex-col gap-3">
          <h1 className="text-display font-semibold text-slate-900 dark:text-slate-100">Enterprise Learning Management</h1>
          <p className="max-w-xl text-sm text-slate-500 dark:text-slate-400">
            Course authoring, enrollment, assessment, and evidentiary certification — built for organizations that need a real audit trail, not just
            a completion checkbox.
          </p>
          <div className="mt-2 flex items-center gap-3">
            <Button size="lg" onClick={() => auth.signinRedirect()}>
              Sign in to get started
            </Button>
            <Button size="lg" variant="outline" asChild>
              <Link to="/verify">
                <ShieldCheck className="size-4" />
                Verify a certificate
              </Link>
            </Button>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {FEATURES.map(({ icon: Icon, title, description }) => (
            <Card key={title}>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Icon className="size-4 text-blue-600" aria-hidden="true" />
                  {title}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-slate-500 dark:text-slate-400">{description}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </main>
    </div>
  );
}
