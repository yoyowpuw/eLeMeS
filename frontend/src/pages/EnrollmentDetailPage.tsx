import { useState } from "react";
import { useParams } from "react-router-dom";
import { useCompleteEnrollment, useEnrollment, useStartEnrollment } from "../api/enrollments";
import { useStartAssessment, useSubmitAssessment } from "../api/assessments";
import { useCertificateByEnrollment } from "../api/certificates";
import { ApiError } from "../api/http";

export function EnrollmentDetailPage() {
  const { enrollmentId } = useParams<{ enrollmentId: string }>();
  const { data: enrollment, isLoading } = useEnrollment(enrollmentId);
  const start = useStartEnrollment();
  const complete = useCompleteEnrollment();
  const startAssessment = useStartAssessment();
  const submitAssessment = useSubmitAssessment();

  const [assessmentId, setAssessmentId] = useState<string | undefined>();
  const [questionText, setQuestionText] = useState("2 + 2 = ?");
  const [optionA, setOptionA] = useState("3");
  const [optionB, setOptionB] = useState("4");
  const [correctOption, setCorrectOption] = useState<0 | 1>(1);
  const [selectedAnswer, setSelectedAnswer] = useState<0 | 1>(0);

  const isCompleted = enrollment?.status === "COMPLETED";
  const { data: certificate } = useCertificateByEnrollment(enrollmentId, isCompleted);

  if (isLoading) return <p>Loading…</p>;
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
