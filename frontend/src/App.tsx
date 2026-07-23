import { Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { RequireAuth } from "./components/RequireAuth";
import { Dashboard } from "./pages/Dashboard";
import { CoursesPage } from "./pages/CoursesPage";
import { EnrollmentsPage } from "./pages/EnrollmentsPage";
import { EnrollmentDetailPage } from "./pages/EnrollmentDetailPage";
import { CertificatesPage } from "./pages/CertificatesPage";
import { VerifyPage } from "./pages/VerifyPage";
import { PathsPage } from "./pages/PathsPage";
import { PathEnrollPage } from "./pages/PathEnrollPage";
import { PathProgressPage } from "./pages/PathProgressPage";
import { OrgUnitsPage } from "./pages/OrgUnitsPage";
import { OrgUnitDetailPage } from "./pages/OrgUnitDetailPage";
import { TenantsPage } from "./pages/TenantsPage";

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="verify" element={<VerifyPage />} />
        <Route path="courses" element={<RequireAuth><CoursesPage /></RequireAuth>} />
        <Route path="paths" element={<RequireAuth><PathsPage /></RequireAuth>} />
        <Route path="paths/:pathId/enroll" element={<RequireAuth><PathEnrollPage /></RequireAuth>} />
        <Route path="path-enrollments/:pathProgressId" element={<RequireAuth><PathProgressPage /></RequireAuth>} />
        <Route path="enrollments" element={<RequireAuth><EnrollmentsPage /></RequireAuth>} />
        <Route path="enrollments/:enrollmentId" element={<RequireAuth><EnrollmentDetailPage /></RequireAuth>} />
        <Route path="certificates" element={<RequireAuth><CertificatesPage /></RequireAuth>} />
        <Route path="org-units" element={<RequireAuth><OrgUnitsPage /></RequireAuth>} />
        <Route path="org-units/:orgUnitId" element={<RequireAuth><OrgUnitDetailPage /></RequireAuth>} />
        <Route path="tenants" element={<RequireAuth><TenantsPage /></RequireAuth>} />
      </Route>
    </Routes>
  );
}
