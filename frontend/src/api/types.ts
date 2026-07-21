export interface Course {
  courseId: string;
  tenantId: string;
  code: string;
  title: string;
  currentVersionId: string;
  orgUnitId: string | null;
}

export interface Enrollment {
  enrollmentId: string;
  tenantId: string;
  learnerId: string;
  courseId: string;
  contentVersionId: string;
  orgUnitId: string | null;
  status: "ASSIGNED" | "IN_PROGRESS" | "AWAITING_GRADING" | "COMPLETED";
  progressPercent: number;
}

export interface Question {
  questionId: string;
  text: string;
  options: string[];
  correctOptionIndex: number;
}

export interface Assessment {
  assessmentId: string;
  enrollmentId: string;
  status: "STARTED" | "AWAITING_GRADING" | "PASSED" | "FAILED";
  score: number | null;
}

export interface Certificate {
  certificateId: string;
  enrollmentId: string;
  learnerId: string;
  courseId: string;
  contentVersionId: string;
  orgUnitId: string | null;
  score: number | null;
  pathId: string | null;
  pathVersionId: string | null;
  realizedStepCourseIds: string[] | null;
  status: "ISSUED" | "REVOKED";
  issuedAt: string;
  signature: string;
}
