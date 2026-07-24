import { Route, Routes } from "react-router-dom";
import { LearnerLayout } from "./layouts/LearnerLayout";
import { ManagerLayout } from "./layouts/ManagerLayout";
import { AdminLayout } from "./layouts/AdminLayout";
import { LegacyPage } from "../components/LegacyPage";
import { Dashboard } from "../pages/Dashboard";
import { CoursesPage } from "../pages/CoursesPage";
import { EnrollmentsPage } from "../pages/EnrollmentsPage";
import { EnrollmentDetailPage } from "../pages/EnrollmentDetailPage";
import { CertificatesPage } from "../pages/CertificatesPage";
import { VerifyPage } from "../pages/VerifyPage";
import { PathsPage } from "../pages/PathsPage";
import { PathEnrollPage } from "../pages/PathEnrollPage";
import { PathProgressPage } from "../pages/PathProgressPage";
import { OrgUnitsPage } from "../pages/OrgUnitsPage";
import { OrgUnitDetailPage } from "../pages/OrgUnitDetailPage";
import { TenantsPage } from "../pages/TenantsPage";

/**
 * Ch.4 ADR-004 / Ch.14 §4-5: Manager and Admin get structurally separate
 * route trees (own URL namespace, own layout, own nav) rather than the
 * single-tree-with-inline-role-checks the app used before this redesign —
 * `RequireRole` (inside each layout) enforces this at the route level, not
 * inside page bodies. `/manage` and `/admin` still render today's
 * still-unmigrated page components for now (Courses/Paths/Enrollments stay
 * in the Learner tree only — browsing/enrolling is a learner action even
 * for a manager/admin acting as their own learner; org-hierarchy management
 * and tenant administration have no learner-facing analogue, so they move
 * into the management trees). Dashboards are real per-role pages built in
 * a later phase — index routes reuse the old Dashboard component as a
 * placeholder until then.
 */
export function App() {
  return (
    <Routes>
      {/* Ch.26 §6: must keep working fully signed-out — no layout, no auth gate. */}
      <Route path="verify" element={<LegacyPage><VerifyPage /></LegacyPage>} />

      <Route element={<LearnerLayout />}>
        <Route index element={<LegacyPage><Dashboard /></LegacyPage>} />
        <Route path="courses" element={<LegacyPage><CoursesPage /></LegacyPage>} />
        <Route path="paths" element={<LegacyPage><PathsPage /></LegacyPage>} />
        <Route path="paths/:pathId/enroll" element={<LegacyPage><PathEnrollPage /></LegacyPage>} />
        <Route path="path-enrollments/:pathProgressId" element={<LegacyPage><PathProgressPage /></LegacyPage>} />
        <Route path="enrollments" element={<LegacyPage><EnrollmentsPage /></LegacyPage>} />
        <Route path="enrollments/:enrollmentId" element={<LegacyPage><EnrollmentDetailPage /></LegacyPage>} />
        <Route path="certificates" element={<LegacyPage><CertificatesPage /></LegacyPage>} />
      </Route>

      <Route path="manage" element={<ManagerLayout />}>
        <Route index element={<LegacyPage><Dashboard /></LegacyPage>} />
        <Route path="org-units" element={<LegacyPage><OrgUnitsPage /></LegacyPage>} />
        <Route path="org-units/:orgUnitId" element={<LegacyPage><OrgUnitDetailPage /></LegacyPage>} />
      </Route>

      <Route path="admin" element={<AdminLayout />}>
        <Route index element={<LegacyPage><Dashboard /></LegacyPage>} />
        <Route path="org-units" element={<LegacyPage><OrgUnitsPage /></LegacyPage>} />
        <Route path="org-units/:orgUnitId" element={<LegacyPage><OrgUnitDetailPage /></LegacyPage>} />
        <Route path="tenants" element={<LegacyPage><TenantsPage /></LegacyPage>} />
      </Route>
    </Routes>
  );
}
