import { Link, Outlet } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { rolesFromAccessToken, decodeAccessToken } from "../auth/jwt";

export function Layout() {
  const auth = useAuth();
  const roles = rolesFromAccessToken(auth.user?.access_token);
  const tenantId = auth.user?.access_token ? decodeAccessToken(auth.user.access_token).tenant_id : undefined;
  const canAuthorContent = roles.includes("admin") || roles.includes("manager");

  return (
    <div className="app-shell">
      <header className="app-nav">
        <div className="app-nav__brand">
          <Link to="/">eLeMeS</Link>
        </div>
        <nav>
          {auth.isAuthenticated && (
            <>
              <Link to="/courses">Courses</Link>
              <Link to="/enrollments">My Enrollments</Link>
              <Link to="/certificates">My Certificates</Link>
            </>
          )}
          <Link to="/verify">Verify a Certificate</Link>
        </nav>
        <div className="app-nav__account">
          {auth.isAuthenticated ? (
            <>
              <span className="app-nav__who">
                {auth.user?.profile.preferred_username} · {tenantId} · {roles.join(", ") || "no roles"}
                {canAuthorContent ? "" : " (learner view)"}
              </span>
              <button onClick={() => auth.removeUser()}>Sign out</button>
            </>
          ) : (
            <button onClick={() => auth.signinRedirect()}>Sign in</button>
          )}
        </div>
      </header>
      <main className="app-content">
        <Outlet />
      </main>
    </div>
  );
}
