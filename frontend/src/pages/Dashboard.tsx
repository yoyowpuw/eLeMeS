import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";

/**
 * Temporary placeholder — reused as the index route for all three of
 * `/`, `/manage`, and `/admin` until the real per-role dashboards are
 * built (see the frontend redesign plan). Org Hierarchy and Tenants are
 * deliberately NOT linked here anymore — they no longer live under this
 * component's own tree (moved to /manage and /admin respectively), and
 * this same component is temporarily mounted at all three roots, so a
 * single correct destination doesn't exist yet for either link.
 */
export function Dashboard() {
  const auth = useAuth();

  return (
    <div>
      <h1>eLeMeS</h1>
      <p>Enterprise Learning Management System — course authoring, enrollment, assessment, and evidentiary certification.</p>
      {auth.isAuthenticated ? (
        <ul className="quick-links">
          <li><Link to="/courses">Browse / author courses</Link></li>
          <li><Link to="/paths">Learning paths</Link></li>
          <li><Link to="/enrollments">Your enrollments</Link></li>
          <li><Link to="/certificates">Your certificates</Link></li>
        </ul>
      ) : (
        <div className="empty-state">
          <p>Sign in to enroll in courses and earn certificates.</p>
          <button onClick={() => auth.signinRedirect()}>Sign in</button>
        </div>
      )}
      <p>
        <Link to="/verify">Verifying someone else's certificate?</Link> No sign-in required —
        certificate verification is deliberately public.
      </p>
    </div>
  );
}
