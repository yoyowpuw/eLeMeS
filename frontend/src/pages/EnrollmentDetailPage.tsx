import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { useCompleteEnrollment, useEnrollment, useStartEnrollment } from "../api/enrollments";
import { useStartAssessment, useSubmitAssessment } from "../api/assessments";
import { useCertificateByEnrollment } from "../api/certificates";
import { ApiError } from "../api/http";
import type { Enrollment } from "../api/types";

export function EnrollmentDetailPage() {
  const { enrollmentId } = useParams<{ enrollmentId: string }>();
  const { data: enrollment, isLoading } = useEnrollment(enrollmentId);
  const start = useStartEnrollment();
  const complete = useCompleteEnrollment();
  const startAssessment = useStartAssessment();
  const submitAssessment = useSubmitAssessment();
  const queryClient = useQueryClient();

  // A Learning Path step's `Enrollment` is created server-side, not via
  // `useEnroll()` — so `myEnrollments` (the session-scoped list the
  // Certificates page reads) would never learn about it, or about its
  // *current* status, without this. Keyed on the live query result (not
  // captured once) so it stays in sync — critically, right after
  // `complete.mutate()` succeeds and `enrollment.status` flips to
  // COMPLETED, which is exactly when the Certificates page needs to know
  // a certificate might now exist. An earlier version of this only
  // registered a one-time snapshot from `PathProgressPage`, which froze
  // at whatever status the enrollment had at that moment (almost always
  // IN_PROGRESS, since the user is about to navigate away to complete
  // it) — found by actually driving the UI through a full path completion
  // in a real browser, not by testing this endpoint in isolation.
  useEffect(() => {
    if (!enrollment) return;
    queryClient.setQueryData(["myEnrollments"], (existing: Enrollment[] = []) => {
      const index = existing.findIndex((e) => e.enrollmentId === enrollment.enrollmentId);
      if (index === -1) return [...existing, enrollment];
      if (existing[index] === enrollment) return existing;
      const copy = [...existing];
      copy[index] = enrollment;
      return copy;
    });
  }, [enrollment, queryClient]);

  const [assessmentId, setAssessmentId] = useState<string | undefined>();
  const [questionText, setQuestionText] = useState("2 + 2 = ?");
  const [optionA, setOptionA] = useState("3");
  const [optionB, setOptionB] = useState("4");
  const [correctOption, setCorrectOption] = useState<0 | 1>(1);
  const [selectedAnswer, setSelectedAnswer] = useState<0 | 1>(0);

  const isCompleted = enrollment?.status === "COMPLETED";
  const { data: certificate } = useCertificateByEnrollment(enrollmentId, isCompleted);

  if (isLoading) return <p><span className="spinner" aria-hidden="true" />Loading…</p>;
  if (!enrollment) return <p>Enrollment not found.</p>;

  function handleStartAssessment(e: React.FormEvent) {
    e.preventDefault();
    startAssessment.mutate(
      {
        enrollmentId: enrollment!.enrollmentId,
        courseId: enrollment!.courseId,
        passingScore: 70,
        questions: [{ questionId: "q1", text: questionText, options: [optionA, optionB], correctOptionIndex: correctOption }],
      },
      { onSuccess: (assessment) => setAssessmentId(assessment.assessmentId) },
    );
  }

  function handleSubmitAssessment(e: React.FormEvent) {
    e.preventDefault();
    if (!assessmentId) return;
    submitAssessment.mutate({ assessmentId, answers: { q1: selectedAnswer } });
  }

  return (
    <div>
      <h1>Enrollment</h1>
      <div className="card">
        <div>Course: {enrollment.courseId}</div>
        <div>Learner: {enrollment.learnerId}</div>
        <div>Status: <strong>{enrollment.status}</strong> ({enrollment.progressPercent}%)</div>
      </div>

      {enrollment.status === "ASSIGNED" && (
        <button onClick={() => start.mutate(enrollment.enrollmentId)} disabled={start.isPending}>
          {start.isPending ? "Starting…" : "Start course"}
        </button>
      )}

      {enrollment.status === "IN_PROGRESS" && (
        <div className="card">
          <h2>Finish this course</h2>
          <p>Either mark it complete directly, or take a short assessment first.</p>
          <button onClick={() => complete.mutate(enrollment.enrollmentId)} disabled={complete.isPending}>
            {complete.isPending ? "Completing…" : "Mark complete (no assessment)"}
          </button>

          <h3>— or take an assessment —</h3>
          {!assessmentId ? (
            <form onSubmit={handleStartAssessment}>
              <label>
                Question
                <input value={questionText} onChange={(e) => setQuestionText(e.target.value)} required />
              </label>
              <label>
                Option A
                <input value={optionA} onChange={(e) => setOptionA(e.target.value)} required />
              </label>
              <label>
                Option B
                <input value={optionB} onChange={(e) => setOptionB(e.target.value)} required />
              </label>
              <label>
                Correct option
                <select value={correctOption} onChange={(e) => setCorrectOption(Number(e.target.value) as 0 | 1)}>
                  <option value={0}>A</option>
                  <option value={1}>B</option>
                </select>
              </label>
              <button type="submit" disabled={startAssessment.isPending}>
                {startAssessment.isPending ? "Creating…" : "Create assessment"}
              </button>
            </form>
          ) : (
            <form onSubmit={handleSubmitAssessment}>
              <p>{questionText}</p>
              <label>
                <input type="radio" name="answer" checked={selectedAnswer === 0} onChange={() => setSelectedAnswer(0)} /> {optionA}
              </label>
              <label>
                <input type="radio" name="answer" checked={selectedAnswer === 1} onChange={() => setSelectedAnswer(1)} /> {optionB}
              </label>
              <button type="submit" disabled={submitAssessment.isPending}>
                {submitAssessment.isPending ? "Submitting…" : "Submit answer"}
              </button>
              {submitAssessment.isSuccess && (
                <p>Result: <strong>{submitAssessment.data.status}</strong> (score {submitAssessment.data.score})</p>
              )}
            </form>
          )}
          {startAssessment.isError && (
            <p role="alert">{startAssessment.error instanceof ApiError ? startAssessment.error.message : "Failed to create assessment"}</p>
          )}
        </div>
      )}

      {isCompleted && (
        <div className="card">
          <h2>Certificate</h2>
          {certificate ? (
            <>
              <p>Issued {new Date(certificate.issuedAt).toLocaleString()} — status {certificate.status}</p>
              <p><code>{certificate.certificateId}</code></p>
            </>
          ) : (
            <p>Completed — waiting for Certification to issue the certificate (a couple of Kafka hops away)…</p>
          )}
        </div>
      )}
    </div>
  );
}
