import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { rolesFromAccessToken } from "../auth/jwt";

export function Dashboard() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);

  return (
    <div>
      <h1>eLeMeS</h1>
      <p>Enterprise Learning Management System — course authoring, enrollment, assessment, and evidentiary certification.</p>
      {auth.isAuthenticated ? (
        <ul className="quick-links">
          <li><Link to="/courses">Browse / author courses</Link></li>
          <li><Link to="/paths">Learning paths</Link></li>
          <li><Link to="/org-units">Org hierarchy</Link></li>
          <li><Link to="/enrollments">Your enrollments</Link></li>
          <li><Link to="/certificates">Your certificates</Link></li>
          <li><Link to="/tenants">{roles.includes("platform-admin") ? "Manage tenants" : "Your tenant"}</Link></li>
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
