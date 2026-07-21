import { Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { RequireAuth } from "./components/RequireAuth";
import { Dashboard } from "./pages/Dashboard";
import { CoursesPage } from "./pages/CoursesPage";
import { EnrollmentsPage } from "./pages/EnrollmentsPage";
import { EnrollmentDetailPage } from "./pages/EnrollmentDetailPage";
import { CertificatesPage } from "./pages/CertificatesPage";
import { VerifyPage } from "./pages/VerifyPage";

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="verify" element={<VerifyPage />} />
        <Route path="courses" element={<RequireAuth><CoursesPage /></RequireAuth>} />
        <Route path="enrollments" element={<RequireAuth><EnrollmentsPage /></RequireAuth>} />
        <Route path="enrollments/:enrollmentId" element={<RequireAuth><EnrollmentDetailPage /></RequireAuth>} />
        <Route path="certificates" element={<RequireAuth><CertificatesPage /></RequireAuth>} />
      </Route>
    </Routes>
  );
}
