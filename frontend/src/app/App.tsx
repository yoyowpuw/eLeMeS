import { Route, Routes } from "react-router-dom";
import { LearnerLayout } from "./layouts/LearnerLayout";
import { ManagerLayout } from "./layouts/ManagerLayout";
import { AdminLayout } from "./layouts/AdminLayout";
import { RootRoute } from "./RootRoute";
import { ManagerDashboard } from "../features/dashboard/pages/ManagerDashboard";
import { AdminDashboard } from "../features/dashboard/pages/AdminDashboard";
import { CoursesListPage } from "../features/courses/pages/CoursesListPage";
import { EnrollmentsListPage } from "../features/enrollments/pages/EnrollmentsListPage";
import { EnrollmentDetailPage } from "../features/enrollments/pages/EnrollmentDetailPage";
import { CertificatesListPage } from "../features/certificates/pages/CertificatesListPage";
import { VerifyPage } from "../features/verify/pages/VerifyPage";
import { PathsListPage } from "../features/paths/pages/PathsListPage";
import { PathEnrollPage } from "../features/paths/pages/PathEnrollPage";
import { PathProgressPage } from "../features/paths/pages/PathProgressPage";
import { OrgUnitsListPage } from "../features/org-units/pages/OrgUnitsListPage";
import { OrgUnitDetailPage } from "../features/org-units/pages/OrgUnitDetailPage";
import { TenantsListPage } from "../features/tenants/pages/TenantsListPage";

/**
 * Ch.4 ADR-004 / Ch.14 §4-5: Manager and Admin get structurally separate
 * route trees (own URL namespace, own layout, own nav) rather than the
 * single-tree-with-inline-role-checks the app used before this redesign —
 * `RequireRole` (inside each layout) enforces this at the route level, not
 * inside page bodies. `/manage` and `/admin` render the shared Org
 * Hierarchy pages (org-hierarchy management has no learner-facing
 * analogue) — Courses/Paths/Enrollments/Certificates stay in the Learner
 * tree only, since browsing/enrolling is a learner action even for a
 * manager/admin acting as their own learner. Every page is now on the new
 * design system with a real per-role dashboard at each workspace's index.
 */
export function App() {
  return (
    <Routes>
      {/* Ch.26 §6: must keep working fully signed-out — no layout, no auth gate. */}
      <Route path="verify" element={<VerifyPage />} />
      {/* Real public landing page when signed out, the Learner dashboard when signed in — see RootRoute's own doc comment. Deliberately outside LearnerLayout's RequireAuth gate. */}
      <Route path="/" element={<RootRoute />} />

      <Route element={<LearnerLayout />}>
        <Route path="courses" element={<CoursesListPage />} />
        <Route path="paths" element={<PathsListPage />} />
        <Route path="paths/:pathId/enroll" element={<PathEnrollPage />} />
        <Route path="path-enrollments/:pathProgressId" element={<PathProgressPage />} />
        <Route path="enrollments" element={<EnrollmentsListPage />} />
        <Route path="enrollments/:enrollmentId" element={<EnrollmentDetailPage />} />
        <Route path="certificates" element={<CertificatesListPage />} />
      </Route>

      <Route path="manage" element={<ManagerLayout />}>
        <Route index element={<ManagerDashboard />} />
        <Route path="org-units" element={<OrgUnitsListPage />} />
        <Route path="org-units/:orgUnitId" element={<OrgUnitDetailPage />} />
      </Route>

      <Route path="admin" element={<AdminLayout />}>
        <Route index element={<AdminDashboard />} />
        <Route path="org-units" element={<OrgUnitsListPage />} />
        <Route path="org-units/:orgUnitId" element={<OrgUnitDetailPage />} />
        <Route path="tenants" element={<TenantsListPage />} />
      </Route>
    </Routes>
  );
}
